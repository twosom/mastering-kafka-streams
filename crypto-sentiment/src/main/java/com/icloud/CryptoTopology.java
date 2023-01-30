package com.icloud;

import com.icloud.language.LanguageClient;
import com.icloud.model.EntitySentiment;
import com.icloud.serialization.Tweet;
import com.icloud.serialization.avro.AvroSerdes;
import com.icloud.serialization.json.TweetSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CryptoTopology {
    private static final List<String> currencies = Arrays.asList("bitcoin", "ethereum");

    public static Topology buildV1() {
        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<byte[], Tweet> tweetsKStream = builder.stream("tweets",
                Consumed.with(Serdes.ByteArray(), new TweetSerdes()));
        tweetsKStream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-stream"));

        return builder.build();
    }

    public static Topology build(final LanguageClient languageClient) {
        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<byte[], Tweet> tweetsKStream =
                builder.stream("tweets", Consumed.with(Serdes.ByteArray(), new TweetSerdes()));


        // TODO source -> filter
        final KStream<byte[], Tweet> filteredTweetKStream =
                tweetsKStream.filterNot((key, value) -> value.isRetweet());

        final Predicate<byte[], Tweet> englishTweets =
                (key, tweet) -> tweet.getLang().equals("en");

        final Predicate<byte[], Tweet> nonEnglishTweets =
                (key, tweet) -> !tweet.getLang().equals("en");

        final Map<String, KStream<byte[], Tweet>> branches = filteredTweetKStream.split(Named.as("tweet"))
                .branch(englishTweets, Branched.as("-english"))
                .branch(nonEnglishTweets, Branched.as("-non-english"))
                .noDefaultBranch();

        final KStream<byte[], Tweet> englishKStream = branches.get("tweet-english");
        final KStream<byte[], Tweet> nonEnglishKStream = branches.get("tweet-non-english");

        final KStream<byte[], Tweet> translatedKStream =
                nonEnglishKStream.mapValues((tweet) -> languageClient.translate(tweet, "en"));

        final KStream<byte[], Tweet> mergedKStream = englishKStream.merge(translatedKStream);

        final KStream<byte[], EntitySentiment> enrichedKStream =
                mergedKStream.flatMapValues(tweet -> {
                    final List<EntitySentiment> results = languageClient.getEntitySentiment(tweet);
                    results.removeIf(entitySentiment ->
                            !currencies.contains(entitySentiment.getEntity()));
                    return results;
                });

        enrichedKStream.to("crypto-sentiment",
                Produced.with(Serdes.ByteArray(),
                        AvroSerdes.EntitySentiment("http://localhost:8081", false)));

        return builder.build();
    }
}