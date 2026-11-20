package com.velvet.couplesgame.core.model;

import java.util.Collections;
import java.util.List;

public class TaskCard {
    public final String id;
    public final String deckId;
    public final int tier;
    public final String type;
    public final List<String> tags;
    public final int durationSec;
    public final String text;
    public final boolean isProcedural;
    public final boolean isCustom;

    public TaskCard(String id, String deckId, int tier, String type,
                    List<String> tags, int durationSec, String text,
                    boolean isProcedural, boolean isCustom) {
        this.id = id;
        this.deckId = deckId;
        this.tier = tier;
        this.type = type;
        this.tags = tags != null ? Collections.unmodifiableList(tags) : Collections.emptyList();
        this.durationSec = Math.max(0, durationSec);
        this.text = text != null ? text : "";
        this.isProcedural = isProcedural;
        this.isCustom = isCustom;
    }
}
