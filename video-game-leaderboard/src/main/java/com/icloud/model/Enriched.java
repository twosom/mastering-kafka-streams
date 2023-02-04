package com.icloud.model;

import lombok.Data;

@Data
public class Enriched implements Comparable<Enriched> {

    private final Long playerId;
    private final Long productId;
    private final String playerName;
    private final String gameName;
    private final Double score;

    public Enriched(final ScoreWithPlayer scoreEventWithPlayer, final Product product) {
        this.playerId = scoreEventWithPlayer.player().id();
        this.productId = product.id();
        this.playerName = scoreEventWithPlayer.player().name();
        this.gameName = product.name();
        this.score = scoreEventWithPlayer.scoreEvent().score();
    }

    @Override
    public int compareTo(final Enriched o) {
        return Double.compare(o.score, this.score);
    }
}
