package com.velvet.couplesgame.core.letter;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Offline store for sealed love letters.
 *
 * <p>Everything lives in this device's SharedPreferences: the body is kept as
 * an AES-GCM blob produced by {@link LetterCrypto}, and the reveal date gates
 * when it may be decrypted. No network, no account, no sync — the two partners
 * share one phone, so the letter is simply waiting on the same device.
 */
public class SealedLetterManager {

    private static final String PREF_NAME = "velvet_sealed_letters";
    private static final String KEY_LETTERS = "letters";
    private static final int MAX_LETTERS = 60;
    private static final int MAX_TITLE = 40;
    private static final int MAX_BODY = 800;

    /**
     * Serialised shape kept in preferences (body stays encrypted).
     *
     * <p>Field names are the on-disk JSON keys, so they are kept by
     * proguard-rules.pro and must never be renamed.
     */
    private static final class StoredLetter {
        String id;
        boolean fromPlayerA;
        String title;
        String bodyCipher;
        String revealDateIso;
        long sealedAt;
        long openedAt;
        String sealId;
    }

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final MutableLiveData<List<SealedLetter>> lettersLiveData = new MutableLiveData<>();

    public SealedLetterManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        refresh();
    }

    public LiveData<List<SealedLetter>> observeLetters() {
        return lettersLiveData;
    }

    /**
     * Seal a new letter.
     *
     * @param revealDate the day the letter becomes readable
     * @return the stored letter, or null when the body could not be persisted
     */
    @Nullable
    public SealedLetter seal(boolean fromPlayerA,
                              String title,
                              String body,
                              LocalDate revealDate,
                              String sealId) {
        if (revealDate == null) return null;
        String cleanTitle = clean(title, MAX_TITLE);
        String cleanBody = clean(body, MAX_BODY);
        if (cleanBody.isEmpty()) return null;

        String cipher = LetterCrypto.encrypt(cleanBody);
        if (cipher == null) return null;

        StoredLetter stored = new StoredLetter();
        stored.id = UUID.randomUUID().toString();
        stored.fromPlayerA = fromPlayerA;
        stored.title = cleanTitle;
        stored.bodyCipher = cipher;
        stored.revealDateIso = revealDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        stored.sealedAt = System.currentTimeMillis();
        stored.openedAt = 0L;
        stored.sealId = sealId != null ? sealId : "seal_crimson";

        List<StoredLetter> all = readStored();
        all.add(stored);
        trim(all);
        writeStored(all);

        SealedLetter letter = new SealedLetter(stored.id, fromPlayerA, stored.title, "",
                stored.revealDateIso, stored.sealedAt, 0L, stored.sealId);
        refresh();
        return letter;
    }

    /** All letters, newest first. Bodies of unopened letters are blank. */
    public List<SealedLetter> getLetters() {
        List<SealedLetter> result = new ArrayList<>();
        for (StoredLetter stored : readStored()) {
            result.add(toLetter(stored, false));
        }
        Collections.sort(result, (a, b) -> Long.compare(b.sealedAt, a.sealedAt));
        return result;
    }

    /** Letters that are sealed and waiting for their reveal date. */
    public List<SealedLetter> getSealedLetters() {
        List<SealedLetter> result = new ArrayList<>();
        for (SealedLetter letter : getLetters()) {
            if (letter.stateOn(LocalDate.now()) == SealedLetter.State.SEALED) {
                result.add(letter);
            }
        }
        return result;
    }

    /** Letters whose reveal date has arrived but that were never opened. */
    public List<SealedLetter> getReadyLetters() {
        List<SealedLetter> result = new ArrayList<>();
        for (SealedLetter letter : getLetters()) {
            if (letter.stateOn(LocalDate.now()) == SealedLetter.State.READY) {
                result.add(letter);
            }
        }
        return result;
    }

    /** Letters that were already opened. */
    public List<SealedLetter> getOpenedLetters() {
        List<SealedLetter> result = new ArrayList<>();
        for (SealedLetter letter : getLetters()) {
            if (letter.stateOn(LocalDate.now()) == SealedLetter.State.OPENED) {
                result.add(letter);
            }
        }
        return result;
    }

    /**
     * Decrypt a letter and mark it as opened.
     *
     * @return the readable letter, or null when the letter is still sealed or
     *         the ciphertext cannot be decrypted.
     */
    @Nullable
    public SealedLetter open(String letterId) {
        if (letterId == null) return null;
        List<StoredLetter> all = readStored();
        for (int i = 0; i < all.size(); i++) {
            StoredLetter stored = all.get(i);
            if (!letterId.equals(stored.id)) continue;
            SealedLetter view = toLetter(stored, false);
            if (view.stateOn(LocalDate.now()) == SealedLetter.State.SEALED) return null;

            String plain = LetterCrypto.decrypt(stored.bodyCipher);
            if (plain == null) return null;

            long now = System.currentTimeMillis();
            stored.openedAt = now;
            all.set(i, stored);
            writeStored(all);
            refresh();
            return new SealedLetter(stored.id, stored.fromPlayerA, stored.title, plain,
                    stored.revealDateIso, stored.sealedAt, now, stored.sealId);
        }
        return null;
    }

    /** Remove a letter permanently. */
    public void delete(String letterId) {
        if (letterId == null) return;
        List<StoredLetter> all = readStored();
        all.removeIf(stored -> letterId.equals(stored.id));
        writeStored(all);
        refresh();
    }

    /** Delete every sealed letter (used by the settings reset). */
    public void clearAll() {
        prefs.edit().remove(KEY_LETTERS).apply();
        refresh();
    }

    private SealedLetter toLetter(StoredLetter stored, boolean decryptBody) {
        String body = "";
        if (decryptBody && stored.openedAt > 0) {
            String plain = LetterCrypto.decrypt(stored.bodyCipher);
            body = plain != null ? plain : "";
        }
        return new SealedLetter(stored.id, stored.fromPlayerA, stored.title, body,
                stored.revealDateIso, stored.sealedAt, stored.openedAt, stored.sealId);
    }

    private List<StoredLetter> readStored() {
        String raw = prefs.getString(KEY_LETTERS, null);
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<StoredLetter>>() {}.getType();
            List<StoredLetter> parsed = gson.fromJson(raw, type);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void writeStored(List<StoredLetter> all) {
        prefs.edit().putString(KEY_LETTERS, gson.toJson(all)).apply();
    }

    /** Keep the newest {@link #MAX_LETTERS} entries. */
    private void trim(List<StoredLetter> all) {
        while (all.size() > MAX_LETTERS) {
            all.remove(0);
        }
    }

    private void refresh() {
        lettersLiveData.setValue(getLetters());
    }

    private static String clean(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
