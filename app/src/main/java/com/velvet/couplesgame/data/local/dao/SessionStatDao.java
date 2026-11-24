package com.velvet.couplesgame.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import java.util.List;

@Dao
public interface SessionStatDao {
    @Query("SELECT * FROM session_stats ORDER BY playedAt DESC")
    LiveData<List<SessionStatEntity>> observeHistory();

    @Insert
    long insert(SessionStatEntity entity);

    @Query("DELETE FROM session_stats")
    void clearAll();
}
