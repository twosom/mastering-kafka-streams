package com.icloud;

import com.icloud.model.*;
import com.icloud.serde.JsonSerdes;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

public class LeaderboardTopology {

    public static Topology build() {

        //TODO 사용할 Serde 변수 정의
        final StreamsBuilder builder = new StreamsBuilder();
        final Serde<String> stringSerde = Serdes.String();
        final JsonSerdes<Player> playerSerde = JsonSerdes.of(Player.class);
        final JsonSerdes<ScoreEvent> scoreEventSerde = JsonSerdes.of(ScoreEvent.class);


        final KStream<String, ScoreEvent> scoreEventStream = builder.stream("score-events",
                        Consumed.with(Serdes.ByteArray(), scoreEventSerde)
                )//TODO PlayerId 를 키로 변환 -> Repartition
                .selectKey((key, value) -> value.playerId().toString());

        final KTable<String, Player> playerTable = builder.table(
                "players",
                Consumed.with(stringSerde, playerSerde)
        );

        final GlobalKTable<String, Product> productGlobalTable = builder.globalTable(
                "products",
                Consumed.with(stringSerde, JsonSerdes.of(Product.class))
        );

        //TODO scoreEvent 와 player 를 Join
        final ValueJoiner<ScoreEvent, Player, ScoreWithPlayer> scorePlayerJoiner = ScoreWithPlayer::new;
        final Joined<String, ScoreEvent, Player> joinParams = Joined.with(stringSerde, scoreEventSerde, playerSerde);
        final KStream<String, ScoreWithPlayer> withPlayer = scoreEventStream.join(playerTable, scorePlayerJoiner, joinParams);

        //TODO withPlayer 와 product 를 Join
        final KeyValueMapper<String, ScoreWithPlayer, String> keyMapper =
                (leftKey, scoreWithPlayer) -> String.valueOf(scoreWithPlayer.scoreEvent().productId());
        final ValueJoiner<ScoreWithPlayer, Product, Enriched> productJoiner = Enriched::of;
        final KStream<String, Enriched> withProduct = withPlayer.join(productGlobalTable, keyMapper, productJoiner);

        final KGroupedStream<String, Enriched> grouped = withProduct.groupBy(
                (key, value) -> value.productId().toString(),
                Grouped.with(stringSerde, JsonSerdes.of(Enriched.class))
        );


        final Initializer<HighScores> highScoresInitializer = HighScores::new;
        final Aggregator<String, Enriched, HighScores> highScoresAdder =
                (key, value, aggregate) -> aggregate.add(value);

        KTable<String, HighScores> highScores = grouped.aggregate(
                highScoresInitializer,
                highScoresAdder,
                /*
                  TODO 3가지 Generic 타입을 지정.
                   1. 저장소 키 타입 -> String
                   2. 저장소 값 타입 -> HighScores
                   3. 저장소 타입 ->  KeyValueStore<Bytes, byte[]> 로 표현한 단순 키-값 저장소
                  */
                Materialized.<String, HighScores, KeyValueStore<Bytes, byte[]>>
                                as("leader-boards")
                        .withKeySerde(stringSerde)
                        .withValueSerde(JsonSerdes.of(HighScores.class))
        );

        return builder.build();
    }
}