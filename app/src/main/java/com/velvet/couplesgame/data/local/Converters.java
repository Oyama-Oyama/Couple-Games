package com.velvet.couplesgame.data.local;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class Converters {
    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private Converters() {}

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        return GSON.toJson(list);
    }

    @TypeConverter
    public static List<String> toStringList(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        List<String> parsed = GSON.fromJson(json, STRING_LIST_TYPE);
        return parsed != null ? parsed : Collections.emptyList();
    }
}
