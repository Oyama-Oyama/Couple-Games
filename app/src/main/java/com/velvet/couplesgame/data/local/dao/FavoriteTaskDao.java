package com.velvet.couplesgame.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.velvet.couplesgame.data.local.entity.FavoriteTaskEntity;
import java.util.List;

@Dao
public interface FavoriteTaskDao {
    @Query("SELECT * FROM favorite_tasks ORDER BY savedAt DESC")
    LiveData<List<FavoriteTaskEntity>> observeAll();

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tasks WHERE cardId = :cardId)")
    boolean isFavoriteSync(String cardId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FavoriteTaskEntity entity);

    @Query("DELETE FROM favorite_tasks WHERE cardId = :cardId")
    void deleteById(String cardId);
}
