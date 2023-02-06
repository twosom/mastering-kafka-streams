package com.icloud;

import com.icloud.model.CombinedVitals;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                StoreQueryParameters.fromNameAndType("pulse-counts", QueryableStoreTypes.windowStore()));
    }

    ReadOnlyKeyValueStore<String, CombinedVitals> getAlertsStore() {
        return streams.store(
                StoreQueryParameters.fromNameAndType("alerts", QueryableStoreTypes.keyValueStore()));
    }

    void start() {
        Javalin app = Javalin.create().start(hostInfo.port());

        /** Local window store query: all entries */
        app.get("/bpm/all", this::getAll);

        app.get("/bpm/range/{from}/{to}", this::getAllInRange);

        app.get("/bpm/range/{key}/{from}/{to}", this::getRange);
    }

    void getAll(Context ctx) {
        Map<String, Long> bpm = new HashMap<>();

        KeyValueIterator<Windowed<String>, Long> range = getBpmStore().all();
        while (range.hasNext()) {
            KeyValue<Windowed<String>, Long> next = range.next();
            Windowed<String> key = next.key;
            Long value = next.value;
            bpm.put(key.toString(), value);
        }
        // close the iterator to avoid memory leaks
        range.close();
        // return a JSON response
        ctx.json(bpm);
    }

    void getAllInRange(Context ctx) {
        List<Map<String, Object>> bpms = new ArrayList<>();

        String from = ctx.pathParam("from");
        String to = ctx.pathParam("to");

        Instant fromTime = Instant.ofEpochMilli(Long.valueOf(from));
        Instant toTime = Instant.ofEpochMilli(Long.valueOf(to));

        KeyValueIterator<Windowed<String>, Long> range = getBpmStore().fetchAll(fromTime, toTime);
        while (range.hasNext()) {
            Map<String, Object> bpm = new HashMap<>();
            KeyValue<Windowed<String>, Long> next = range.next();
            String key = next.key.key();
            Window window = next.key.window();
            Long start = window.start();
            Long end = window.end();
            Long count = next.value;
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

    void getRange(Context ctx) {
        List<Map<String, Object>> bpms = new ArrayList<>();

        String key = ctx.pathParam("key");
        String from = ctx.pathParam("from");
        String to = ctx.pathParam("to");

        Instant fromTime = Instant.ofEpochMilli(Long.valueOf(from));
        Instant toTime = Instant.ofEpochMilli(Long.valueOf(to));

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
