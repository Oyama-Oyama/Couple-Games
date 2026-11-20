package com.velvet.couplesgame.core.engine;

import com.velvet.couplesgame.core.i18n.AssetContentLoader;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.TemplateDto;
import com.velvet.couplesgame.core.model.TaskCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure offline procedural prompt synthesizer ("Offline AI").
 * Expands localized slot templates while strictly honoring ConsentFilter and intensity tier.
 */
public class ProceduralTaskGenerator {
    private static final Pattern SLOT_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    private final AssetContentLoader assetLoader;
    private final Random random;
    private String lastGeneratedText = "";

    public ProceduralTaskGenerator(AssetContentLoader assetLoader) {
        this(assetLoader, new Random());
    }

    public ProceduralTaskGenerator(AssetContentLoader assetLoader, Random random) {
        this.assetLoader = assetLoader;
        this.random = random;
    }

    public TaskCard generate(int maxTier, String requiredType, ConsentFilter consentFilter) {
        GeneratorBundleDto bundle = assetLoader.loadGeneratorBundle();
        return generateFromBundle(bundle, maxTier, requiredType, consentFilter);
    }

    public TaskCard generateFromBundle(GeneratorBundleDto bundle,
                                       int maxTier,
                                       String requiredType,
                                       ConsentFilter consentFilter) {
        if (bundle == null || bundle.templates == null || bundle.templates.isEmpty()) {
            return null;
        }

        List<TemplateDto> eligible = new ArrayList<>();
        for (TemplateDto tpl : bundle.templates) {
            if (tpl.tier > maxTier) continue;
            if (requiredType != null && !requiredType.equals(tpl.type)) continue;
            if (consentFilter != null && !consentFilter.isAllowed(tpl.tags)) continue;
            eligible.add(tpl);
        }

        if (eligible.isEmpty()) return null;

        // Try up to 4 times to avoid repeating the exact previous prompt text
        for (int attempt = 0; attempt < 4; attempt++) {
            TemplateDto chosen = eligible.get(random.nextInt(eligible.size()));
            RenderResult rendered = renderPattern(chosen.pattern, bundle.slots, chosen.durationSec);
            if (!rendered.text.equals(lastGeneratedText) || attempt == 3) {
                lastGeneratedText = rendered.text;
                String id = "proc_" + chosen.id + "_" + UUID.randomUUID().toString().substring(0, 8);
                return new TaskCard(
                        id,
                        "procedural_ai",
                        chosen.tier,
                        chosen.type,
                        chosen.tags,
                        rendered.durationSec,
                        rendered.text,
                        true,
                        false
                );
            }
        }
        return null;
    }

    private RenderResult renderPattern(String pattern, Map<String, List<String>> slots, int fallbackDuration) {
        if (pattern == null) return new RenderResult("", fallbackDuration);
        Matcher matcher = SLOT_PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        int resolvedDuration = fallbackDuration;

        while (matcher.find()) {
            String slotKey = matcher.group(1);
            String replacement = slotKey;
            if (slots != null && slots.containsKey(slotKey)) {
                List<String> options = slots.get(slotKey);
                if (options != null && !options.isEmpty()) {
                    replacement = options.get(random.nextInt(options.size()));
                    if ("duration".equals(slotKey)) {
                        try {
                            resolvedDuration = Integer.parseInt(replacement.trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return new RenderResult(sb.toString(), resolvedDuration);
    }

    private static class RenderResult {
        final String text;
        final int durationSec;

        RenderResult(String text, int durationSec) {
            this.text = text;
            this.durationSec = durationSec;
        }
    }
}
