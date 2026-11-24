package com.velvet.couplesgame.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "blind_matches")
public class BlindMatchEntity {
    @PrimaryKey
    @NonNull
    public String cardId;
    public boolean playerALiked;
    public boolean playerBLiked;
    public long updatedAt;

    public BlindMatchEntity(@NonNull String cardId,
                            boolean playerALiked,
                            boolean playerBLiked,
                            long updatedAt) {
        this.cardId = cardId;
        this.playerALiked = playerALiked;
        this.playerBLiked = playerBLiked;
        this.updatedAt = updatedAt;
    }

    public boolean isMutualMatch() {
        return playerALiked && playerBLiked;
    }
}
