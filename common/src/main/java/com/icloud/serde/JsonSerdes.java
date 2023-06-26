package com.icloud.serde;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class JsonSerdes<T> implements Serde<T> {

    private JsonSerdes() {/*Not Allow instantiation*/}

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();

    private Class<T> targetClass;

    public JsonSerdes(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public Serializer<T> serializer() {
        return (String topic, T data) -> data == null ? null :
                gson.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Deserializer<T> deserializer() {
        return (String topic, byte[] bytes) -> bytes == null ? null :
                gson.fromJson(new String(bytes, StandardCharsets.UTF_8), targetClass);
    }
}
