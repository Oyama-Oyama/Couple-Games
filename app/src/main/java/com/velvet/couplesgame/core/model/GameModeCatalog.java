package com.velvet.couplesgame.core.model;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import com.velvet.couplesgame.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Authoritative registry of all 36 Velvet Date Night game modes, ordered by Stimulation / Heat Rating (5 -> 1)
 * with commercial VIP subscription gating on the most intense / advanced modes.
 */
public enum GameModeCatalog {
    // 5-Flame Extreme Physical & Exploratory Modes (VIP Subscription)
    POSITIONS_ROULETTE(
            "positions_roulette", 5, true,
            R.id.action_home_to_positions,
            R.string.mode_positions, R.string.mode_positions_desc, R.string.heat_tag_extreme
    ),
    FOREPLAY_ESCALATOR(
            "foreplay_escalator", 5, true,
            R.id.action_home_to_foreplay,
            R.string.mode_foreplay, R.string.mode_foreplay_desc, R.string.heat_tag_extreme
    ),
    BODY_MAP_HUNT(
            "body_map_hunt", 5, true,
            R.id.action_home_to_body_map,
            R.string.mode_body_map, R.string.mode_body_map_desc, R.string.heat_tag_extreme
    ),
    DESIRE_LADDER(
            "desire_ladder", 5, true,
            R.id.action_home_to_ladder,
            R.string.mode_ladder, R.string.mode_ladder_desc, R.string.heat_tag_extreme
    ),
    DESIRE_AUCTION(
            "desire_auction", 5, true,
            R.id.action_home_to_auction,
            R.string.mode_auction, R.string.mode_auction_desc, R.string.heat_tag_extreme
    ),

    // 4-Flame Bold Physical & Competitive Modes (4 VIP + 2 Free Viral Hooks)
    PROP_SENSORY_LAB(
            "prop_sensory_lab", 4, true,
            R.id.action_home_to_props,
            R.string.mode_props, R.string.mode_props_desc, R.string.heat_tag_bold
    ),
    NAUGHTY_CONNECT4(
            "naughty_connect4", 4, true,
            R.id.action_home_to_connect4,
            R.string.mode_connect4, R.string.mode_connect4_desc, R.string.heat_tag_bold
    ),
    ROLEPLAY_THEATER(
            "roleplay_theater", 4, true,
            R.id.action_home_to_roleplay,
            R.string.mode_roleplay, R.string.mode_roleplay_desc, R.string.heat_tag_bold
    ),
    VELVET_BLACKJACK(
            "velvet_blackjack", 4, true,
            R.id.action_home_to_blackjack,
            R.string.mode_blackjack, R.string.mode_blackjack_desc, R.string.heat_tag_bold
    ),
    FATE_CUBES(
            "fate_cubes", 4, false,
            R.id.action_home_to_fate_cubes,
            R.string.mode_fate_cubes, R.string.mode_fate_cubes_desc, R.string.heat_tag_bold
    ),
    HEARTBEAT_HOLD(
            "heartbeat_hold", 4, false,
            R.id.action_home_to_heartbeat,
            R.string.mode_heartbeat, R.string.mode_heartbeat_desc, R.string.heat_tag_bold
    ),

    // 3-Flame Escalating Tabletop & Discovery Modes (Free)
    VELVET_BOMB(
            "velvet_bomb", 3, false,
            R.id.action_home_to_bomb,
            R.string.mode_bomb, R.string.mode_bomb_desc, R.string.heat_tag_spicy
    ),
    SCRATCH_ADVENTURE(
            "scratch_adventure", 3, false,
            R.id.action_home_to_scratch,
            R.string.mode_scratch, R.string.mode_scratch_desc, R.string.heat_tag_spicy
    ),
    COUPLE_BOARD(
            "couple_board", 3, false,
            R.id.action_home_to_couple_board,
            R.string.mode_chess, R.string.mode_chess_desc, R.string.heat_tag_spicy
    ),
    DESIRE_MATCH(
            "desire_match", 3, false,
            R.id.action_home_to_blind_match,
            R.string.mode_blind_match, R.string.mode_blind_match_desc, R.string.heat_tag_spicy
    ),
    LUCKY_WHEEL(
            "lucky_wheel", 3, false,
            R.id.action_home_to_lucky_wheel,
            R.string.mode_wheel, R.string.mode_wheel_desc, R.string.heat_tag_spicy
    ),

    // 2-Flame Flirty Icebreaker Modes (Free)
    COUPLE_MINES(
            "couple_mines", 2, false,
            R.id.action_home_to_couple_mines,
            R.string.mode_minesweeper, R.string.mode_minesweeper_desc, R.string.heat_tag_flirty
    ),
    PARTY_DUEL(
            "party_duel", 2, false,
            R.id.action_home_to_party_duel,
            R.string.mode_party_duel, R.string.mode_party_duel_desc, R.string.heat_tag_flirty
    ),
    COUPLE_QUIZ(
            "couple_quiz", 2, false,
            R.id.action_home_to_quiz,
            R.string.mode_quiz, R.string.mode_quiz_desc, R.string.heat_tag_flirty
    ),

    // 1-Flame Cozy Warm-Up Mode (Free)
    DRAW_GUESS(
            "draw_guess", 1, false,
            R.id.action_home_to_draw_guess,
            R.string.mode_draw_guess, R.string.mode_draw_guess_desc, R.string.heat_tag_warmup
    ),

    // ---- Social games sourced from comparable couple apps (12 new modes) ----
    // 4-Flame VIP advanced exploration
    SECRET_MISSION(
            "secret_mission", 4, true,
            R.id.action_social_secret_mission,
            R.string.mode_secret_mission, R.string.mode_secret_mission_desc, R.string.heat_tag_bold
    ),
    // 3-Flame: 2 VIP + 3 free
    SCENARIO_CHOICE(
            "scenario_choice", 3, true,
            R.id.action_social_scenario_choice,
            R.string.mode_scenario_choice, R.string.mode_scenario_choice_desc, R.string.heat_tag_spicy
    ),
    HOT_SEAT(
            "hot_seat", 3, true,
            R.id.action_social_hot_seat,
            R.string.mode_hot_seat, R.string.mode_hot_seat_desc, R.string.heat_tag_spicy
    ),
    FORFEIT_WHEEL(
            "forfeit_wheel", 3, false,
            R.id.action_social_forfeit_wheel,
            R.string.mode_forfeit_wheel, R.string.mode_forfeit_wheel_desc, R.string.heat_tag_spicy
    ),
    BUCKET_LIST(
            "bucket_list", 3, false,
            R.id.action_social_bucket_list,
            R.string.mode_bucket_list, R.string.mode_bucket_list_desc, R.string.heat_tag_spicy
    ),
    LDR_SYNC(
            "ldr_sync", 3, false,
            R.id.action_social_ldr_sync,
            R.string.mode_ldr_sync, R.string.mode_ldr_sync_desc, R.string.heat_tag_spicy
    ),
    // 2-Flame free icebreakers
    MOST_LIKELY_TO(
            "most_likely_to", 2, false,
            R.id.action_social_most_likely_to,
            R.string.mode_most_likely_to, R.string.mode_most_likely_to_desc, R.string.heat_tag_flirty
    ),
    MEMORY_LANE(
            "memory_lane", 2, false,
            R.id.action_social_memory_lane,
            R.string.mode_memory_lane, R.string.mode_memory_lane_desc, R.string.heat_tag_flirty
    ),
    // 1-Flame free warm-ups
    LOVE_LANGUAGE(
            "love_language", 1, false,
            R.id.action_social_love_language,
            R.string.mode_love_language, R.string.mode_love_language_desc, R.string.heat_tag_warmup
    ),
    DAILY_QUESTION(
            "daily_question", 1, false,
            R.id.action_social_daily_question,
            R.string.mode_daily_question, R.string.mode_daily_question_desc, R.string.heat_tag_warmup
    ),
    RIDDLE_ME(
            "riddle_me", 1, false,
            R.id.action_social_riddle_me,
            R.string.mode_riddle_me, R.string.mode_riddle_me_desc, R.string.heat_tag_warmup
    ),
    CONVERSATION_STARTER(
            "conversation_starter", 1, false,
            R.id.action_social_conversation_starter,
            R.string.mode_conversation_starter, R.string.mode_conversation_starter_desc, R.string.heat_tag_warmup
    ),
    DAILY_REVEAL(
            "daily_reveal", 1, false,
            R.id.action_home_to_daily_reveal,
            R.string.mode_daily_reveal, R.string.mode_daily_reveal_desc, R.string.heat_tag_warmup
    ),
    DATE_PLANNER(
            "date_planner", 2, false,
            R.id.action_home_to_date_planner,
            R.string.mode_date_planner, R.string.mode_date_planner_desc, R.string.heat_tag_flirty
    ),
    SEALED_LETTER(
            "sealed_letter", 1, false,
            R.id.action_home_to_sealed_letter,
            R.string.mode_sealed_letter, R.string.mode_sealed_letter_desc, R.string.heat_tag_warmup
    ),
    LOVE_GRAPH(
            "love_graph", 1, false,
            R.id.action_home_to_love_graph,
            R.string.mode_love_graph, R.string.mode_love_graph_desc, R.string.heat_tag_warmup
    );

    public enum SortFilter {
        HOTTEST_FIRST,   // 5 flames -> 1 flame
        GENTLE_TO_WILD,  // 1 flame -> 5 flames
        FREE_ONLY,
        VIP_ONLY
    }

    public final String id;
    public final int heatRating; // 1..5
    public final boolean vipRequired;
    @IdRes public final int navActionId;
    @StringRes public final int titleRes;
    @StringRes public final int descRes;
    @StringRes public final int heatTagRes;

    GameModeCatalog(String id, int heatRating, boolean vipRequired,
                    @IdRes int navActionId, @StringRes int titleRes,
                    @StringRes int descRes, @StringRes int heatTagRes) {
        this.id = id;
        this.heatRating = heatRating;
        this.vipRequired = vipRequired;
        this.navActionId = navActionId;
        this.titleRes = titleRes;
        this.descRes = descRes;
        this.heatTagRes = heatTagRes;
    }

    public String flameIndicator() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < heatRating; i++) sb.append("🔥");
        return sb.toString();
    }

    public static GameModeCatalog fromId(String id) {
        if (id == null) return null;
        for (GameModeCatalog m : values()) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }

    public static List<GameModeCatalog> queryModes(SortFilter filter) {
        List<GameModeCatalog> list = new ArrayList<>();
        for (GameModeCatalog m : values()) {
            if (filter == SortFilter.FREE_ONLY && m.vipRequired) continue;
            if (filter == SortFilter.VIP_ONLY && !m.vipRequired) continue;
            list.add(m);
        }
        if (filter == SortFilter.GENTLE_TO_WILD) {
            Collections.sort(list, Comparator.comparingInt(a -> a.heatRating));
        } else {
            Collections.sort(list, (a, b) -> Integer.compare(b.heatRating, a.heatRating));
        }
        return list;
    }

    public static List<GameModeCatalog> sliceForDisplay(List<GameModeCatalog> modes,
                                                        boolean expanded,
                                                        int collapsedLimit) {
        if (modes == null || modes.isEmpty()) return Collections.emptyList();
        if (expanded || collapsedLimit <= 0 || modes.size() <= collapsedLimit) {
            return new ArrayList<>(modes);
        }
        return new ArrayList<>(modes.subList(0, collapsedLimit));
    }
}
