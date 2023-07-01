package com.icloud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icloud.model.CombinedVitals;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.*;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
public class RestService {
    private final HostInfo hostInfo;
    private final KafkaStreams streams;

    RestService(HostInfo hostInfo, KafkaStreams streams) {
        this.hostInfo = hostInfo;
        this.streams = streams;
    }

    ReadOnlyWindowStore<String, Long> getBpmStore() {
        return streams.store(
                StoreQueryParameters.fromNameAndType("pulse-counts", QueryableStoreTypes.windowStore())
        );
    }

    ReadOnlyKeyValueStore<String, CombinedVitals> getAlertsStore() {
        return streams.store(
                StoreQueryParameters.fromNameAndType("alerts", QueryableStoreTypes.keyValueStore())
        );
    }

    void start() {
        Javalin app = Javalin.create().start(hostInfo.port());
        app.get("/bpm/all", this::getAllPatientBpm);
        app.get("/bpm/local", this::getLocalPatientBpm);
        app.get("/bpm/range/{from}/{to}", this::getAllBpmRange);
        app.get("/bpm/range/{key}/{from}/{to}", this::getBpmRange);
        app.get("/alerts/all", this::getAllCurrentAlerts);
        app.get("/alerts/local", this::getLocalCurrentAlerts);
    }

    private List<PatientBpm> getLocalPatientBpmList() {
        List<PatientBpm> patientBpmList = new ArrayList<>();
        KeyValueIterator<Windowed<String>, Long> range = getBpmStore().all();
        while (range.hasNext()) {
            PatientBpm patientBpm = PatientBpm.of(range.next());
            patientBpmList.add(patientBpm);
        }
        range.close();
        return patientBpmList;
    }

    void getLocalPatientBpm(Context ctx) {
        log.info("[bpm[local]] called");
        List<PatientBpm> localPatientBpmList = getLocalPatientBpmList();
        ctx.json(localPatientBpmList);
    }

    Map<String, CombinedVitals> getLocalCurrentAlerts() {
        KeyValueIterator<String, CombinedVitals> all = getAlertsStore().all();
        Map<String, CombinedVitals> map = new HashMap<>();
        while (all.hasNext()) {
            KeyValue<String, CombinedVitals> next = all.next();
            map.put(next.key, next.value);
        }
        all.close();
        return map;
    }

    void getLocalCurrentAlerts(Context ctx) {
        log.info("[alert[local]] called");
        Map<String, CombinedVitals> map = getLocalCurrentAlerts();
        ctx.json(map);
    }

    /**
     * 모든 사용자의 <b>현재</b> 긴급 알림 정보를 가져오는 메소드
     *
     * @param ctx 컨텍스트 객체
     */
    void getAllCurrentAlerts(Context ctx) {
        log.info("[alert[all]] called");
        Map<String, CombinedVitals> localCurrentAlerts = getLocalCurrentAlerts();
        Map<String, CombinedVitals> remoteCurrentAlerts = getRemoteAlerts();
        localCurrentAlerts.putAll(remoteCurrentAlerts);
        ctx.json(localCurrentAlerts);
    }

    private Map<String, CombinedVitals> getRemoteAlerts() {
        Map<String, CombinedVitals> map = new HashMap<>();
        for (StreamsMetadata metadata : this.streams.streamsMetadataForStore("alerts")) {
            if (this.hostInfo.equals(metadata.hostInfo())) continue;
            OkHttpClient client = new OkHttpClient();
            String url = "http://%s:%d/alerts/local".formatted(metadata.host(), metadata.port());
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, CombinedVitals> remoteResult = mapper.readValue(response.body().bytes(), new TypeReference<>() {
                });
                map.putAll(remoteResult);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }

    record PatientBpm(String start, String end, String patientId, Long bpm) {
        static PatientBpm of(KeyValue<Windowed<String>, Long> next) {
            Windowed<String> key = next.key;
            Long value = next.value;
            ZoneOffset zoneOffset = ZoneOffset.of("+09:00");
            OffsetDateTime startTime = key.window().startTime().atOffset(zoneOffset);
            OffsetDateTime endTime = key.window().endTime().atOffset(zoneOffset);
            return new PatientBpm(
                    startTime.toString(),
                    endTime.toString(),
                    key.key(),
                    value
            );
        }

    }

    void getAllPatientBpm(Context ctx) {
        log.info("[bpm[all]] called");
        List<PatientBpm> localPatientBpmList = getLocalPatientBpmList();
        for (StreamsMetadata metadata : this.streams.streamsMetadataForStore("pulse-counts")) {
            if (this.hostInfo.equals(metadata.hostInfo())) continue;
            OkHttpClient client = new OkHttpClient();
            String url = "http://%s:%d/bpm/local".formatted(metadata.host(), metadata.port());
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                List<PatientBpm> remotePatientBpmList = new ObjectMapper().readValue(response.body().bytes(), new TypeReference<List<PatientBpm>>() {
                });
                localPatientBpmList.addAll(remotePatientBpmList);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Map<String, List<PatientBpm>> collect = localPatientBpmList.stream()
                .collect(groupingBy(PatientBpm::patientId));
        ctx.json(collect);
    }

    void getAllBpmRange(Context ctx) {
        List<Map<String, Object>> bpms = new ArrayList<>();

        String from = ctx.pathParam("from");
        String to = ctx.pathParam("to");

        Instant fromTime = Instant.ofEpochMilli(Long.parseLong(from));
        Instant toTime = Instant.ofEpochMilli(Long.parseLong(to));

        KeyValueIterator<Windowed<String>, Long> range = getBpmStore().fetchAll(fromTime, toTime);
        while (range.hasNext()) {
            Map<String, Object> bpm = new HashMap<>();
            KeyValue<Windowed<String>, Long> next = range.next();
            String key = next.key.key();
            Window window = next.key.window();
            long start = window.start();
            long end = window.end();
            long count = next.value;
            bpm.put("key", key);
            bpm.put("start", Instant.ofEpochMilli(start).toString());
            bpm.put("end", Instant.ofEpochMilli(end).toString());
            bpm.put("count", count);
            bpms.add(bpm);
        }
        // close the iterator to avoid memory leaks
        range.close();
        // return a JSON response
        ctx.json(bpms);
    }

    void getBpmRange(Context ctx) {
        List<Map<String, Object>> bpms = new ArrayList<>();

        String key = ctx.pathParam("key");
        String from = ctx.pathParam("from");
        String to = ctx.pathParam("to");

        Instant fromTime = Instant.ofEpochMilli(Long.parseLong(from));
        Instant toTime = Instant.ofEpochMilli(Long.parseLong(to));

        WindowStoreIterator<Long> range = getBpmStore().fetch(key, fromTime, toTime);
        while (range.hasNext()) {
            Map<String, Object> bpm = new HashMap<>();
            KeyValue<Long, Long> next = range.next();
            Long timestamp = next.key;
            Long count = next.value;
            bpm.put("timestamp", Instant.ofEpochMilli(timestamp).toString());
            bpm.put("count", count);
            bpms.add(bpm);
        }
        // close the iterator to avoid memory leaks
        range.close();
        // return a JSON response
        ctx.json(bpms);
    }
}
