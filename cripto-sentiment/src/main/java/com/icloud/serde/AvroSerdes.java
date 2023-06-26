package com.icloud.serde;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;

import java.util.Collections;
import java.util.Map;

public class AvroSerdes<T extends SpecificRecord> extends SpecificAvroSerde<T> {

    public static <T extends SpecificRecord> AvroSerdes<T> of(String url, boolean isKey) {
        return new AvroSerdes<>(url, isKey);
    }

    public AvroSerdes(String url, boolean isKey) {
        super();
        final Map<String, String> serdeConfig =
                Collections.singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, url);
        configure(serdeConfig, isKey);
    }
}
