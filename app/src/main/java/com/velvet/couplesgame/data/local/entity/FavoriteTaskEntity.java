package com.velvet.couplesgame.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_tasks")
public class FavoriteTaskEntity {
    @PrimaryKey
    @NonNull
    public String cardId;
    @NonNull
    public String snapshotText;
    public int tier;
    public long savedAt;

    public FavoriteTaskEntity(@NonNull String cardId,
                              @NonNull String snapshotText,
                              int tier,
                              long savedAt) {
        this.cardId = cardId;
        this.snapshotText = snapshotText;
        this.tier = tier;
        this.savedAt = savedAt;
    }
}
