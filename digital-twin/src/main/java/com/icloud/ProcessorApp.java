package com.icloud;

import com.icloud.model.DigitalTwin;
import com.icloud.model.TurbineState;
import com.icloud.processor.DigitalTwinProcessor;
import com.icloud.processor.HighWindsFlatmapProcessor;
import com.icloud.serialization.JsonSerdes;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Properties;

@Slf4j
public class ProcessorApp {
    public static void main(String[] args) {
        Topology topology = getTopology();
        Properties props = getProperties();
        KafkaStreams streams = new KafkaStreams(topology, props);
        // clean up local state since many of the tutorials write to the same location
        // you should run this sparingly in production since it will force the state
        // store to be rebuilt on start up
        streams.cleanUp();

        log.info("Starting Digital Twin Streams App");
        streams.start();
        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        HostInfo hostInfo = new HostInfo("localhost", 8000);
        RestService service = new RestService(hostInfo, streams);
        log.info("Starting Digital Twin REST Service");
        service.start();
    }

    private static Properties getProperties() {
        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "localhost:8000");
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "1");
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, "0");
        return props;
    }

    private static Topology getTopology() {
        final Topology builder = new Topology();
        final Serde<String> stringSerde = Serdes.String();
        final Serde<TurbineState> turbineStateSerde = JsonSerdes.of(TurbineState.class);
        final Serde<DigitalTwin> digitalTwinSerde = JsonSerdes.of(DigitalTwin.class);

        builder.addSource(
                "Desired State Events",
                stringSerde.deserializer(),
                turbineStateSerde.deserializer(),
                "desired-state-events"
        );

        builder.addSource(
                "Reported State Events",
                stringSerde.deserializer(),
                turbineStateSerde.deserializer(),
                "reported-state-events"
        );

        builder.addProcessor(
                "High Winds Flatmap Processor",
                HighWindsFlatmapProcessor::new,
                "Reported State Events"
        );


        builder.addProcessor(
                "Digital Twin Processor",
                DigitalTwinProcessor::new,
                "High Winds Flatmap Processor", "Desired State Events"
        );

        final StoreBuilder<KeyValueStore<String, DigitalTwin>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("digital-twin-store"),
                stringSerde,
                digitalTwinSerde
        );

        builder.addStateStore(
                storeBuilder,
                "Digital Twin Processor"
        );

        builder.addSink(
                "Digital Twin Sink",
                "digital-twins",
                stringSerde.serializer(),
                digitalTwinSerde.serializer(),
                "Digital Twin Processor"
        );

        return builder;
    }
}