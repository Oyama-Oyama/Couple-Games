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
import com.velvet.couplesgame.data.local.Converters;
import com.velvet.couplesgame.data.local.entity.CustomTaskEntity;
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
public final class CustomTaskDao_Impl implements CustomTaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CustomTaskEntity> __insertionAdapterOfCustomTaskEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public CustomTaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCustomTaskEntity = new EntityInsertionAdapter<CustomTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `custom_tasks` (`id`,`deckId`,`tier`,`type`,`tags`,`durationSec`,`text`,`createdAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final CustomTaskEntity entity) {
        if (entity.id == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.id);
        }
        if (entity.deckId == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.deckId);
        }
        statement.bindLong(3, entity.tier);
        if (entity.type == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.type);
        }
        final String _tmp = Converters.fromStringList(entity.tags);
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp);
        }
        statement.bindLong(6, entity.durationSec);
        if (entity.text == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.text);
        }
        statement.bindLong(8, entity.createdAt);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM custom_tasks WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void upsert(final CustomTaskEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCustomTaskEntity.insert(entity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final String id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
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
  public LiveData<List<CustomTaskEntity>> observeAll() {
    final String _sql = "SELECT * FROM custom_tasks ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"custom_tasks"}, false, new Callable<List<CustomTaskEntity>>() {
      @Override
      @Nullable
      public List<CustomTaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeckId = CursorUtil.getColumnIndexOrThrow(_cursor, "deckId");
          final int _cursorIndexOfTier = CursorUtil.getColumnIndexOrThrow(_cursor, "tier");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfDurationSec = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSec");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CustomTaskEntity> _result = new ArrayList<CustomTaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomTaskEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpDeckId;
            if (_cursor.isNull(_cursorIndexOfDeckId)) {
              _tmpDeckId = null;
            } else {
              _tmpDeckId = _cursor.getString(_cursorIndexOfDeckId);
            }
            final int _tmpTier;
            _tmpTier = _cursor.getInt(_cursorIndexOfTier);
            final String _tmpType;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmpType = null;
            } else {
              _tmpType = _cursor.getString(_cursorIndexOfType);
            }
            final List<String> _tmpTags;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfTags);
            }
            _tmpTags = Converters.toStringList(_tmp);
            final int _tmpDurationSec;
            _tmpDurationSec = _cursor.getInt(_cursorIndexOfDurationSec);
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CustomTaskEntity(_tmpId,_tmpDeckId,_tmpTier,_tmpType,_tmpTags,_tmpDurationSec,_tmpText,_tmpCreatedAt);
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
  public List<CustomTaskEntity> getAllSync() {
    final String _sql = "SELECT * FROM custom_tasks ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfDeckId = CursorUtil.getColumnIndexOrThrow(_cursor, "deckId");
      final int _cursorIndexOfTier = CursorUtil.getColumnIndexOrThrow(_cursor, "tier");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
      final int _cursorIndexOfDurationSec = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSec");
      final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final List<CustomTaskEntity> _result = new ArrayList<CustomTaskEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final CustomTaskEntity _item;
        final String _tmpId;
        if (_cursor.isNull(_cursorIndexOfId)) {
          _tmpId = null;
        } else {
          _tmpId = _cursor.getString(_cursorIndexOfId);
        }
        final String _tmpDeckId;
        if (_cursor.isNull(_cursorIndexOfDeckId)) {
          _tmpDeckId = null;
        } else {
          _tmpDeckId = _cursor.getString(_cursorIndexOfDeckId);
        }
        final int _tmpTier;
        _tmpTier = _cursor.getInt(_cursorIndexOfTier);
        final String _tmpType;
        if (_cursor.isNull(_cursorIndexOfType)) {
          _tmpType = null;
        } else {
          _tmpType = _cursor.getString(_cursorIndexOfType);
        }
        final List<String> _tmpTags;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfTags)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfTags);
        }
        _tmpTags = Converters.toStringList(_tmp);
        final int _tmpDurationSec;
        _tmpDurationSec = _cursor.getInt(_cursorIndexOfDurationSec);
        final String _tmpText;
        if (_cursor.isNull(_cursorIndexOfText)) {
          _tmpText = null;
        } else {
          _tmpText = _cursor.getString(_cursorIndexOfText);
        }
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _item = new CustomTaskEntity(_tmpId,_tmpDeckId,_tmpTier,_tmpType,_tmpTags,_tmpDurationSec,_tmpText,_tmpCreatedAt);
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
