package com.icloud;

import com.icloud.language.LanguageClient;
import com.icloud.model.EntitySentiment;
import com.icloud.model.Tweet;
import com.icloud.serde.AvroSerdes;
import com.icloud.serde.JsonSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CryptoTopology {

    private static final List<String> currencies = Arrays.asList("bitcoin", "ethereum");

    public static Topology build(LanguageClient languageClient) {
        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<byte[], Tweet> stream = builder.stream(
                "tweets",
                Consumed.with(Serdes.ByteArray(), new JsonSerdes<>(Tweet.class))
        );

        final KStream<byte[], Tweet> filtered =
                stream.filterNot((key, tweet) -> tweet.isRetweet());

        final Predicate<byte[], Tweet> englishTweets = (key, tweet) -> tweet.getLang().equals("en");
        final Predicate<byte[], Tweet> nonEnglishTweets = (key, tweet) -> !tweet.getLang().equals("en");
        final Map<String, KStream<byte[], Tweet>> branches = filtered.split(Named.as("tweets-"))
                .branch(englishTweets, Branched.as("en"))
                .branch(nonEnglishTweets, Branched.as("non-en"))
                .noDefaultBranch();

        final KStream<byte[], Tweet> englishStream = branches.get("tweets-en");
        final KStream<byte[], Tweet> nonEnglishStream = branches.get("tweets-non-en");

        final KStream<byte[], Tweet> translatedStream =
                nonEnglishStream.mapValues(tweet -> languageClient.translate(tweet, "en"));

        final KStream<byte[], Tweet> merged = englishStream.merge(translatedStream);

        final KStream<byte[], EntitySentiment> enriched =
                merged.flatMapValues(tweet -> {
                    List<EntitySentiment> results = languageClient.getEntitySentiment(tweet);
                    results.removeIf(entitySentiment -> !currencies.contains(entitySentiment.getEntity()));
                    return results;
                });

        enriched.print(Printed.<byte[], EntitySentiment>toSysOut().withLabel("enriched"));

        enriched.to("crypto-sentiment-without-schema-registry",
                Produced.with(Serdes.ByteArray(), com.mitchseymour.kafka.serialization.avro.AvroSerdes.get(EntitySentiment.class))
        );

        enriched.to(
                "crypto-sentiment",
                Produced.with(
                        Serdes.ByteArray(),
                        AvroSerdes.of("http://localhost:8081", false)
                )
        );

        return builder.build();
    }

}

