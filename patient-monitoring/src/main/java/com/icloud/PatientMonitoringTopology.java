package com.icloud;

import com.icloud.model.BodyTemp;
import com.icloud.model.CombinedVitals;
import com.icloud.model.Pulse;
import com.icloud.serde.JsonSerdes;
import com.icloud.timestamp.VitalTimestampExtractor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;

public class PatientMonitoringTopology {

    public static Topology build() {
        final StreamsBuilder builder = new StreamsBuilder();
        final Serde<String> stringSerde = Serdes.String();
        final Serde<Pulse> pulseSerdes = JsonSerdes.of(Pulse.class);
        final Serde<BodyTemp> bodyTempSerdes = JsonSerdes.of(BodyTemp.class);

        //TODO pulse-events <stream>
        final Consumed<String, Pulse> pulseConsumerOptions =
                Consumed.with(stringSerde, pulseSerdes)
                        .withTimestampExtractor(new VitalTimestampExtractor());
        final KStream<String, Pulse> pulseEvents = builder.stream("pulse-events", pulseConsumerOptions);

        //TODO body-temp-events <stream>
        final Consumed<String, BodyTemp> bodyTempConsumerOptions =
                Consumed.with(stringSerde, bodyTempSerdes)
                        .withTimestampExtractor(new VitalTimestampExtractor());
        final KStream<String, BodyTemp> tempEvents = builder.stream("body-temp-events", bodyTempConsumerOptions);

        //TODO 텀블링 윈도우 정의
        final var tumblingWindow = TimeWindows.ofSizeAndGrace(
                        Duration.ofMinutes(1),
                        Duration.ofSeconds(5)
                )
                .advanceBy(Duration.ofSeconds(3L));

        final KTable<Windowed<String>, Long> pulseCountsTable = pulseEvents
                .groupByKey() //TODO Record Grouping 은 집계 수행하기 위한 전제 조건 -> 더군다나 이미 원하는 필드를 key 로 사용 중이기 때문에, groupByKey 사용
                .windowedBy(tumblingWindow)
                .count(Materialized.as("pulse-counts"))
                .suppress(
                        //TODO 심박수의 최종 결과만 원하므로, #untilWindowsCloses
                        Suppressed.untilWindowCloses(
                                //TODO 키 공간이 상대적으로 작을 것으로 예상, 따라서 제거한 레코드 보관 시, 가능한 많은 Heap 할당
                                Suppressed.BufferConfig.unbounded()
                                        //TODO 만약, 결과를 일찍 내보내는 경우 -> OOM 등으로 예외가 발생한 경우,
                                        // 결과를 일찍 내보내길 원하지 않기 때문에 종료
                                        .shutDownWhenFull()
                        )
                );

        pulseCountsTable.toStream()
                .print(Printed.<Windowed<String>, Long>toSysOut().withLabel("pulse-counts"));

        KStream<String, Long> highPulse = pulseCountsTable.toStream()
                .filter((key, value) -> value >= 100)
                .selectKey((key, value) -> key.key());

        highPulse.print(Printed.<String, Long>toSysOut().withLabel("high-pulse"));

        KStream<String, BodyTemp> highTemp = tempEvents.filter((key, value) -> value.temperature() > 100.4);

        final ValueJoiner<Long, BodyTemp, CombinedVitals> valueJoiner =
                (pulseRate, bodyTemp) -> new CombinedVitals(pulseRate.intValue(), bodyTemp);

        JoinWindows joinWindows = JoinWindows.ofTimeDifferenceAndGrace(
                Duration.ofMinutes(1),
                Duration.ofSeconds(10)
        );
        KStream<String, CombinedVitals> vitalsJoined = highPulse.join(
                highTemp, valueJoiner, joinWindows,
                StreamJoined.with(stringSerde, Serdes.Long(), bodyTempSerdes)
        );

        //TODO 각 환자의 현재 Alert 정보를 상태로 저장

        KTable<String, CombinedVitals> alertsTable = vitalsJoined.toTable(
                Materialized.<String, CombinedVitals, KeyValueStore<Bytes, byte[]>>
                                as("alerts")
                        .withKeySerde(stringSerde)
                        .withValueSerde(JsonSerdes.of(CombinedVitals.class))
        );

        KStream<String, CombinedVitals> alertsStream = alertsTable.toStream();
        alertsStream.print(Printed.<String, CombinedVitals>toSysOut().withLabel("alerts"));
        alertsStream.to(
                "alerts",
                Produced.with(stringSerde, JsonSerdes.of(CombinedVitals.class))
        );


        return builder.build();
    }

}