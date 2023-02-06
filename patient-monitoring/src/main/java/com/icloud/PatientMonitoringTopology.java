package com.icloud;

import com.icloud.model.BodyTemp;
import com.icloud.model.CombinedVitals;
import com.icloud.model.Pulse;
import com.icloud.serialization.JsonSerdes;
import com.icloud.timestamp.VitalTimestampExtractor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;

public class PatientMonitoringTopology {
    public static Topology build() {
        final StreamsBuilder builder = new StreamsBuilder();

        final Serde<String> stringSerde = Serdes.String();
        final Serde<Pulse> pulseSerde = JsonSerdes.of(Pulse.class);
        final Serde<BodyTemp> bodyTempSerde = JsonSerdes.of(BodyTemp.class);
        final Serde<CombinedVitals> combinedVitalsSerde = JsonSerdes.of(CombinedVitals.class);

        final KStream<String, Pulse> pulseEventsKStream = builder.stream("pulse-events",
                Consumed.with(stringSerde, pulseSerde)
                        .withTimestampExtractor(new VitalTimestampExtractor())
        );

        final KStream<String, BodyTemp> tempEventsKStream = builder.stream("body-temp-events",
                Consumed.with(stringSerde, bodyTempSerde)
                        .withTimestampExtractor(new VitalTimestampExtractor())
        );

        final TimeWindows tumblingWindow =
                TimeWindows.ofSizeAndGrace(Duration.ofSeconds(60), Duration.ofSeconds(5));

        final KTable<Windowed<String>, Long> pulseCounts = pulseEventsKStream.groupByKey()
                .windowedBy(tumblingWindow)
                .count(Materialized.as("pulse-counts"))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded().shutDownWhenFull()));

        pulseCounts.toStream()
                .print(Printed.<Windowed<String>, Long>toSysOut().withLabel("pulse-counts"));

        final KStream<String, Long> highPulseKStream = pulseCounts.toStream()
                .filter((key, value) -> value >= 100)
                .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value));

        final KStream<String, BodyTemp> highTempKStream =
                tempEventsKStream.filter((key, value) -> value.getTemperature() > 100.4);

        final ValueJoiner<Long, BodyTemp, CombinedVitals> valueJoiner =
                (pulseRate, bodyTemp) -> new CombinedVitals(pulseRate.intValue(), bodyTemp);

        final JoinWindows joinWindows =
                JoinWindows.ofTimeDifferenceAndGrace(Duration.ofSeconds(60), Duration.ofSeconds(10));

        final KStream<String, CombinedVitals> vitalsJoinedKStream =
                highPulseKStream.join(highTempKStream, valueJoiner, joinWindows,
                        StreamJoined.with(stringSerde, Serdes.Long(), bodyTempSerde)
                );

        vitalsJoinedKStream.to(
                "alerts",
                Produced.with(stringSerde, combinedVitalsSerde)
        );


        return builder.build();
    }
}
