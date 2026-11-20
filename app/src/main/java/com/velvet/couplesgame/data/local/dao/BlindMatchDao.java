package com.velvet.couplesgame.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.velvet.couplesgame.data.local.entity.BlindMatchEntity;
import java.util.List;

@Dao
public interface BlindMatchDao {
    @Query("SELECT * FROM blind_matches WHERE playerALiked = 1 AND playerBLiked = 1 ORDER BY updatedAt DESC")
    LiveData<List<BlindMatchEntity>> observeMutualMatches();

    @Query("SELECT * FROM blind_matches WHERE playerALiked = 1 AND playerBLiked = 1")
    List<BlindMatchEntity> getMutualMatchesSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(BlindMatchEntity entity);

    @Query("DELETE FROM blind_matches")
    void clearAll();
}
