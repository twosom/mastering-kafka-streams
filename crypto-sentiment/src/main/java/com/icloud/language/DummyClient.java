package com.icloud.language;

import com.icloud.model.EntitySentiment;
import com.icloud.serialization.Tweet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DummyClient implements LanguageClient {

    @Override
    public Tweet translate(final Tweet tweet, final String targetLanguage) {
        tweet.setText("Translated " + tweet.getText());
        return tweet;
    }

    @Override
    public List<EntitySentiment> getEntitySentiment(final Tweet tweet) {
        List<EntitySentiment> result = new ArrayList<>();
        String[] words = tweet.getText().toLowerCase().replace("#", " ").split(" ");
        for (final String entity : words) {
            EntitySentiment entitySentiment = EntitySentiment.newBuilder()
                    .setCreatedAt(tweet.getCreatedAt())
                    .setId(tweet.getId())
                    .setEntity(entity)
                    .setText(tweet.getText())
                    .setSalience(randomDouble())
                    .setSentimentScore(randomDouble())
                    .setSentimentMagnitude(randomDouble())
                    .build();
            result.add(entitySentiment);
        }
        return result;
    }

    private Double randomDouble() {
        return ThreadLocalRandom.current().nextDouble(0, 1);
    }
}
