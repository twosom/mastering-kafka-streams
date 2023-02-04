package com.icloud;

import com.icloud.model.HighScores;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class LeaderboardService {
    private final HostInfo hostInfo;
    private final KafkaStreams streams;

    ReadOnlyKeyValueStore<String, HighScores> getStores() {
        return streams.store(
                StoreQueryParameters.fromNameAndType(
                        "leader-boards",
                        QueryableStoreTypes.keyValueStore()
                )
        );
    }

    void start() {
        Javalin app = Javalin.create().start(hostInfo.port());
        app.get("/leaderboard/count", this::getCount);
        app.get("/leaderboard/{key}", this::getKey);
    }

    private void getCount(Context ctx) {
        long count = getStores().approximateNumEntries();
        for (StreamsMetadata metadata : streams.streamsMetadataForStore("leader-boards")) {
            if (hostInfo.equals(metadata.hostInfo())) {
                continue;
            }
            count += fetchCountFromRemoteInstance(metadata.hostInfo());
        }
        ctx.json(count);
    }

    private long fetchCountFromRemoteInstance(HostInfo hostInfo) {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%d/leaderboard/count/local", hostInfo.host(), hostInfo.port());
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            return Long.parseLong(response.body().string());
        } catch (Exception e) {
            return 0L;
        }
    }

    private void getKey(Context ctx) {
        String productId = ctx.pathParam("key");
        KeyQueryMetadata metadata = streams.queryMetadataForKey(
                "leader-boards", productId, Serdes.String().serializer()
        );

        if (hostInfo.equals(metadata.activeHost())) {
            HighScores highScores = getStores().get(productId);
            if (highScores == null) {
                ctx.status(404);
                return;
            }

            ctx.json(highScores.toList());
            return;
        }

        String remoteHost = metadata.activeHost().host();
        int remotePort = metadata.activeHost().port();
        String url = String.format(
                "http://%s:%d/leaderboard/%s", remoteHost, remotePort, productId
        );

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            ctx.result(response.body().string());
        } catch (IOException e) {
            ctx.status(500);
        }
    }


}
