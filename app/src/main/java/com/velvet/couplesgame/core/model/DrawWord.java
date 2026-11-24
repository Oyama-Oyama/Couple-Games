package com.velvet.couplesgame.core.model;

import java.util.Collections;
import java.util.List;

public class DrawWord {
    public final String word;
    public final int tier;
    public final List<String> tags;

    public DrawWord(String word, int tier, List<String> tags) {
        this.word = word;
        this.tier = tier;
        this.tags = tags != null ? Collections.unmodifiableList(tags) : Collections.emptyList();
    }
}
