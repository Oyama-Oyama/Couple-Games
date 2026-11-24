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
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
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
public final class SessionStatDao_Impl implements SessionStatDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SessionStatEntity> __insertionAdapterOfSessionStatEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public SessionStatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSessionStatEntity = new EntityInsertionAdapter<SessionStatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `session_stats` (`id`,`gameMode`,`playerACompleted`,`playerBCompleted`,`skipsUsed`,`playedAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final SessionStatEntity entity) {
        statement.bindLong(1, entity.id);
        if (entity.gameMode == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.gameMode);
        }
        statement.bindLong(3, entity.playerACompleted);
        statement.bindLong(4, entity.playerBCompleted);
        statement.bindLong(5, entity.skipsUsed);
        statement.bindLong(6, entity.playedAt);
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM session_stats";
        return _query;
      }
    };
  }

  @Override
  public long insert(final SessionStatEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfSessionStatEntity.insertAndReturnId(entity);
      __db.setTransactionSuccessful();
      return _result;
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
  public LiveData<List<SessionStatEntity>> observeHistory() {
    final String _sql = "SELECT * FROM session_stats ORDER BY playedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"session_stats"}, false, new Callable<List<SessionStatEntity>>() {
      @Override
      @Nullable
      public List<SessionStatEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGameMode = CursorUtil.getColumnIndexOrThrow(_cursor, "gameMode");
          final int _cursorIndexOfPlayerACompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "playerACompleted");
          final int _cursorIndexOfPlayerBCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "playerBCompleted");
          final int _cursorIndexOfSkipsUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "skipsUsed");
          final int _cursorIndexOfPlayedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "playedAt");
          final List<SessionStatEntity> _result = new ArrayList<SessionStatEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SessionStatEntity _item;
            final String _tmpGameMode;
            if (_cursor.isNull(_cursorIndexOfGameMode)) {
              _tmpGameMode = null;
            } else {
              _tmpGameMode = _cursor.getString(_cursorIndexOfGameMode);
            }
            final int _tmpPlayerACompleted;
            _tmpPlayerACompleted = _cursor.getInt(_cursorIndexOfPlayerACompleted);
            final int _tmpPlayerBCompleted;
            _tmpPlayerBCompleted = _cursor.getInt(_cursorIndexOfPlayerBCompleted);
            final int _tmpSkipsUsed;
            _tmpSkipsUsed = _cursor.getInt(_cursorIndexOfSkipsUsed);
            final long _tmpPlayedAt;
            _tmpPlayedAt = _cursor.getLong(_cursorIndexOfPlayedAt);
            _item = new SessionStatEntity(_tmpGameMode,_tmpPlayerACompleted,_tmpPlayerBCompleted,_tmpSkipsUsed,_tmpPlayedAt);
            _item.id = _cursor.getLong(_cursorIndexOfId);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
