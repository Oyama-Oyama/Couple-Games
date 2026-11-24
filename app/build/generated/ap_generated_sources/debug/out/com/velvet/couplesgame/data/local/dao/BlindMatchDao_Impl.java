package com.velvet.couplesgame.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.velvet.couplesgame.data.local.entity.BlindMatchEntity;
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
public final class BlindMatchDao_Impl implements BlindMatchDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BlindMatchEntity> __insertionAdapterOfBlindMatchEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public BlindMatchDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBlindMatchEntity = new EntityInsertionAdapter<BlindMatchEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `blind_matches` (`cardId`,`playerALiked`,`playerBLiked`,`updatedAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final BlindMatchEntity entity) {
        if (entity.cardId == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.cardId);
        }
        final int _tmp = entity.playerALiked ? 1 : 0;
        statement.bindLong(2, _tmp);
        final int _tmp_1 = entity.playerBLiked ? 1 : 0;
        statement.bindLong(3, _tmp_1);
        statement.bindLong(4, entity.updatedAt);
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM blind_matches";
        return _query;
      }
    };
  }

  @Override
  public void upsert(final BlindMatchEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfBlindMatchEntity.insert(entity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void clearAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearAll.release(_stmt);
    }
  }

  @Override
  public LiveData<List<BlindMatchEntity>> observeMutualMatches() {
    final String _sql = "SELECT * FROM blind_matches WHERE playerALiked = 1 AND playerBLiked = 1 ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"blind_matches"}, false, new Callable<List<BlindMatchEntity>>() {
      @Override
      @Nullable
      public List<BlindMatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
          final int _cursorIndexOfPlayerALiked = CursorUtil.getColumnIndexOrThrow(_cursor, "playerALiked");
          final int _cursorIndexOfPlayerBLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "playerBLiked");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<BlindMatchEntity> _result = new ArrayList<BlindMatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BlindMatchEntity _item;
            final String _tmpCardId;
            if (_cursor.isNull(_cursorIndexOfCardId)) {
              _tmpCardId = null;
            } else {
              _tmpCardId = _cursor.getString(_cursorIndexOfCardId);
            }
            final boolean _tmpPlayerALiked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPlayerALiked);
            _tmpPlayerALiked = _tmp != 0;
            final boolean _tmpPlayerBLiked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPlayerBLiked);
            _tmpPlayerBLiked = _tmp_1 != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new BlindMatchEntity(_tmpCardId,_tmpPlayerALiked,_tmpPlayerBLiked,_tmpUpdatedAt);
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
  public List<BlindMatchEntity> getMutualMatchesSync() {
    final String _sql = "SELECT * FROM blind_matches WHERE playerALiked = 1 AND playerBLiked = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
      final int _cursorIndexOfPlayerALiked = CursorUtil.getColumnIndexOrThrow(_cursor, "playerALiked");
      final int _cursorIndexOfPlayerBLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "playerBLiked");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final List<BlindMatchEntity> _result = new ArrayList<BlindMatchEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final BlindMatchEntity _item;
        final String _tmpCardId;
        if (_cursor.isNull(_cursorIndexOfCardId)) {
          _tmpCardId = null;
        } else {
          _tmpCardId = _cursor.getString(_cursorIndexOfCardId);
        }
        final boolean _tmpPlayerALiked;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfPlayerALiked);
        _tmpPlayerALiked = _tmp != 0;
        final boolean _tmpPlayerBLiked;
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfPlayerBLiked);
        _tmpPlayerBLiked = _tmp_1 != 0;
        final long _tmpUpdatedAt;
        _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
        _item = new BlindMatchEntity(_tmpCardId,_tmpPlayerALiked,_tmpPlayerBLiked,_tmpUpdatedAt);
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
