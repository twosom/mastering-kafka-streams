package com.icloud.model;

import java.util.List;
import java.util.TreeSet;

public record HighScores(TreeSet<Enriched> highScores) {
    public HighScores() {
        this(new TreeSet<>());
    }

    public HighScores add(final Enriched enriched) {
        highScores.add(enriched);
        if (highScores.size() > 3)
            highScores.remove(highScores.last());
        return this;
    }

    public List<Enriched> toList() {
        return highScores.stream().toList();
    }
}
