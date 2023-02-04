package com.icloud;

import com.icloud.model.*;
import com.icloud.serialization.JsonSerdes;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

public class LeaderboardTopology {

    public static Topology build() {
        final StreamsBuilder builder = new StreamsBuilder();

        final Serde<ScoreEvent> scoreEventSerde = JsonSerdes.of(ScoreEvent.class);
        final Serde<Player> playerserde = JsonSerdes.of(Player.class);
        final Serde<Product> productSerde = JsonSerdes.of(Product.class);
        final Serde<Enriched> enrichedSerde = JsonSerdes.of(Enriched.class);


        final KStream<String, ScoreEvent> scoreEventsKStream =
                builder.stream("score-events", Consumed.with(Serdes.ByteArray(), scoreEventSerde))
                        .selectKey((key, value) -> value.playerId().toString());
        scoreEventsKStream.print(Printed.<String, ScoreEvent>toSysOut().withLabel("score-events"));

        final KTable<String, Player> playersKTable =
                builder.table("players", Consumed.with(Serdes.String(), playerserde));
        playersKTable.toStream().print(Printed.<String, Player>toSysOut().withLabel("players"));

        final GlobalKTable<String, Product> productsGKTable =
                builder.globalTable("products", Consumed.with(Serdes.String(), productSerde));

        final ValueJoiner<ScoreEvent, Player, ScoreWithPlayer> scorePlayerJoiner = ScoreWithPlayer::new;


        //TODO join with KTable
        final KStream<String, ScoreWithPlayer> withPlayersKStream =
                scoreEventsKStream.join(playersKTable, scorePlayerJoiner, Joined.with(
                        Serdes.String(),
                        scoreEventSerde,
                        playerserde
                ));
        withPlayersKStream.print(Printed.<String, ScoreWithPlayer>toSysOut().withLabel("with-players"));

        //TODO join with GlobalKTable
        final KeyValueMapper<String, ScoreWithPlayer, String> keyMapper =
                (key, value) -> value.scoreEvent().productId().toString();
        final ValueJoiner<ScoreWithPlayer, Product, Enriched> productJoiner = Enriched::new;

        final KStream<String, Enriched> withProductsKStream =
                withPlayersKStream.join(productsGKTable, keyMapper, productJoiner);

        withProductsKStream.print(Printed.<String, Enriched>toSysOut().withLabel("enriched"));

        //TODO groupBy
        final KGroupedStream<String, Enriched> grouped =
                withProductsKStream.groupBy((key, value) -> value.getProductId().toString(),
                        Grouped.with(Serdes.String(), enrichedSerde)
                );

        //TODO aggregate
        final Initializer<HighScores> highScoresInitializer = HighScores::new;

        final Aggregator<String, Enriched, HighScores> highScoresAdder =
                (key, value, aggregate) -> aggregate.add(value);

        final KTable<String, HighScores> highScores =
                grouped.aggregate(
                        highScoresInitializer,
                        highScoresAdder,
                        Materialized.<String, HighScores, KeyValueStore<Bytes, byte[]>>
                                // 저장소의 키 타입, 저장소의 값 타입, 상태 저장소의 타입
                                        as("leader-boards") // 저장소에 이름을 붙이면 외부에서 쿼리 가능
                                .withKeySerde(Serdes.String())
                                .withValueSerde(JsonSerdes.of(HighScores.class))
                );


        highScores.toStream().to("high-scores");
        return builder.build();
    }
}
