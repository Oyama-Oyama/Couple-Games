package com.velvet.couplesgame.core.i18n;

import android.content.Context;
import androidx.annotation.StringRes;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.model.TaskType;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates internal English metadata keys (tags, tiers, task types, game modes) into
 * localized EN / ZH / JA labels so no raw developer tokens ever leak into the UI.
 */
public final class TagDisplayMapper {
    private TagDisplayMapper() {}

    @StringRes
    public static int resolveTagTitleRes(String tagKey) {
        if (tagKey == null) return R.string.tag_deep_talk;
        switch (tagKey) {
            case "deep_talk": return R.string.tag_deep_talk;
            case "playful": return R.string.tag_playful;
            case "eye_contact": return R.string.tag_eye_contact;
            case "physical_light": return R.string.tag_physical_light;
            case "blindfold": return R.string.tag_blindfold;
            case "roleplay": return R.string.tag_roleplay;
            case "public_space": return R.string.tag_public_space;
            case "alcohol": return R.string.tag_alcohol;
            case "indoor": return R.string.tag_indoor;
            case "timed": return R.string.tag_timed;
            case "custom": return R.string.tag_custom;
            default: return R.string.tag_playful;
        }
    }

    @StringRes
    public static int resolveTierNameRes(int tier) {
        switch (tier) {
            case 1: return R.string.tier_name_1;
            case 2: return R.string.tier_name_2;
            case 3: return R.string.tier_name_3;
            case 4:
            default: return R.string.tier_name_4;
        }
    }

    @StringRes
    public static int resolveTaskTypeRes(String taskType) {
        if (TaskType.TRUTH.equals(taskType)) return R.string.task_type_truth;
        if (TaskType.DARE.equals(taskType)) return R.string.task_type_dare;
        if (TaskType.CONNECTION.equals(taskType)) return R.string.task_type_connection;
        if (TaskType.DESIRE_MATCH.equals(taskType)) return R.string.task_type_desire;
        return R.string.task_type_dare;
    }

    @StringRes
    public static int resolveGameModeRes(String gameModeKey) {
        if ("COUPLE_BOARD".equals(gameModeKey)) return R.string.mode_chess;
        if ("LUCKY_WHEEL".equals(gameModeKey)) return R.string.mode_wheel;
        if ("COUPLE_MINES".equals(gameModeKey)) return R.string.mode_minesweeper;
        if ("DRAW_GUESS".equals(gameModeKey)) return R.string.mode_draw_guess;
        if ("DESIRE_MATCH".equals(gameModeKey)) return R.string.mode_blind_match;
        return R.string.mode_chess;
    }

    public static String formatTagList(Context ctx, List<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        List<String> labels = new ArrayList<>();
        for (String t : tags) {
            if ("indoor".equals(t) || "timed".equals(t)) continue;
            labels.add(ctx.getString(resolveTagTitleRes(t)));
            if (labels.size() >= 2) break;
        }
        return String.join(" · ", labels);
    }

    public static String formatCardMeta(Context ctx, int tier, String type, boolean isProcedural, boolean isCustom) {
        String tierLabel = ctx.getString(resolveTierNameRes(tier));
        String typeLabel = ctx.getString(resolveTaskTypeRes(type));
        String suffix = isProcedural
                ? " · " + ctx.getString(R.string.badge_velvet_spark)
                : (isCustom ? " · " + ctx.getString(R.string.badge_custom_card) : "");
        return tierLabel + " · " + typeLabel + suffix;
    }
}
