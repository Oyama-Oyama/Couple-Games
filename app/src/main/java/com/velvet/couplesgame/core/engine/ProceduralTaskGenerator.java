package com.velvet.couplesgame.core.engine;

import com.velvet.couplesgame.core.i18n.AssetContentLoader;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.TemplateDto;
import com.velvet.couplesgame.core.model.TaskCard;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure offline procedural prompt synthesizer.
 * Uses rolling text deduplication, template cooldowns, per-slot variety memory, and
 * heat-weighted selection while strictly honoring ConsentFilter boundaries.
 */
public class ProceduralTaskGenerator {
    private static final Pattern SLOT_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
    private static final int RECENT_TEXT_WINDOW = 160;
    private static final int MAX_TEMPLATE_COOLDOWN = 10;
    private static final int SLOT_MEMORY_DEPTH = 6;

    private final AssetContentLoader assetLoader;
    private final Random random;
    private final Deque<String> recentTexts = new ArrayDeque<>();
    private final Set<String> recentTextSet = new HashSet<>();
    private final Deque<String> recentTemplateIds = new ArrayDeque<>();
    private static final String HISTORY_BUCKET = "procedural_texts";
    private final Map<String, Deque<String>> recentSlotValues = new HashMap<>();
    private PromptHistoryStore historyStore;

    public ProceduralTaskGenerator(AssetContentLoader assetLoader) {
        this(assetLoader, new Random());
    }

    public ProceduralTaskGenerator(AssetContentLoader assetLoader, Random random) {
        this.assetLoader = assetLoader;
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            for (String hash : store.getRecentList(HISTORY_BUCKET)) {
                if (recentTextSet.add(hash)) {
                    recentTexts.addLast(hash);
                    while (recentTexts.size() > RECENT_TEXT_WINDOW) {
                        String oldest = recentTexts.removeFirst();
                        recentTextSet.remove(oldest);
                    }
                }
            }
        }
    }

    public void resetHistory() {
        recentTexts.clear();
        recentTextSet.clear();
        recentTemplateIds.clear();
        recentSlotValues.clear();
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

        List<TemplateDto> eligibleDistinct = new ArrayList<>();
        for (TemplateDto tpl : bundle.templates) {
            if (tpl.tier > maxTier) continue;
            if (requiredType != null && !requiredType.equals(tpl.type)) continue;
            if (consentFilter != null && !consentFilter.isAllowed(tpl.tags)) continue;
            eligibleDistinct.add(tpl);
        }
        if (eligibleDistinct.isEmpty()) return null;

        // Apply template cooldown only when enough distinct templates exist to preserve heat weighting
        Set<String> cooldownSet = new HashSet<>();
        if (eligibleDistinct.size() >= 4) {
            int allowedCooldown = Math.min(MAX_TEMPLATE_COOLDOWN, eligibleDistinct.size() / 2);
            int skipped = Math.max(0, recentTemplateIds.size() - allowedCooldown);
            int idx = 0;
            for (String tid : recentTemplateIds) {
                if (idx++ >= skipped) {
                    cooldownSet.add(tid);
                }
            }
        }

        List<TemplateDto> weightedPool = buildWeightedPool(eligibleDistinct, maxTier, cooldownSet);
        if (weightedPool.isEmpty()) {
            weightedPool = buildWeightedPool(eligibleDistinct, maxTier, null);
        }
        if (weightedPool.isEmpty()) return null;

        TemplateDto fallbackTpl = null;
        RenderResult fallbackRender = null;

        for (int attempt = 0; attempt < 32; attempt++) {
            TemplateDto chosen = weightedPool.get(random.nextInt(weightedPool.size()));
            RenderResult rendered = renderPattern(chosen.pattern, bundle.slots, chosen.durationSec);
            fallbackTpl = chosen;
            fallbackRender = rendered;

            String textKey = Integer.toHexString(rendered.text.hashCode());
            if (!recentTextSet.contains(rendered.text) && !recentTextSet.contains(textKey)) {
                recordGenerated(chosen.id, rendered.text);
                return buildCard(chosen, rendered);
            }
        }

        if (fallbackTpl != null && fallbackRender != null) {
            recordGenerated(fallbackTpl.id, fallbackRender.text);
            return buildCard(fallbackTpl, fallbackRender);
        }
        return null;
    }

    private List<TemplateDto> buildWeightedPool(List<TemplateDto> eligibleDistinct,
                                                int maxTier,
                                                Set<String> excludedTemplateIds) {
        List<TemplateDto> pool = new ArrayList<>();
        for (TemplateDto tpl : eligibleDistinct) {
            if (excludedTemplateIds != null && excludedTemplateIds.contains(tpl.id)) {
                continue;
            }
            int weight = 1;
            if (tpl.tier == maxTier) weight = 3;
            else if (tpl.tier == maxTier - 1) weight = 2;

            for (int w = 0; w < weight; w++) {
                pool.add(tpl);
            }
        }
        return pool;
    }

    private TaskCard buildCard(TemplateDto chosen, RenderResult rendered) {
        String id = "proc_" + chosen.id + "_" + UUID.randomUUID().toString().substring(0, 8);
        return new TaskCard(
                id,
                "procedural_spark",
                chosen.tier,
                chosen.type,
                chosen.tags,
                rendered.durationSec,
                rendered.text,
                true,
                false
        );
    }

    private void recordGenerated(String templateId, String text) {
        if (text != null) {
            if (recentTextSet.add(text)) {
                recentTexts.addLast(text);
                while (recentTexts.size() > RECENT_TEXT_WINDOW) {
                    String oldest = recentTexts.removeFirst();
                    recentTextSet.remove(oldest);
                }
            }
            if (historyStore != null) {
                historyStore.recordShown(HISTORY_BUCKET, Integer.toHexString(text.hashCode()), 200);
            }
        }
        if (templateId != null) {
            recentTemplateIds.remove(templateId);
            recentTemplateIds.addLast(templateId);
            while (recentTemplateIds.size() > MAX_TEMPLATE_COOLDOWN) {
                recentTemplateIds.removeFirst();
            }
        }
    }

    private RenderResult renderPattern(String pattern, Map<String, List<String>> slots, int fallbackDuration) {
        if (pattern == null) return new RenderResult("", fallbackDuration);
        Matcher matcher = SLOT_PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        int resolvedDuration = fallbackDuration;
        Set<String> usedValuesInPrompt = new HashSet<>();

        while (matcher.find()) {
            String slotKey = matcher.group(1);
            String replacement = slotKey;
            if (slots != null && slots.containsKey(slotKey)) {
                List<String> options = slots.get(slotKey);
                if (options != null && !options.isEmpty()) {
                    replacement = pickFreshSlotValue(slotKey, options, usedValuesInPrompt);
                    usedValuesInPrompt.add(replacement);
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

    private String pickFreshSlotValue(String slotKey, List<String> options, Set<String> usedInPrompt) {
        Deque<String> history = recentSlotValues.get(slotKey);
        List<String> fresh = new ArrayList<>();
        for (String opt : options) {
            if (!usedInPrompt.contains(opt) && (history == null || !history.contains(opt))) {
                fresh.add(opt);
            }
        }
        if (fresh.isEmpty()) {
            for (String opt : options) {
                if (!usedInPrompt.contains(opt)) {
                    fresh.add(opt);
                }
            }
        }
        List<String> source = fresh.isEmpty() ? options : fresh;
        String chosen = source.get(random.nextInt(source.size()));

        if (history == null) {
            history = new ArrayDeque<>();
            recentSlotValues.put(slotKey, history);
        }
        history.remove(chosen);
        history.addLast(chosen);
        int maxDepth = Math.min(SLOT_MEMORY_DEPTH, Math.max(1, options.size() - 1));
        while (history.size() > maxDepth) {
            history.removeFirst();
        }
        return chosen;
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
