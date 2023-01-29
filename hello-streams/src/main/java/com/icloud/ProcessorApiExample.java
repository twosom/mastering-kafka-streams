package com.icloud;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.Record;

import java.util.Properties;

public class ProcessorApiExample {
    public static void main(String[] args) {
        final Topology topology = new Topology();
        topology.addSource("UserSource", "users");
        topology.addProcessor("SayHello", SayHelloProcessor::new, "UserSource");

        KafkaStreams streams = new KafkaStreams(topology, getConfig());
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

    static class SayHelloProcessor implements Processor<Void, String, Void, Void> {
        @Override
        public void process(Record<Void, String> record) {
            System.out.println("(Processor API) Hello, " + record.value());
        }
    }


}

