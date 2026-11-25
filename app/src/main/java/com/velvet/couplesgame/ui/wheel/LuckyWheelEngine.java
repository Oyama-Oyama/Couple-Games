package com.velvet.couplesgame.ui.wheel;

import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java wheel sector builder and deterministic pointer-to-sector angle resolver.
 * Sector 0 starts at 12 o'clock (-90 deg in Android Canvas) and sweeps clockwise.
 */
public final class LuckyWheelEngine {
    private LuckyWheelEngine() {}

    /**
     * Resolves which sector sits directly underneath the fixed top (12 o'clock) pointer
     * after the wheel view has rotated clockwise by {@code viewRotationDeg}.
     */
    public static int resolveSelectedSectorIndex(float viewRotationDeg, int sectorCount) {
        if (sectorCount <= 0) return -1;
        float sectorAngle = 360f / sectorCount;
        float normalized = ((viewRotationDeg % 360f) + 360f) % 360f;
        // Rotating clockwise by `normalized` brings angle `(360 - normalized)` under 12 o'clock
        float pointerAngle = (360f - normalized) % 360f;
        int idx = (int) Math.floor(pointerAngle / sectorAngle);
        return Math.max(0, Math.min(sectorCount - 1, idx));
    }

    public static List<WheelSector> buildClassicMixSectors(String lblTruth,
                                                           String lblPlayful,
                                                           String lblFlirty,
                                                           String lblBold,
                                                           String lblDesire,
                                                           String lblAiWild) {
        List<WheelSector> list = new ArrayList<>();
        list.add(new WheelSector("s_truth", lblTruth, 1, TaskType.TRUTH, false, null));
        list.add(new WheelSector("s_playful", lblPlayful, 2, TaskType.DARE, false, null));
        list.add(new WheelSector("s_desire", lblDesire, 3, null, true, null));
        list.add(new WheelSector("s_flirty", lblFlirty, 3, TaskType.DARE, false, null));
        list.add(new WheelSector("s_ai", lblAiWild, 3, null, false, null));
        list.add(new WheelSector("s_bold", lblBold, 4, TaskType.DARE, false, null));
        list.add(new WheelSector("s_connect", lblTruth, 2, TaskType.CONNECTION, false, null));
        list.add(new WheelSector("s_desire_2", lblDesire, 4, null, true, null));
        return Collections.unmodifiableList(list);
    }

    public static List<WheelSector> buildFromDesireMatches(List<TaskCard> matches) {
        if (matches == null || matches.isEmpty()) return Collections.emptyList();
        List<WheelSector> list = new ArrayList<>();
        int count = Math.min(8, matches.size());
        for (int i = 0; i < count; i++) {
            TaskCard c = matches.get(i);
            String shortLabel = c.text.length() > 14 ? c.text.substring(0, 13) + "…" : c.text;
            list.add(new WheelSector(c.id, "♥ " + shortLabel, c.tier, c.type, true, c.text));
        }
        return list;
    }

    public static List<WheelSector> buildFromCustomOptions(List<String> options) {
        if (options == null || options.isEmpty()) return Collections.emptyList();
        List<WheelSector> list = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            list.add(new WheelSector("cust_" + i, opt, 2, TaskType.DARE, false, opt));
        }
        return list;
    }
}
