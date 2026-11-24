package com.velvet.couplesgame.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.List;

@Entity(tableName = "custom_tasks")
public class CustomTaskEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String deckId;
    public int tier;
    @NonNull
    public String type;
    public List<String> tags;
    public int durationSec;
    @NonNull
    public String text;
    public long createdAt;

    public CustomTaskEntity(@NonNull String id,
                            @NonNull String deckId,
                            int tier,
                            @NonNull String type,
                            List<String> tags,
                            int durationSec,
                            @NonNull String text,
                            long createdAt) {
        this.id = id;
        this.deckId = deckId;
        this.tier = tier;
        this.type = type;
        this.tags = tags;
        this.durationSec = durationSec;
        this.text = text;
        this.createdAt = createdAt;
    }
}
