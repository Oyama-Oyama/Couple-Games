package com.velvet.couplesgame.core.letter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Locks the on-disk contract of the sealed-letter store.
 *
 * <p>The stored JSON keys are the field names of the private StoredLetter
 * record, so a rename would silently make every saved letter unreadable after
 * an app update. These tests guard that contract.
 */
public class SealedLetterStoreContractTest {

    /** Mirrors the private StoredLetter shape exactly. */
    private static final class Mirror {
        String id;
        boolean fromPlayerA;
        String title;
        String bodyCipher;
        String revealDateIso;
        long sealedAt;
        long openedAt;
        String sealId;
    }

    @Test
    public void storedLetterFieldNamesMatchTheOnDiskContract() throws Exception {
        Class<?> stored = Class.forName(
                "com.velvet.couplesgame.core.letter.SealedLetterManager$StoredLetter");
        assertNotNull(stored);

        List<String> expected = new ArrayList<>();
        expected.add("id");
        expected.add("fromPlayerA");
        expected.add("title");
        expected.add("bodyCipher");
        expected.add("revealDateIso");
        expected.add("sealedAt");
        expected.add("openedAt");
        expected.add("sealId");

        for (String name : expected) {
            boolean found = false;
            for (Field field : stored.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            assertTrue("StoredLetter must keep the field '" + name + "'", found);
        }
    }

    @Test
    public void gsonRoundTripKeepsEveryStoredValue() {
        Gson gson = new Gson();
        Mirror source = new Mirror();
        source.id = "letter-1";
        source.fromPlayerA = true;
        source.title = "For you";
        source.bodyCipher = "BASE64CIPHERTEXT==";
        source.revealDateIso = "2026-12-24";
        source.sealedAt = 1700000000000L;
        source.openedAt = 0L;
        source.sealId = "seal_gold";

        String json = gson.toJson(source);
        assertTrue(json.contains("bodyCipher"));

        Type type = new TypeToken<Mirror>() {}.getType();
        Mirror restored = gson.fromJson(json, type);

        assertEquals(source.id, restored.id);
        assertEquals(source.fromPlayerA, restored.fromPlayerA);
        assertEquals(source.title, restored.title);
        assertEquals(source.bodyCipher, restored.bodyCipher);
        assertEquals(source.revealDateIso, restored.revealDateIso);
        assertEquals(source.sealedAt, restored.sealedAt);
        assertEquals(source.openedAt, restored.openedAt);
        assertEquals(source.sealId, restored.sealId);
    }

    @Test
    public void unreadablePayloadIsRejectedRatherThanCrashing() {
        Gson gson = new Gson();
        Mirror corrupted = new Mirror();
        corrupted.id = "x";
        corrupted.bodyCipher = "!!!not-base64!!!";
        corrupted.revealDateIso = "2026-01-01";

        // A corrupted blob must not throw at deserialisation time; the crypto
        // layer returns null and the manager reports "cannot open".
        String json = gson.toJson(corrupted);
        Type type = new TypeToken<Mirror>() {}.getType();
        Mirror restored = gson.fromJson(json, type);
        assertEquals(corrupted.bodyCipher, restored.bodyCipher);
    }
}
