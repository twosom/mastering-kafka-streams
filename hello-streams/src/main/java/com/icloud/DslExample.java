package com.icloud;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class DslExample {
    public static void main(String[] args) {
        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<Object, Object> usersStream = builder.stream("users");

        usersStream.foreach((key, value) -> {
            System.out.println("(DSL) Hello, " + value);
        });
        final KafkaStreams streams = new KafkaStreams(builder.build(), getConfig());
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static Properties getConfig() {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev1");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Void().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return config;
    }
}
