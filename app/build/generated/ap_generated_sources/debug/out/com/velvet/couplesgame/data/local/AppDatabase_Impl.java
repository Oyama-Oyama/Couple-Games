package com.velvet.couplesgame.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.velvet.couplesgame.data.local.dao.BlindMatchDao;
import com.velvet.couplesgame.data.local.dao.BlindMatchDao_Impl;
import com.velvet.couplesgame.data.local.dao.BoundaryDao;
import com.velvet.couplesgame.data.local.dao.BoundaryDao_Impl;
import com.velvet.couplesgame.data.local.dao.CustomTaskDao;
import com.velvet.couplesgame.data.local.dao.CustomTaskDao_Impl;
import com.velvet.couplesgame.data.local.dao.FavoriteTaskDao;
import com.velvet.couplesgame.data.local.dao.FavoriteTaskDao_Impl;
import com.velvet.couplesgame.data.local.dao.SessionStatDao;
import com.velvet.couplesgame.data.local.dao.SessionStatDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile BoundaryDao _boundaryDao;

  private volatile CustomTaskDao _customTaskDao;

  private volatile FavoriteTaskDao _favoriteTaskDao;

  private volatile BlindMatchDao _blindMatchDao;

  private volatile SessionStatDao _sessionStatDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `boundary_rules` (`tagKey` TEXT NOT NULL, `playerAStatus` TEXT NOT NULL, `playerBStatus` TEXT NOT NULL, PRIMARY KEY(`tagKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_tasks` (`id` TEXT NOT NULL, `deckId` TEXT NOT NULL, `tier` INTEGER NOT NULL, `type` TEXT NOT NULL, `tags` TEXT, `durationSec` INTEGER NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_tasks` (`cardId` TEXT NOT NULL, `snapshotText` TEXT NOT NULL, `tier` INTEGER NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`cardId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `blind_matches` (`cardId` TEXT NOT NULL, `playerALiked` INTEGER NOT NULL, `playerBLiked` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`cardId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `session_stats` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gameMode` TEXT NOT NULL, `playerACompleted` INTEGER NOT NULL, `playerBCompleted` INTEGER NOT NULL, `skipsUsed` INTEGER NOT NULL, `playedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e9af661f61149627541ea3ec7d844e07')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `boundary_rules`");
        db.execSQL("DROP TABLE IF EXISTS `custom_tasks`");
        db.execSQL("DROP TABLE IF EXISTS `favorite_tasks`");
        db.execSQL("DROP TABLE IF EXISTS `blind_matches`");
        db.execSQL("DROP TABLE IF EXISTS `session_stats`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsBoundaryRules = new HashMap<String, TableInfo.Column>(3);
        _columnsBoundaryRules.put("tagKey", new TableInfo.Column("tagKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBoundaryRules.put("playerAStatus", new TableInfo.Column("playerAStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBoundaryRules.put("playerBStatus", new TableInfo.Column("playerBStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBoundaryRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBoundaryRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBoundaryRules = new TableInfo("boundary_rules", _columnsBoundaryRules, _foreignKeysBoundaryRules, _indicesBoundaryRules);
        final TableInfo _existingBoundaryRules = TableInfo.read(db, "boundary_rules");
        if (!_infoBoundaryRules.equals(_existingBoundaryRules)) {
          return new RoomOpenHelper.ValidationResult(false, "boundary_rules(com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity).\n"
                  + " Expected:\n" + _infoBoundaryRules + "\n"
                  + " Found:\n" + _existingBoundaryRules);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomTasks = new HashMap<String, TableInfo.Column>(8);
        _columnsCustomTasks.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomTasks.put("deckId", new TableInfo.Column("deckId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomTasks.put("tier", new TableInfo.Column("tier", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomTasks.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomTasks.put("tags", new TableInfo.Column("tags", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomTasks.put("durationSec", new TableInfo.Column("durationSec", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomTasks.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomTasks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomTasks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomTasks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomTasks = new TableInfo("custom_tasks", _columnsCustomTasks, _foreignKeysCustomTasks, _indicesCustomTasks);
        final TableInfo _existingCustomTasks = TableInfo.read(db, "custom_tasks");
        if (!_infoCustomTasks.equals(_existingCustomTasks)) {
          return new RoomOpenHelper.ValidationResult(false, "custom_tasks(com.velvet.couplesgame.data.local.entity.CustomTaskEntity).\n"
                  + " Expected:\n" + _infoCustomTasks + "\n"
                  + " Found:\n" + _existingCustomTasks);
        }
        final HashMap<String, TableInfo.Column> _columnsFavoriteTasks = new HashMap<String, TableInfo.Column>(4);
        _columnsFavoriteTasks.put("cardId", new TableInfo.Column("cardId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavoriteTasks.put("snapshotText", new TableInfo.Column("snapshotText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavoriteTasks.put("tier", new TableInfo.Column("tier", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavoriteTasks.put("savedAt", new TableInfo.Column("savedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFavoriteTasks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFavoriteTasks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFavoriteTasks = new TableInfo("favorite_tasks", _columnsFavoriteTasks, _foreignKeysFavoriteTasks, _indicesFavoriteTasks);
        final TableInfo _existingFavoriteTasks = TableInfo.read(db, "favorite_tasks");
        if (!_infoFavoriteTasks.equals(_existingFavoriteTasks)) {
          return new RoomOpenHelper.ValidationResult(false, "favorite_tasks(com.velvet.couplesgame.data.local.entity.FavoriteTaskEntity).\n"
                  + " Expected:\n" + _infoFavoriteTasks + "\n"
                  + " Found:\n" + _existingFavoriteTasks);
        }
        final HashMap<String, TableInfo.Column> _columnsBlindMatches = new HashMap<String, TableInfo.Column>(4);
        _columnsBlindMatches.put("cardId", new TableInfo.Column("cardId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlindMatches.put("playerALiked", new TableInfo.Column("playerALiked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlindMatches.put("playerBLiked", new TableInfo.Column("playerBLiked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlindMatches.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBlindMatches = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBlindMatches = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBlindMatches = new TableInfo("blind_matches", _columnsBlindMatches, _foreignKeysBlindMatches, _indicesBlindMatches);
        final TableInfo _existingBlindMatches = TableInfo.read(db, "blind_matches");
        if (!_infoBlindMatches.equals(_existingBlindMatches)) {
          return new RoomOpenHelper.ValidationResult(false, "blind_matches(com.velvet.couplesgame.data.local.entity.BlindMatchEntity).\n"
                  + " Expected:\n" + _infoBlindMatches + "\n"
                  + " Found:\n" + _existingBlindMatches);
        }
        final HashMap<String, TableInfo.Column> _columnsSessionStats = new HashMap<String, TableInfo.Column>(6);
        _columnsSessionStats.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessionStats.put("gameMode", new TableInfo.Column("gameMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessionStats.put("playerACompleted", new TableInfo.Column("playerACompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessionStats.put("playerBCompleted", new TableInfo.Column("playerBCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessionStats.put("skipsUsed", new TableInfo.Column("skipsUsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessionStats.put("playedAt", new TableInfo.Column("playedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSessionStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSessionStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSessionStats = new TableInfo("session_stats", _columnsSessionStats, _foreignKeysSessionStats, _indicesSessionStats);
        final TableInfo _existingSessionStats = TableInfo.read(db, "session_stats");
        if (!_infoSessionStats.equals(_existingSessionStats)) {
          return new RoomOpenHelper.ValidationResult(false, "session_stats(com.velvet.couplesgame.data.local.entity.SessionStatEntity).\n"
                  + " Expected:\n" + _infoSessionStats + "\n"
                  + " Found:\n" + _existingSessionStats);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e9af661f61149627541ea3ec7d844e07", "2226a0973e8acbef42e2c5b56850e4b2");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "boundary_rules","custom_tasks","favorite_tasks","blind_matches","session_stats");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `boundary_rules`");
      _db.execSQL("DELETE FROM `custom_tasks`");
      _db.execSQL("DELETE FROM `favorite_tasks`");
      _db.execSQL("DELETE FROM `blind_matches`");
      _db.execSQL("DELETE FROM `session_stats`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BoundaryDao.class, BoundaryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CustomTaskDao.class, CustomTaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FavoriteTaskDao.class, FavoriteTaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BlindMatchDao.class, BlindMatchDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SessionStatDao.class, SessionStatDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BoundaryDao boundaryDao() {
    if (_boundaryDao != null) {
      return _boundaryDao;
    } else {
      synchronized(this) {
        if(_boundaryDao == null) {
          _boundaryDao = new BoundaryDao_Impl(this);
        }
        return _boundaryDao;
      }
    }
  }

  @Override
  public CustomTaskDao customTaskDao() {
    if (_customTaskDao != null) {
      return _customTaskDao;
    } else {
      synchronized(this) {
        if(_customTaskDao == null) {
          _customTaskDao = new CustomTaskDao_Impl(this);
        }
        return _customTaskDao;
      }
    }
  }

  @Override
  public FavoriteTaskDao favoriteTaskDao() {
    if (_favoriteTaskDao != null) {
      return _favoriteTaskDao;
    } else {
      synchronized(this) {
        if(_favoriteTaskDao == null) {
          _favoriteTaskDao = new FavoriteTaskDao_Impl(this);
        }
        return _favoriteTaskDao;
      }
    }
  }

  @Override
  public BlindMatchDao blindMatchDao() {
    if (_blindMatchDao != null) {
      return _blindMatchDao;
    } else {
      synchronized(this) {
        if(_blindMatchDao == null) {
          _blindMatchDao = new BlindMatchDao_Impl(this);
        }
        return _blindMatchDao;
      }
    }
  }

  @Override
  public SessionStatDao sessionStatDao() {
    if (_sessionStatDao != null) {
      return _sessionStatDao;
    } else {
      synchronized(this) {
        if(_sessionStatDao == null) {
          _sessionStatDao = new SessionStatDao_Impl(this);
        }
        return _sessionStatDao;
      }
    }
  }
}
