package com.velvet.couplesgame.core.model;

import java.util.Collections;
import java.util.List;

public class DeckMeta {
    public final String id;
    public final String title;
    public final String subtitle;
    public final int tier;
    public final boolean premium;
    public final List<TaskCard> cards;

    public DeckMeta(String id, String title, String subtitle, int tier, boolean premium, List<TaskCard> cards) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.tier = tier;
        this.premium = premium;
        this.cards = cards != null ? Collections.unmodifiableList(cards) : Collections.emptyList();
    }
}
