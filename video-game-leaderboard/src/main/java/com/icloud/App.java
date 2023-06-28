package com.icloud;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.HostInfo;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class App {
    public static void main(String[] args) {
        String host = System.getProperty("host");
        Integer port = Integer.parseInt(System.getProperty("port"));
        String stateDir = System.getProperty("stateDir");
        String endpoint = String.format("%s:%s", host, port);
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("localhost:9092", "localhost:9093", "localhost:9094"));
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, endpoint);
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        System.out.println("Starting Videogame Leaderboard");
        final Topology topology = LeaderboardTopology.build();
        KafkaStreams streams = new KafkaStreams(topology, props);
//         close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        streams.setUncaughtExceptionHandler((t, e) -> System.err.println(e));
        // start streaming!
        streams.start();

        // start the REST service
        HostInfo hostInfo = new HostInfo(host, port);
        LeaderboardService service = new LeaderboardService(hostInfo, streams);
        service.start();
    }
}