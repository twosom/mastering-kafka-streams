package com.icloud.model;

public record Enriched(Long playerId, Long productId, String playerName, String gameName, Double score)
        implements Comparable<Enriched> {

    public static Enriched of(ScoreWithPlayer scoreWithPlayer, Product product) {
        Player player = scoreWithPlayer.player();
        return new Enriched(
                player.id(),
                product.id(),
                player.name(),
                product.name(),
                scoreWithPlayer.scoreEvent().score()
        );
    }

    @Override
    public int compareTo(final Enriched other) {
        return Double.compare(this.score, other.score);
    }
}
