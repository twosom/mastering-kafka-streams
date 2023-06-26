package com.icloud.language;


import com.icloud.model.EntitySentiment;
import com.icloud.model.Tweet;

import java.util.List;

public interface LanguageClient {
    Tweet translate(Tweet tweet, String targetLanguage);

    List<EntitySentiment> getEntitySentiment(Tweet tweet);
}
