package com.velvet.couplesgame.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import java.util.List;

@Dao
public interface BoundaryDao {
    @Query("SELECT * FROM boundary_rules ORDER BY tagKey ASC")
    LiveData<List<BoundaryRuleEntity>> observeAll();

    @Query("SELECT * FROM boundary_rules ORDER BY tagKey ASC")
    List<BoundaryRuleEntity> getAllSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(BoundaryRuleEntity entity);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertDefaults(List<BoundaryRuleEntity> defaults);
}
