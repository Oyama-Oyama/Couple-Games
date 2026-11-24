package com.velvet.couplesgame.ui.wheel;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.fragment.app.Fragment;
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

public class LuckyWheelFragment extends Fragment {
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

        // Seed starter custom wheel choices
        customOptions.clear();
        customOptions.add(getString(R.string.wheel_custom_seed_1));
        customOptions.add(getString(R.string.wheel_custom_seed_2));
        customOptions.add(getString(R.string.wheel_custom_seed_3));
        customOptions.add(getString(R.string.wheel_custom_seed_4));

        binding.btnModeClassic.setOnClickListener(v -> switchMode(WheelMode.CLASSIC));
        binding.btnModeDesire.setOnClickListener(v -> switchMode(WheelMode.DESIRE_MATCH));
        binding.btnModeCustom.setOnClickListener(v -> switchMode(WheelMode.CUSTOM));

        binding.btnAddWheelOption.setOnClickListener(v -> {
            String text = binding.etWheelOption.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            if (customOptions.size() >= 10) {
                Toast.makeText(requireContext(), R.string.wheel_custom_max_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            customOptions.add(text);
            binding.etWheelOption.setText("");
            refreshSectorsForActiveMode();
        });

        View.OnClickListener spinAction = v -> spinWheel();
        binding.btnSpinWheel.setOnClickListener(spinAction);
        binding.wheelView.setOnClickListener(spinAction);

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
        } else if (activeMode == WheelMode.CUSTOM) {
            currentSectors = new ArrayList<>(LuckyWheelEngine.buildFromCustomOptions(customOptions));
            binding.wheelView.setSectors(currentSectors);
            binding.tvWheelStatus.setText(getString(R.string.wheel_custom_count, currentSectors.size()));
        } else {
            container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (mutualMatches == null || mutualMatches.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.wheel_no_desire_fallback, Toast.LENGTH_SHORT).show();
                        activeMode = WheelMode.CLASSIC;
                        updateModeButtonsUi();
                        refreshSectorsForActiveMode();
                    } else {
                        List<TaskCard> shuffledMatches = new ArrayList<>(mutualMatches);
                        Collections.shuffle(shuffledMatches, random);
                        currentSectors = new ArrayList<>(LuckyWheelEngine.buildFromDesireMatches(shuffledMatches));
                        binding.wheelView.setSectors(currentSectors);
                        binding.tvWheelStatus.setText(getString(R.string.wheel_desire_loaded, currentSectors.size()));
                    }
                });
            });
        }
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

        int maxAllowedTier = container.entitlementManager.getSelectedMaxTier();
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
