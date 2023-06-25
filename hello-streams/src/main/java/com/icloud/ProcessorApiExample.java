package com.icloud;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.Record;

import javax.lang.model.type.NullType;


public class ProcessorApiExample {

    public static void main(String[] args) {
        final Topology topology = new Topology();
        final class SayHelloProcessor implements Processor<NullType, String, NullType, NullType> {
            @Override
            public void process(Record<NullType, String> record) {
                System.out.println("(Processor API) Hello, " + record.value());
            }
        }
        topology.addSource("UserSource", "users")
                .addProcessor("SayHello", SayHelloProcessor::new, "UserSource")
        ;

        final KafkaStreams streams = new KafkaStreams(topology, Config.getConfig());
        streams.start();


    }
}
