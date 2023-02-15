package com.icloud;

import com.icloud.model.DigitalTwin;
import com.icloud.model.TurbineState;
import com.icloud.processor.DigitalTwinValueProcessorSupplier;
import com.icloud.serialization.JsonSerdes;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.ArrayList;
import java.util.Properties;

@Slf4j
public class CombinedApp {
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
        final StreamsBuilder builder = new StreamsBuilder();

        final Serde<String> stringSerde = Serdes.String();
        final Serde<TurbineState> turbineStateSerde = JsonSerdes.of(TurbineState.class);
        final Serde<DigitalTwin> digitalTwinSerde = JsonSerdes.of(DigitalTwin.class);

        final KStream<String, TurbineState> desiredStateEventsKStream =
                builder.stream("desired-state-events", Consumed.with(stringSerde, turbineStateSerde));
        final KStream<String, TurbineState> reportedStateEventsKStream =
                builder.stream("reported-state-events", Consumed.with(stringSerde, turbineStateSerde));
        final KStream<String, TurbineState> highWindsKStream =
                reportedStateEventsKStream.flatMapValues((key, reported) -> {
                            final var records = new ArrayList<TurbineState>();
                            records.add(reported);
                            if (reported.isHighWindAndPowerOn()) {
                                final TurbineState desireOff = TurbineState.desireOff(reported);
                                records.add(desireOff);
                            }
                            return records;
                        })
                        .merge(desiredStateEventsKStream);
        final StoreBuilder<KeyValueStore<String, DigitalTwin>> storeBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore("digital-twin-store"),
                        stringSerde,
                        digitalTwinSerde
                );
        builder.addStateStore(storeBuilder);
        highWindsKStream
                .processValues(
                        new DigitalTwinValueProcessorSupplier(),
                        "digital-twin-store"
                )
                .to("digital-twins", Produced.with(stringSerde, digitalTwinSerde));
        return builder.build();
    }

}
