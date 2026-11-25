package com.velvet.couplesgame.ui.wheel;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.fragment.app.Fragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import com.velvet.couplesgame.databinding.FragmentLuckyWheelBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONException;

public class LuckyWheelFragment extends Fragment {
    private static final String PREFS_WHEEL = "lucky_wheel_local";
    private static final String KEY_CUSTOM_OPTIONS = "custom_options";
    private static final int MAX_CUSTOM_OPTIONS = 10;

    private enum WheelMode {
        CLASSIC,
        DESIRE_MATCH,
        CUSTOM
    }

    private FragmentLuckyWheelBinding binding;
    private AppContainer container;
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private final Random random = new Random();

    private WheelMode activeMode = WheelMode.CLASSIC;
    private List<WheelSector> currentSectors = new ArrayList<>();
    private final List<String> customOptions = new ArrayList<>();
    private boolean isPlayerATurn = true;
    private boolean isSpinning = false;
    private int spinsCompleted = 0;
    private int sectorRequestVersion = 0;
    private FlingAnimation activeFling;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLuckyWheelBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        binding.wheelView.applyTheme(container.themeManager.getCurrentTheme());

        loadCustomOptions();

        binding.btnModeClassic.setOnClickListener(v -> switchMode(WheelMode.CLASSIC));
        binding.btnModeDesire.setOnClickListener(v -> switchMode(WheelMode.DESIRE_MATCH));
        binding.btnModeCustom.setOnClickListener(v -> switchMode(WheelMode.CUSTOM));

        binding.btnAddWheelOption.setOnClickListener(v -> {
            String text = binding.etWheelOption.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            if (customOptions.size() >= MAX_CUSTOM_OPTIONS) {
                Toast.makeText(requireContext(), R.string.wheel_custom_max_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            if (containsOption(text)) {
                Toast.makeText(requireContext(), R.string.wheel_custom_duplicate_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            customOptions.add(text);
            saveCustomOptions();
            binding.etWheelOption.setText("");
            refreshSectorsForActiveMode();
        });

        binding.btnSpinWheel.setOnClickListener(v -> spinWheel());

        updateTurnHud();
        updateModeButtonsUi();
        refreshSectorsForActiveMode();
    }

    private void switchMode(WheelMode next) {
        if (isSpinning) return;
        HapticHelper.tick(binding.getRoot());
        activeMode = next;
        binding.layoutCustomInput.setVisibility(next == WheelMode.CUSTOM ? View.VISIBLE : View.GONE);
        updateModeButtonsUi();
        refreshSectorsForActiveMode();
    }

    private void refreshSectorsForActiveMode() {
        if (binding == null) return;
        int requestVersion = ++sectorRequestVersion;
        binding.scrollWheelCustomOptions.setVisibility(
                activeMode == WheelMode.CUSTOM ? View.VISIBLE : View.GONE);
        if (activeMode == WheelMode.CUSTOM) renderCustomOptionChips();
        if (activeMode == WheelMode.CLASSIC) {
            currentSectors = new ArrayList<>(LuckyWheelEngine.buildClassicMixSectors(
                    getString(R.string.wheel_slice_truth),
                    getString(R.string.wheel_slice_playful),
                    getString(R.string.wheel_slice_flirty),
                    getString(R.string.wheel_slice_bold),
                    getString(R.string.wheel_slice_desire),
                    getString(R.string.wheel_slice_ai)
            ));
            binding.wheelView.setSectors(currentSectors);
            binding.tvWheelStatus.setText(R.string.wheel_ready_hint);
            binding.btnSpinWheel.setEnabled(true);
        } else if (activeMode == WheelMode.CUSTOM) {
            currentSectors = new ArrayList<>(LuckyWheelEngine.buildFromCustomOptions(customOptions));
            binding.wheelView.setSectors(currentSectors);
            binding.tvWheelStatus.setText(getString(R.string.wheel_custom_count, currentSectors.size()));
            binding.btnSpinWheel.setEnabled(!currentSectors.isEmpty());
        } else {
            currentSectors = new ArrayList<>();
            binding.wheelView.setSectors(currentSectors);
            binding.btnSpinWheel.setEnabled(false);
            binding.tvWheelStatus.setText(R.string.wheel_desire_loading);
            container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding == null || requestVersion != sectorRequestVersion
                            || activeMode != WheelMode.DESIRE_MATCH) return;
                    if (mutualMatches == null || mutualMatches.isEmpty()) {
                        binding.wheelView.setSectors(currentSectors);
                        binding.tvWheelStatus.setText(R.string.wheel_no_desire_fallback);
                    } else {
                        List<TaskCard> shuffledMatches = new ArrayList<>(mutualMatches);
                        Collections.shuffle(shuffledMatches, random);
                        currentSectors = new ArrayList<>(LuckyWheelEngine.buildFromDesireMatches(shuffledMatches));
                        binding.wheelView.setSectors(currentSectors);
                        binding.tvWheelStatus.setText(getString(R.string.wheel_desire_loaded, currentSectors.size()));
                        binding.btnSpinWheel.setEnabled(!currentSectors.isEmpty());
                    }
                });
            });
        }
    }

    private void loadCustomOptions() {
        customOptions.clear();
        String encoded = requireContext().getSharedPreferences(PREFS_WHEEL, 0)
                .getString(KEY_CUSTOM_OPTIONS, null);
        if (encoded != null) {
            try {
                JSONArray saved = new JSONArray(encoded);
                for (int i = 0; i < saved.length() && customOptions.size() < MAX_CUSTOM_OPTIONS; i++) {
                    String option = saved.optString(i, "").trim();
                    if (!option.isEmpty() && !containsOption(option)) customOptions.add(option);
                }
            } catch (JSONException exception) {
                requireContext().getSharedPreferences(PREFS_WHEEL, 0).edit()
                        .remove(KEY_CUSTOM_OPTIONS).apply();
                Toast.makeText(requireContext(), R.string.wheel_custom_restore_hint, Toast.LENGTH_LONG).show();
            }
        }
        if (customOptions.isEmpty()) {
            customOptions.add(getString(R.string.wheel_custom_seed_1));
            customOptions.add(getString(R.string.wheel_custom_seed_2));
            customOptions.add(getString(R.string.wheel_custom_seed_3));
            customOptions.add(getString(R.string.wheel_custom_seed_4));
            saveCustomOptions();
        }
    }

    private boolean containsOption(String candidate) {
        for (String option : customOptions) {
            if (option.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }

    private void saveCustomOptions() {
        JSONArray saved = new JSONArray();
        for (String option : customOptions) saved.put(option);
        requireContext().getSharedPreferences(PREFS_WHEEL, 0).edit()
                .putString(KEY_CUSTOM_OPTIONS, saved.toString()).apply();
    }

    private void renderCustomOptionChips() {
        binding.groupWheelCustomOptions.removeAllViews();
        for (int i = 0; i < customOptions.size(); i++) {
            final int index = i;
            Chip chip = new Chip(requireContext());
            chip.setText(customOptions.get(i));
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setSingleLine(true);
            chip.setMaxWidth(Math.round(160f * getResources().getDisplayMetrics().density));
            chip.setEllipsize(TruncateAt.END);
            chip.setOnClickListener(v -> showEditCustomOptionDialog(index));
            binding.groupWheelCustomOptions.addView(chip);
        }
    }

    private void showEditCustomOptionDialog(int index) {
        if (index < 0 || index >= customOptions.size() || isSpinning) return;
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        input.setHint(R.string.wheel_custom_hint);
        input.setText(customOptions.get(index));
        input.setSelection(input.length());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.wheel_custom_edit_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String updated = input.getText().toString().trim();
                    if (updated.isEmpty()) return;
                    for (int i = 0; i < customOptions.size(); i++) {
                        if (i != index && customOptions.get(i).equalsIgnoreCase(updated)) {
                            Toast.makeText(requireContext(), R.string.wheel_custom_duplicate_hint,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    customOptions.set(index, updated);
                    saveCustomOptions();
                    refreshSectorsForActiveMode();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void spinWheel() {
        if (isSpinning || currentSectors.isEmpty()) return;
        isSpinning = true;
        binding.btnSpinWheel.setEnabled(false);
        HapticHelper.tick(binding.btnSpinWheel);

        float sectorAngle = 360f / currentSectors.size();
        float startVelocity = 1150f + random.nextFloat() * 900f;

        binding.tvWheelStatus.setText(R.string.wheel_spinning);

        activeFling = PhysicsAnimator.flingWheel(
                binding.wheelView,
                startVelocity,
                sectorAngle,
                this::onWheelStopped
        );
    }

    private void onWheelStopped() {
        if (binding == null || !isAdded()) return;
        isSpinning = false;
        binding.btnSpinWheel.setEnabled(true);

        int idx = LuckyWheelEngine.resolveSelectedSectorIndex(
                binding.wheelView.getRotation(),
                currentSectors.size()
        );
        if (idx < 0 || idx >= currentSectors.size()) return;

        WheelSector landed = currentSectors.get(idx);
        HapticHelper.confirm(binding.wheelView);
        binding.tvWheelStatus.setText(getString(R.string.wheel_landed_status, landed.label));

        final boolean actorA = isPlayerATurn;
        if (landed.customPromptText != null) {
            TaskCard directCard = new TaskCard(
                    landed.id, "lucky_wheel", landed.tier, TaskType.DARE,
                    Collections.singletonList("custom"), 0, landed.customPromptText, false, true
            );
            presentWheelPrompt(actorA, directCard, landed.label);
            return;
        }

        int maxAllowedTier = container.getEffectiveMaxTier();
        int effectiveTier = Math.min(landed.tier, maxAllowedTier);
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard picked = null;
            if (landed.desireWildcard && mutualMatches != null && !mutualMatches.isEmpty()) {
                picked = container.taskEngine.drawNext(mutualMatches, mutualMatches, maxAllowedTier, null, false);
            }
            if (picked == null) {
                boolean forceAi = "s_ai".equals(landed.id);
                if (forceAi) {
                    picked = container.proceduralGenerator.generate(effectiveTier, null, container.consentFilter);
                    if (picked == null) {
                        picked = container.taskEngine.drawNext(allCards, mutualMatches, effectiveTier, null, true);
                    }
                } else {
                    picked = container.taskEngine.drawNext(allCards, mutualMatches, effectiveTier, landed.requiredType, true);
                    if (picked == null && landed.requiredType != null) {
                        picked = container.taskEngine.drawNext(allCards, mutualMatches, effectiveTier, null, true);
                    }
                }
            }

            TaskCard finalPicked = picked;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (finalPicked == null) {
                    Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                } else {
                    presentWheelPrompt(actorA, finalPicked, landed.label);
                }
            });
        });
    }

    private void presentWheelPrompt(boolean actorA, TaskCard card, String badge) {
        TaskPromptDialogHelper.show(
                this,
                container,
                dareTimer,
                actorA,
                card,
                badge,
                getString(R.string.btn_wheel_pass_turn),
                new TaskPromptDialogHelper.Callback() {
                    @Override
                    public void onApproved() {
                        spinsCompleted++;
                        if (spinsCompleted % 5 == 0) {
                            container.contentRepository.recordSessionStat("LUCKY_WHEEL", spinsCompleted, 0, 0);
                        }
                        isPlayerATurn = !isPlayerATurn;
                        updateTurnHud();
                    }

                    @Override
                    public void onSecondaryAction() {
                        isPlayerATurn = !isPlayerATurn;
                        updateTurnHud();
                    }
                }
        );
    }

    private void updateModeButtonsUi() {
        if (binding == null) return;
        binding.btnModeClassic.setAlpha(activeMode == WheelMode.CLASSIC ? 1.0f : 0.52f);
        binding.btnModeDesire.setAlpha(activeMode == WheelMode.DESIRE_MATCH ? 1.0f : 0.52f);
        binding.btnModeCustom.setAlpha(activeMode == WheelMode.CUSTOM ? 1.0f : 0.52f);
    }

    private void updateTurnHud() {
        String label = container.profileManager.getPlayerName(isPlayerATurn);
        binding.tvWheelTurn.setText(getString(R.string.board_turn_indicator, label));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeFling != null) activeFling.cancel();
        dareTimer.cancel();
        binding = null;
    }
}
