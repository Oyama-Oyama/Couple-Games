package com.velvet.couplesgame.core.reward;

import android.content.Context;
import android.content.SharedPreferences;
import com.velvet.couplesgame.core.engine.ConsentFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Local-only storage for auction rewards. */
public final class RewardVoucherManager {
    private static final String PREF_NAME = "velvet_reward_vouchers";
    private static final String KEY_PREFIX = "voucher.";
    private static final String TITLE = ".title";
    private static final String DESCRIPTION = ".description";
    private static final String WINNER = ".winner";
    private static final String SOURCE_ID = ".source_id";
    private static final String TIER = ".tier";
    private static final String TAGS = ".tags";
    private static final String CREATED_AT = ".created_at";
    private static final String REDEEMED_AT = ".redeemed_at";
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 600;
    private static final int MAX_NAME_LENGTH = 48;

    private final SharedPreferences preferences;

    public RewardVoucherManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean saveVoucher(String sourceContractId, String title, String description,
                               String winnerName, int tier, Collection<String> tags) {
        String cleanSourceId = requireText(sourceContractId, "sourceContractId", 120);
        String cleanTitle = requireText(title, "title", MAX_TITLE_LENGTH);
        String cleanDescription = requireText(description, "description", MAX_DESCRIPTION_LENGTH);
        String cleanWinner = requireText(winnerName, "winnerName", MAX_NAME_LENGTH);
        if (tier < 1 || tier > 4) {
            throw new IllegalArgumentException("tier must be between 1 and 4");
        }

        String id = UUID.randomUUID().toString();
        long createdAt = System.currentTimeMillis();
        SharedPreferences.Editor editor = preferences.edit()
                .putString(key(id, TITLE), cleanTitle)
                .putString(key(id, DESCRIPTION), cleanDescription)
                .putString(key(id, WINNER), cleanWinner)
                .putString(key(id, SOURCE_ID), cleanSourceId)
                .putInt(key(id, TIER), tier)
                .putString(key(id, TAGS), encodeTags(tags))
                .putLong(key(id, CREATED_AT), createdAt)
                .putLong(key(id, REDEEMED_AT), 0L);
        return editor.commit();
    }

    public List<RewardVoucher> getAvailable(ConsentFilter consentFilter, int maxTier) {
        int tierCeiling = Math.max(1, Math.min(4, maxTier));
        List<RewardVoucher> available = new ArrayList<>();
        for (RewardVoucher voucher : getAll()) {
            if (voucher.isRedeemed() || voucher.tier > tierCeiling) continue;
            if (consentFilter != null && !consentFilter.isAllowed(voucher.tags)) continue;
            available.add(voucher);
        }
        return available;
    }

    public List<RewardVoucher> getAll() {
        Map<String, ?> stored = preferences.getAll();
        List<RewardVoucher> vouchers = new ArrayList<>();
        for (String titleKey : stored.keySet()) {
            if (!titleKey.startsWith(KEY_PREFIX) || !titleKey.endsWith(TITLE)) continue;
            String id = titleKey.substring(KEY_PREFIX.length(), titleKey.length() - TITLE.length());
            if (id.isEmpty()) continue;
            Object titleValue = stored.get(titleKey);
            if (!(titleValue instanceof String)) continue;
            vouchers.add(new RewardVoucher(
                    id,
                    stringValue(stored, key(id, SOURCE_ID), ""),
                    (String) titleValue,
                    stringValue(stored, key(id, DESCRIPTION), ""),
                    stringValue(stored, key(id, WINNER), ""),
                    intValue(stored, key(id, TIER), 1),
                    decodeTags(stringValue(stored, key(id, TAGS), "")),
                    longValue(stored, key(id, CREATED_AT), 0L),
                    longValue(stored, key(id, REDEEMED_AT), 0L)
            ));
        }
        vouchers.sort(Comparator.comparingLong((RewardVoucher voucher) -> voucher.createdAtMillis)
                .reversed());
        return Collections.unmodifiableList(vouchers);
    }

    /** Marks a voucher used once; stale, missing, or previously redeemed IDs are rejected. */
    public synchronized boolean redeem(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        String cleanId = id.trim();
        String redeemedKey = key(cleanId, REDEEMED_AT);
        Map<String, ?> stored = preferences.getAll();
        if (!(stored.get(key(cleanId, TITLE)) instanceof String)) {
            return false;
        }
        Object redeemedValue = stored.get(redeemedKey);
        if (redeemedValue != null
                && (!(redeemedValue instanceof Long) || (Long) redeemedValue > 0L)) return false;
        return preferences.edit().putLong(redeemedKey, System.currentTimeMillis()).commit();
    }

    private static String stringValue(Map<String, ?> values, String key, String fallback) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : fallback;
    }

    private static int intValue(Map<String, ?> values, String key, int fallback) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : fallback;
    }

    private static long longValue(Map<String, ?> values, String key, long fallback) {
        Object value = values.get(key);
        return value instanceof Long ? (Long) value : fallback;
    }

    private static String key(String id, String field) {
        return KEY_PREFIX + id + field;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String clean = value.trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    private static String encodeTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        StringBuilder encoded = new StringBuilder();
        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) continue;
            if (encoded.length() > 0) encoded.append(',');
            encoded.append(tag.trim());
        }
        return encoded.toString();
    }

    private static List<String> decodeTags(String encoded) {
        if (encoded == null || encoded.isEmpty()) return Collections.emptyList();
        return Arrays.asList(encoded.split(",", -1));
    }
}
