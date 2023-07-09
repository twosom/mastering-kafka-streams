package com.icloud;

import com.icloud.model.DigitalTwin;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class RestService {
    private final HostInfo hostInfo;
    private final KafkaStreams streams;


    ReadOnlyKeyValueStore<String, DigitalTwin> getStore() {
        return streams.store(
                StoreQueryParameters.fromNameAndType(
                        "digital-twin-store",
                        QueryableStoreTypes.keyValueStore()
                )
        );
    }

    void start() {
        Javalin app = Javalin.create().start(hostInfo.port());
        app.get("/devices", this::getAllDevice);
        app.get("/devices/{id}", this::getDevice);
    }

    void getAllDevice(Context context) {
        Map<String, DigitalTwin> allDevices = new HashMap<>();
        try (KeyValueIterator<String, DigitalTwin> iter = getStore().all()) {
            while (iter.hasNext()) {
                KeyValue<String, DigitalTwin> entry = iter.next();
                allDevices.put(entry.key, entry.value);
            }
        }
        context.json(allDevices);
    }

    void getDevice(Context context) {
        String deviceId = context.pathParam("id");
        try {
            DigitalTwin latestState = getStore().get(deviceId);
            context.json(latestState);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
