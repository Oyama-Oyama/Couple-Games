package com.velvet.couplesgame.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.velvet.couplesgame.data.local.entity.CustomTaskEntity;
import java.util.List;

@Dao
public interface CustomTaskDao {
    @Query("SELECT * FROM custom_tasks ORDER BY createdAt DESC")
    LiveData<List<CustomTaskEntity>> observeAll();

    @Query("SELECT * FROM custom_tasks ORDER BY createdAt DESC")
    List<CustomTaskEntity> getAllSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CustomTaskEntity entity);

    @Query("DELETE FROM custom_tasks WHERE id = :id")
    void deleteById(String id);
}
