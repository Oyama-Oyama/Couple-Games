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
import com.velvet.couplesgame.data.local.entity.FavoriteTaskEntity;
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
public final class FavoriteTaskDao_Impl implements FavoriteTaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FavoriteTaskEntity> __insertionAdapterOfFavoriteTaskEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public FavoriteTaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFavoriteTaskEntity = new EntityInsertionAdapter<FavoriteTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `favorite_tasks` (`cardId`,`snapshotText`,`tier`,`savedAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final FavoriteTaskEntity entity) {
        if (entity.cardId == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.cardId);
        }
        if (entity.snapshotText == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.snapshotText);
        }
        statement.bindLong(3, entity.tier);
        statement.bindLong(4, entity.savedAt);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM favorite_tasks WHERE cardId = ?";
        return _query;
      }
    };
  }

  @Override
  public void upsert(final FavoriteTaskEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfFavoriteTaskEntity.insert(entity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final String cardId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    if (cardId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, cardId);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
    }
  }

  @Override
  public LiveData<List<FavoriteTaskEntity>> observeAll() {
    final String _sql = "SELECT * FROM favorite_tasks ORDER BY savedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"favorite_tasks"}, false, new Callable<List<FavoriteTaskEntity>>() {
      @Override
      @Nullable
      public List<FavoriteTaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
          final int _cursorIndexOfSnapshotText = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotText");
          final int _cursorIndexOfTier = CursorUtil.getColumnIndexOrThrow(_cursor, "tier");
          final int _cursorIndexOfSavedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "savedAt");
          final List<FavoriteTaskEntity> _result = new ArrayList<FavoriteTaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FavoriteTaskEntity _item;
            final String _tmpCardId;
            if (_cursor.isNull(_cursorIndexOfCardId)) {
              _tmpCardId = null;
            } else {
              _tmpCardId = _cursor.getString(_cursorIndexOfCardId);
            }
            final String _tmpSnapshotText;
            if (_cursor.isNull(_cursorIndexOfSnapshotText)) {
              _tmpSnapshotText = null;
            } else {
              _tmpSnapshotText = _cursor.getString(_cursorIndexOfSnapshotText);
            }
            final int _tmpTier;
            _tmpTier = _cursor.getInt(_cursorIndexOfTier);
            final long _tmpSavedAt;
            _tmpSavedAt = _cursor.getLong(_cursorIndexOfSavedAt);
            _item = new FavoriteTaskEntity(_tmpCardId,_tmpSnapshotText,_tmpTier,_tmpSavedAt);
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
  public boolean isFavoriteSync(final String cardId) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM favorite_tasks WHERE cardId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (cardId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, cardId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final boolean _result;
      if (_cursor.moveToFirst()) {
        final int _tmp;
        _tmp = _cursor.getInt(0);
        _result = _tmp != 0;
      } else {
        _result = false;
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
