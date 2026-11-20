package com.velvet.couplesgame.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.velvet.couplesgame.data.local.dao.BlindMatchDao;
import com.velvet.couplesgame.data.local.dao.BoundaryDao;
import com.velvet.couplesgame.data.local.dao.CustomTaskDao;
import com.velvet.couplesgame.data.local.dao.FavoriteTaskDao;
import com.velvet.couplesgame.data.local.dao.SessionStatDao;
import com.velvet.couplesgame.data.local.entity.BlindMatchEntity;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.data.local.entity.CustomTaskEntity;
import com.velvet.couplesgame.data.local.entity.FavoriteTaskEntity;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;

@Database(
        entities = {
                BoundaryRuleEntity.class,
                CustomTaskEntity.class,
                FavoriteTaskEntity.class,
                BlindMatchEntity.class,
                SessionStatEntity.class
        },
        version = 1,
        exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract BoundaryDao boundaryDao();
    public abstract CustomTaskDao customTaskDao();
    public abstract FavoriteTaskDao favoriteTaskDao();
    public abstract BlindMatchDao blindMatchDao();
    public abstract SessionStatDao sessionStatDao();

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "velvet_couples_offline.db"
        ).build();
    }
}
