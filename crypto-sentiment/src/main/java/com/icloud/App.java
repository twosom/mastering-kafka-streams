package com.icloud;

import com.icloud.language.DummyClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

public class App {
    public static void main(String[] args) {
        final Topology topology = CryptoTopology.build(new DummyClient());
        final Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final KafkaStreams streams = new KafkaStreams(topology, config);
        streams.setUncaughtExceptionHandler((thread, throwable) -> System.out.println(throwable));
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        System.out.println("Starting Twitter streams");
        streams.start();
    }
}
