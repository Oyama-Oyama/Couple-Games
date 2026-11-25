package com.velvet.couplesgame.ui.props;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.button.MaterialButton;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PropChallengeDto;
import com.velvet.couplesgame.databinding.FragmentPropLabBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import org.json.JSONArray;
import org.json.JSONException;

public class PropLabFragment extends Fragment {
    private static final String PREFS_PROPS = "prop_lab_local";
    private static final String KEY_CUSTOM_PROPS = "custom_props";
    private static final int MAX_CUSTOM_PROPS = 8;

    private FragmentPropLabBinding binding;
    private AppContainer container;
    private final PropLabEngine engine = new PropLabEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private boolean actorIsPlayerA = true;
    private final List<String> customProps = new ArrayList<>();
    private final List<PropChallengeDto> baseChallenges = new ArrayList<>();
    private int effectiveMaxTier = 4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPropLabBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);
        loadCustomProps();

        bindPropToggle(binding.btnPropIce, "ice");
        bindPropToggle(binding.btnPropBlindfold, "blindfold");
        bindPropToggle(binding.btnPropOil, "oil");
        bindPropToggle(binding.btnPropTie, "tie");
        bindPropToggle(binding.btnPropHoney, "honey");
        bindPropToggle(binding.btnPropMirror, "mirror");
        bindPropToggle(binding.btnPropFeather, "feather");
        binding.btnAddPropCustom.setOnClickListener(v -> addCustomProp());
        refreshCustomPropChips();
        refreshPropChipStates();

        binding.btnPropTimer.setOnClickListener(v -> {
            PropChallengeDto c = engine.getCurrentChallenge();
            if (c == null || c.durationSec <= 0) return;
            binding.tvPropTimer.setVisibility(View.VISIBLE);
            dareTimer.start(c.durationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvPropTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvPropTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnPropDraw.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            dareTimer.cancel();
            if (engine.getCurrentChallenge() != null) {
                engine.markCompleted();
                if (engine.getCompletedCount() % 3 == 0) {
                    container.contentRepository.recordSessionStat("PROP_LAB", engine.getCompletedCount(), engine.getCompletedCount(), 0);
                }
            }
            actorIsPlayerA = !actorIsPlayerA;
            drawNext();
        });

        effectiveMaxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadExtremeModesAsync(bundle -> {
            baseChallenges.clear();
            if (bundle.propChallenges != null) baseChallenges.addAll(bundle.propChallenges);
            baseChallenges.addAll(container.modeSynthesizer.synthesizePropChallenges(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, effectiveMaxTier, 48
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                reloadChallengePool();
                drawNext();
            });
        });
    }

    private void bindPropToggle(MaterialButton btn, String key) {
        btn.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.toggleProp(key);
            refreshPropChipStates();
        });
    }

    private void refreshPropChipStates() {
        styleChip(binding.btnPropIce, "ice");
        styleChip(binding.btnPropBlindfold, "blindfold");
        styleChip(binding.btnPropOil, "oil");
        styleChip(binding.btnPropTie, "tie");
        styleChip(binding.btnPropHoney, "honey");
        styleChip(binding.btnPropMirror, "mirror");
        styleChip(binding.btnPropFeather, "feather");
    }

    private void addCustomProp() {
        String text = binding.etPropCustom.getText() != null
                ? binding.etPropCustom.getText().toString().trim() : "";
        if (text.isEmpty()) {
            binding.inputPropCustom.setError(getString(R.string.props_custom_required));
            return;
        }
        binding.inputPropCustom.setError(null);
        if (customProps.size() >= MAX_CUSTOM_PROPS) {
            Toast.makeText(requireContext(), R.string.props_custom_max, Toast.LENGTH_SHORT).show();
            return;
        }
        if (customProps.stream().anyMatch(prop -> prop.equalsIgnoreCase(text))) {
            binding.inputPropCustom.setError(getString(R.string.props_custom_duplicate));
            return;
        }
        customProps.add(text);
        persistCustomProps();
        engine.toggleProp(customPropKey(text));
        refreshCustomPropChips();
        reloadChallengePool();
        binding.etPropCustom.setText("");
        HapticHelper.confirm(binding.btnAddPropCustom);
    }

    private void loadCustomProps() {
        customProps.clear();
        String saved = requireContext().getSharedPreferences(PREFS_PROPS, 0)
                .getString(KEY_CUSTOM_PROPS, null);
        if (saved == null) return;
        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length() && customProps.size() < MAX_CUSTOM_PROPS; i++) {
                String prop = array.optString(i, "").trim();
                if (!prop.isEmpty() && customProps.stream().noneMatch(p -> p.equalsIgnoreCase(prop))) {
                    customProps.add(prop);
                }
            }
        } catch (JSONException exception) {
            requireContext().getSharedPreferences(PREFS_PROPS, 0).edit()
                    .remove(KEY_CUSTOM_PROPS).apply();
            Toast.makeText(requireContext(), R.string.props_custom_restore_hint, Toast.LENGTH_LONG).show();
        }
    }

    private void persistCustomProps() {
        JSONArray array = new JSONArray();
        for (String prop : customProps) array.put(prop);
        requireContext().getSharedPreferences(PREFS_PROPS, 0).edit()
                .putString(KEY_CUSTOM_PROPS, array.toString()).apply();
    }

    private void refreshCustomPropChips() {
        if (binding == null) return;
        binding.groupPropCustom.removeAllViews();
        for (String prop : customProps) {
            String key = customPropKey(prop);
            Chip chip = new Chip(requireContext());
            chip.setText(prop);
            chip.setCheckable(true);
            chip.setChecked(engine.isPropSelected(key));
            chip.setCloseIconVisible(true);
            chip.setOnClickListener(v -> {
                engine.toggleProp(key);
                chip.setChecked(engine.isPropSelected(key));
            });
            chip.setOnCloseIconClickListener(v -> {
                engine.removePropSelection(key);
                customProps.remove(prop);
                persistCustomProps();
                refreshCustomPropChips();
                reloadChallengePool();
            });
            binding.groupPropCustom.addView(chip);
        }
    }

    private String customPropKey(String prop) {
        return "custom:" + prop.toLowerCase(Locale.ROOT);
    }

    private void reloadChallengePool() {
        if (container == null) return;
        List<PropChallengeDto> combined = new ArrayList<>(baseChallenges);
        String[] promptTemplates = getResources().getStringArray(R.array.props_custom_prompt_templates);
        for (int i = 0; i < customProps.size(); i++) {
            String prop = customProps.get(i);
            String propKey = customPropKey(prop);
            String propId = Integer.toHexString(propKey.hashCode());
            for (int promptIndex = 0; promptIndex < promptTemplates.length; promptIndex++) {
                PropChallengeDto challenge = new PropChallengeDto();
                challenge.id = "custom_prop_" + propId + "_" + promptIndex;
                challenge.propKey = propKey;
                challenge.tier = 1;
                challenge.durationSec = 30;
                challenge.tags = java.util.Collections.singletonList("physical_light");
                challenge.text = String.format(Locale.ROOT, promptTemplates[promptIndex], prop);
                combined.add(challenge);
            }
        }
        engine.loadChallenges(combined, container.consentFilter, effectiveMaxTier);
    }

    private void styleChip(MaterialButton btn, String key) {
        boolean on = engine.isPropSelected(key);
        btn.setAlpha(on ? 1.0f : 0.42f);
    }

    private void drawNext() {
        PropChallengeDto c = engine.drawNextChallenge();
        if (c == null) {
            binding.tvPropMeta.setText("");
            binding.tvPropBody.setText(R.string.msg_no_eligible_cards);
            binding.tvPropSoftLimitWarning.setVisibility(View.GONE);
            binding.checkPropSoftLimitAck.setVisibility(View.GONE);
            binding.tvPropTimer.setVisibility(View.GONE);
            binding.btnPropTimer.setVisibility(View.GONE);
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        String actor = container.profileManager.getPlayerName(actorIsPlayerA);
        PhysicsAnimator.flipCard3D(binding.cardPropChallenge, () -> {
            if (binding == null || !isAdded()) return;
            binding.tvPropMeta.setText(actor + "  ·  " + resolvePropLabel(c.propKey));
            binding.tvPropBody.setText(c.text);
            SoftLimitPromptHelper.bind(
                    container.consentFilter,
                    c.tags,
                    binding.tvPropSoftLimitWarning,
                    binding.checkPropSoftLimitAck,
                    binding.btnPropTimer
            );
            binding.tvPropTimer.setVisibility(View.GONE);
            if (c.durationSec > 0) {
                binding.btnPropTimer.setVisibility(View.VISIBLE);
                binding.btnPropTimer.setText(getString(R.string.btn_start_timer, c.durationSec));
            } else {
                binding.btnPropTimer.setVisibility(View.GONE);
            }
        });
    }

    private String resolvePropLabel(String key) {
        if (key != null && key.startsWith("custom:")) return key.substring("custom:".length());
        switch (key) {
            case "ice": return getString(R.string.prop_ice);
            case "blindfold": return getString(R.string.prop_blindfold);
            case "oil": return getString(R.string.prop_oil);
            case "tie": return getString(R.string.prop_tie);
            case "honey": return getString(R.string.prop_honey);
            case "mirror": return getString(R.string.prop_mirror);
            default: return getString(R.string.prop_feather);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        binding = null;
    }
}
