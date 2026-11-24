package com.velvet.couplesgame.core.i18n;

import android.content.Context;
import androidx.annotation.StringRes;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.model.TaskType;
import java.util.ArrayList;
import java.util.List;

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
            case "bite_lick": return R.string.tag_bite_lick;
            case "clothing_dare": return R.string.tag_clothing_dare;
            case "restraint_light": return R.string.tag_restraint_light;
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
    public static int resolveTierDescriptionRes(int tier) {
        switch (tier) {
            case 1: return R.string.tier_desc_1;
            case 2: return R.string.tier_desc_2;
            case 3: return R.string.tier_desc_3;
            case 4:
            default: return R.string.tier_desc_4;
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
        if ("FATE_CUBES".equals(gameModeKey)) return R.string.mode_fate_cubes;
        if ("LUCKY_WHEEL".equals(gameModeKey)) return R.string.mode_wheel;
        if ("COUPLE_MINES".equals(gameModeKey)) return R.string.mode_minesweeper;
        if ("DRAW_GUESS".equals(gameModeKey)) return R.string.mode_draw_guess;
        if ("DESIRE_MATCH".equals(gameModeKey)) return R.string.mode_blind_match;
        if ("SCRATCH_ADVENTURE".equals(gameModeKey)) return R.string.mode_scratch;
        if ("ROLEPLAY".equals(gameModeKey)) return R.string.mode_roleplay;
        if ("COUPLE_QUIZ".equals(gameModeKey)) return R.string.mode_quiz;
        if ("PARTY_DUEL".equals(gameModeKey)) return R.string.mode_party_duel;
        if ("HEARTBEAT_HOLD".equals(gameModeKey)) return R.string.mode_heartbeat;
        if ("VELVET_BLACKJACK".equals(gameModeKey)) return R.string.mode_blackjack;
        if ("VELVET_BOMB".equals(gameModeKey)) return R.string.mode_bomb;
        if ("BODY_MAP".equals(gameModeKey)) return R.string.mode_body_map;
        if ("DESIRE_AUCTION".equals(gameModeKey)) return R.string.mode_auction;
        if ("POSITIONS_ROULETTE".equals(gameModeKey)) return R.string.mode_positions;
        if ("FOREPLAY_ESCALATOR".equals(gameModeKey)) return R.string.mode_foreplay;
        if ("PROP_LAB".equals(gameModeKey)) return R.string.mode_props;
        if ("NAUGHTY_CONNECT4".equals(gameModeKey)) return R.string.mode_connect4;
        if ("DESIRE_LADDER".equals(gameModeKey)) return R.string.mode_ladder;
        // 12 social games (modeId.toUpperCase() keys written by SocialGameFragment)
        if ("CONVERSATION_STARTER".equals(gameModeKey)) return R.string.mode_conversation_starter;
        if ("LOVE_LANGUAGE".equals(gameModeKey)) return R.string.mode_love_language;
        if ("DAILY_QUESTION".equals(gameModeKey)) return R.string.mode_daily_question;
        if ("RIDDLE_ME".equals(gameModeKey)) return R.string.mode_riddle_me;
        if ("MEMORY_LANE".equals(gameModeKey)) return R.string.mode_memory_lane;
        if ("MOST_LIKELY_TO".equals(gameModeKey)) return R.string.mode_most_likely_to;
        if ("BUCKET_LIST".equals(gameModeKey)) return R.string.mode_bucket_list;
        if ("LDR_SYNC".equals(gameModeKey)) return R.string.mode_ldr_sync;
        if ("FORFEIT_WHEEL".equals(gameModeKey)) return R.string.mode_forfeit_wheel;
        if ("SCENARIO_CHOICE".equals(gameModeKey)) return R.string.mode_scenario_choice;
        if ("HOT_SEAT".equals(gameModeKey)) return R.string.mode_hot_seat;
        if ("SECRET_MISSION".equals(gameModeKey)) return R.string.mode_secret_mission;
        if ("DAILY_REVEAL".equals(gameModeKey)) return R.string.mode_daily_reveal;
        if ("DATE_PLANNER".equals(gameModeKey)) return R.string.mode_date_planner;
        if ("SEALED_LETTER".equals(gameModeKey)) return R.string.mode_sealed_letter;
        if ("LOVE_GRAPH".equals(gameModeKey)) return R.string.mode_love_graph;
        // A 项 · 引导式约会夜（纯显示标签；不是 GameModeCatalog 项，不影响 36 模式计数）
        if ("DATE_NIGHT".equals(gameModeKey)) return R.string.mode_date_night;
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
