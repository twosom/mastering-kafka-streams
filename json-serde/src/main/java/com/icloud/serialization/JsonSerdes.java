package com.icloud.serialization;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

public class JsonSerdes {

    public static <T> Serde<T> of(final Class<T> clazz) {
        final JsonSerializer<T> serializer = new JsonSerializer<>();
        final JsonDeserializer<T> deserializer = new JsonDeserializer<>(clazz);
        return Serdes.serdeFrom(serializer, deserializer);
    }
}
