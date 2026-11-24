package com.velvet.couplesgame.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BoundaryDao_Impl implements BoundaryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BoundaryRuleEntity> __insertionAdapterOfBoundaryRuleEntity;

  private final EntityInsertionAdapter<BoundaryRuleEntity> __insertionAdapterOfBoundaryRuleEntity_1;

  public BoundaryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBoundaryRuleEntity = new EntityInsertionAdapter<BoundaryRuleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `boundary_rules` (`tagKey`,`playerAStatus`,`playerBStatus`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final BoundaryRuleEntity entity) {
        if (entity.tagKey == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.tagKey);
        }
        if (entity.playerAStatus == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.playerAStatus);
        }
        if (entity.playerBStatus == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.playerBStatus);
        }
      }
    };
    this.__insertionAdapterOfBoundaryRuleEntity_1 = new EntityInsertionAdapter<BoundaryRuleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `boundary_rules` (`tagKey`,`playerAStatus`,`playerBStatus`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final BoundaryRuleEntity entity) {
        if (entity.tagKey == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.tagKey);
        }
        if (entity.playerAStatus == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.playerAStatus);
        }
        if (entity.playerBStatus == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.playerBStatus);
        }
      }
    };
  }

  @Override
  public void upsert(final BoundaryRuleEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfBoundaryRuleEntity.insert(entity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertDefaults(final List<BoundaryRuleEntity> defaults) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfBoundaryRuleEntity_1.insert(defaults);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<BoundaryRuleEntity>> observeAll() {
    final String _sql = "SELECT * FROM boundary_rules ORDER BY tagKey ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"boundary_rules"}, false, new Callable<List<BoundaryRuleEntity>>() {
      @Override
      @Nullable
      public List<BoundaryRuleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTagKey = CursorUtil.getColumnIndexOrThrow(_cursor, "tagKey");
          final int _cursorIndexOfPlayerAStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "playerAStatus");
          final int _cursorIndexOfPlayerBStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "playerBStatus");
          final List<BoundaryRuleEntity> _result = new ArrayList<BoundaryRuleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BoundaryRuleEntity _item;
            final String _tmpTagKey;
            if (_cursor.isNull(_cursorIndexOfTagKey)) {
              _tmpTagKey = null;
            } else {
              _tmpTagKey = _cursor.getString(_cursorIndexOfTagKey);
            }
            final String _tmpPlayerAStatus;
            if (_cursor.isNull(_cursorIndexOfPlayerAStatus)) {
              _tmpPlayerAStatus = null;
            } else {
              _tmpPlayerAStatus = _cursor.getString(_cursorIndexOfPlayerAStatus);
            }
            final String _tmpPlayerBStatus;
            if (_cursor.isNull(_cursorIndexOfPlayerBStatus)) {
              _tmpPlayerBStatus = null;
            } else {
              _tmpPlayerBStatus = _cursor.getString(_cursorIndexOfPlayerBStatus);
            }
            _item = new BoundaryRuleEntity(_tmpTagKey,_tmpPlayerAStatus,_tmpPlayerBStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<BoundaryRuleEntity> getAllSync() {
    final String _sql = "SELECT * FROM boundary_rules ORDER BY tagKey ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfTagKey = CursorUtil.getColumnIndexOrThrow(_cursor, "tagKey");
      final int _cursorIndexOfPlayerAStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "playerAStatus");
      final int _cursorIndexOfPlayerBStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "playerBStatus");
      final List<BoundaryRuleEntity> _result = new ArrayList<BoundaryRuleEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final BoundaryRuleEntity _item;
        final String _tmpTagKey;
        if (_cursor.isNull(_cursorIndexOfTagKey)) {
          _tmpTagKey = null;
        } else {
          _tmpTagKey = _cursor.getString(_cursorIndexOfTagKey);
        }
        final String _tmpPlayerAStatus;
        if (_cursor.isNull(_cursorIndexOfPlayerAStatus)) {
          _tmpPlayerAStatus = null;
        } else {
          _tmpPlayerAStatus = _cursor.getString(_cursorIndexOfPlayerAStatus);
        }
        final String _tmpPlayerBStatus;
        if (_cursor.isNull(_cursorIndexOfPlayerBStatus)) {
          _tmpPlayerBStatus = null;
        } else {
          _tmpPlayerBStatus = _cursor.getString(_cursorIndexOfPlayerBStatus);
        }
        _item = new BoundaryRuleEntity(_tmpTagKey,_tmpPlayerAStatus,_tmpPlayerBStatus);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
