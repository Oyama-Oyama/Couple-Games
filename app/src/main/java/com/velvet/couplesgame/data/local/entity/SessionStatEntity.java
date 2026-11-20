package com.velvet.couplesgame.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "session_stats")
public class SessionStatEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String gameMode;
    public int playerACompleted;
    public int playerBCompleted;
    public int skipsUsed;
    public long playedAt;

    public SessionStatEntity(@NonNull String gameMode,
                             int playerACompleted,
                             int playerBCompleted,
                             int skipsUsed,
                             long playedAt) {
        this.gameMode = gameMode;
        this.playerACompleted = playerACompleted;
        this.playerBCompleted = playerBCompleted;
        this.skipsUsed = skipsUsed;
        this.playedAt = playedAt;
    }
}
