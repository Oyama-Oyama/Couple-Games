package com.velvet.couplesgame.core.letter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Validates the sealed_letter.json asset across all 8 languages: identical id
 * sets, non-empty copy, and a sane delay ladder.
 */
public class SealedLetterContentTest {

    private static final String[] LANGS = {"en", "zh", "zh_tw", "ja", "ko", "es", "fr", "pt"};
    private static final File ASSET_ROOT = new File("src/main/assets/content");

    private JsonObject load(String lang) throws Exception {
        File file = new File(ASSET_ROOT, lang + "/sealed_letter.json");
        assertTrue("missing asset: " + file.getPath(), file.exists());
        try (FileInputStream in = new FileInputStream(file)) {
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private Set<String> idsOf(JsonArray array) {
        Set<String> ids = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            ids.add(obj.get("id").getAsString());
        }
        return ids;
    }

    @Test
    public void allLanguagesShareTheSameIdSets() throws Exception {
        JsonObject base = load("en");
        Set<String> baseSeals = idsOf(base.getAsJsonArray("seals"));
        Set<String> basePrompts = idsOf(base.getAsJsonArray("prompts"));
        Set<String> baseDelays = idsOf(base.getAsJsonArray("delays"));

        for (String lang : LANGS) {
            JsonObject dto = load(lang);
            assertEquals("seal ids differ in " + lang,
                    baseSeals, idsOf(dto.getAsJsonArray("seals")));
            assertEquals("prompt ids differ in " + lang,
                    basePrompts, idsOf(dto.getAsJsonArray("prompts")));
            assertEquals("delay ids differ in " + lang,
                    baseDelays, idsOf(dto.getAsJsonArray("delays")));
        }
    }

    @Test
    public void everyLanguageHasUsableCopy() throws Exception {
        for (String lang : LANGS) {
            JsonObject dto = load(lang);
            for (JsonElement element : dto.getAsJsonArray("seals")) {
                String label = element.getAsJsonObject().get("label").getAsString();
                assertFalse("empty seal label in " + lang, label.trim().isEmpty());
                String symbol = element.getAsJsonObject().get("symbol").getAsString();
                assertFalse("empty seal symbol in " + lang, symbol.trim().isEmpty());
            }
            for (JsonElement element : dto.getAsJsonArray("prompts")) {
                String text = element.getAsJsonObject().get("text").getAsString();
                assertFalse("empty prompt in " + lang, text.trim().isEmpty());
            }
            for (JsonElement element : dto.getAsJsonArray("delays")) {
                String label = element.getAsJsonObject().get("label").getAsString();
                assertFalse("empty delay label in " + lang, label.trim().isEmpty());
                int days = element.getAsJsonObject().get("days").getAsInt();
                assertTrue("delay must be at least 1 day in " + lang, days >= 1);
            }
        }
    }

    @Test
    public void delaysAreStrictlyIncreasing() throws Exception {
        JsonObject dto = load("en");
        JsonArray delays = dto.getAsJsonArray("delays");
        int previous = 0;
        List<Integer> values = new ArrayList<>();
        for (JsonElement element : delays) {
            int days = element.getAsJsonObject().get("days").getAsInt();
            assertTrue("delays must increase", days > previous);
            previous = days;
            values.add(days);
        }
        assertTrue("need several delay presets", values.size() >= 4);
    }

    @Test
    public void promptCountIsReasonable() throws Exception {
        JsonObject dto = load("en");
        assertTrue("need writing prompts",
                dto.getAsJsonArray("prompts").size() >= 8);
        assertTrue("need several seal styles",
                dto.getAsJsonArray("seals").size() >= 4);
    }
}
