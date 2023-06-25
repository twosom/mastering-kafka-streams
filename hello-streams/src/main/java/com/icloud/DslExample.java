package com.icloud;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;

public class DslExample {

    public static final String TOPIC = "users";

    public static void main(String[] args) {
        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<Void, String> stream = builder.stream(TOPIC);
        stream.foreach((Void key, String value) -> System.out.println("(DSL) Hello, " + value));
        final KafkaStreams streams = new KafkaStreams(builder.build(), Config.getConfig());
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }


}