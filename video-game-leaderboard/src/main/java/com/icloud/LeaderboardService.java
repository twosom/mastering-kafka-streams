package com.icloud;

import com.icloud.model.HighScores;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LeaderboardService {
    private final HostInfo hostInfo;
    private final KafkaStreams streams;

    /**
     * 쿼리 가능한 스토어 반환
     *
     * @return 쿼리 가능한 스토어
     */
    ReadOnlyKeyValueStore<String, HighScores> getStore() {
        return streams.store(
                StoreQueryParameters.fromNameAndType(
                        "leader-boards",
                        QueryableStoreTypes.keyValueStore()
                )
        );
    }

    void start() {
        Javalin app = Javalin.create().start(hostInfo.port());

        app.get("/leaderboard/count/local", this::getCountLocal);
        app.get("/leaderboard/count", this::getCount);
        app.get("/leaderboard/{key}", this::getKey);
    }

    void getCountLocal(Context context) {
        log.info("[count local] called");
        long count = getStore().approximateNumEntries();
        context.json(count);
    }

    void getCount(Context context) {
        log.info("[count] called");
        // 로컬 상태 저장소의 엔트리 개수로 count 를 초기화
        long count = getStore().approximateNumEntries();

        // 각 카프카 스트림즈 인스턴스의 호스트/포트 쌍을 얻기 위해 allMetadataForStore 사용
        count += streams.streamsMetadataForStore("leader-boards").stream()
                .filter(metadata -> !hostInfo.equals(metadata.hostInfo()))
                .mapToLong(this::fetchCountFromRemoteInstance)
                .sum();
        context.json(count);
    }

    private long fetchCountFromRemoteInstance(StreamsMetadata meatdata) {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%d/leaderboard/count/local", meatdata.host(), meatdata.port());
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            return Long.parseLong(response.body().string());
        } catch (Exception e) {
            return 0L;
        }
    }

    void getKey(Context context) {
        log.info("[key] called");
        String productId = context.pathParam("key");

        KeyQueryMetadata metadata = streams.queryMetadataForKey(
                "leader-boards",
                productId, Serdes.String().serializer()
        );


        if (hostInfo.equals(metadata.activeHost())) {
            HighScores highScores = getStore().get(productId);
            if (highScores == null) {
                // 해당 인스턴스에서 값을 찾지 못한 경우
                context.status(404);
                return;
            }

            // 게임을 발견했으므로 상위 점수들 반환
            context.json(highScores.toList());
            return;
        }

        String remoteHost = metadata.activeHost().host();
        int remotePort = metadata.activeHost().port();
        String url = "http://%s:%d/leaderboard/%s".formatted(remoteHost, remotePort, productId);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            context.result(response.body().string());
        } catch (IOException e) {
            context.status(500);
        }
    }

}
