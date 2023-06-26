package com.icloud;

import com.icloud.language.DummyClient;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.List;
import java.util.Properties;

public class App {

    public static void main(String[] args) {
        final Topology topology = CryptoTopology.build(new DummyClient());

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("localhost:9093", "localhost:9094", "localhost:9095"));

        final KafkaStreams streams = new KafkaStreams(topology, config);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        System.out.println("Starting Twitter streams");
        streams.start();
    }
}