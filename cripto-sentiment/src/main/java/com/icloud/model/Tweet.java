package com.icloud.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tweet {
    private Long createdAt;
    private Long id;
    private String lang;
    private boolean retweet;
    private String text;


}
