package com.velvet.couplesgame.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "boundary_rules")
public class BoundaryRuleEntity {
    @PrimaryKey
    @NonNull
    public String tagKey;
    @NonNull
    public String playerAStatus;
    @NonNull
    public String playerBStatus;

    public BoundaryRuleEntity(@NonNull String tagKey,
                              @NonNull String playerAStatus,
                              @NonNull String playerBStatus) {
        this.tagKey = tagKey;
        this.playerAStatus = playerAStatus;
        this.playerBStatus = playerBStatus;
    }

    public boolean isBlocked() {
        return "HARD_NO".equals(playerAStatus) || "HARD_NO".equals(playerBStatus);
    }

    public boolean isSoftLimit() {
        return !isBlocked() && ("SOFT_LIMIT".equals(playerAStatus) || "SOFT_LIMIT".equals(playerBStatus));
    }
}
