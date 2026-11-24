package com.velvet.couplesgame.ui.fatecubes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeFaceDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeModifierDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import com.velvet.couplesgame.databinding.FragmentFateCubesBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import java.util.Collections;

/**
 * 6th Game Mode: Fate Cubes (Top Action Block x Bottom Body Spot Block + Bonus Wild Modifier & Timer).
 * Partner 1 taps the Top Cube to lock an Action; Partner 2 taps the Bottom Cube to lock a Body Zone.
 */
public class FateCubesFragment extends Fragment {
    private FragmentFateCubesBinding binding;
    private AppContainer container;
    private final FateCubesEngine engine = new FateCubesEngine();
    private final DareTimerHelper timerHelper = new DareTimerHelper();
    private boolean bundleLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFateCubesBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.btnSwapRoles.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.swapRoles();
            refreshRoleHeaders();
            refreshComboPanel();
        });

        binding.switchWildModifier.setOnCheckedChangeListener((btn, checked) -> {
            HapticHelper.tick(btn);
            engine.setWildModifierEnabled(checked);
            refreshComboPanel();
        });

        // Partner 1 taps TOP Action Cube
        binding.cubeAction.setOnClickListener(v -> spinTopActionCube());

        // Partner 2 taps BOTTOM Body Spot Cube
        binding.cubeSpot.setOnClickListener(v -> spinBottomSpotCube());

        // Quick Spin Both button
        binding.btnSpinBoth.setOnClickListener(v -> {
            if (!bundleLoaded) return;
            spinTopActionCube();
            spinBottomSpotCube();
        });

        // Built-in countdown for combo modifier
        binding.btnComboTimer.setOnClickListener(v -> {
            CubeModifierDto mod = engine.getSelectedModifier();
            int sec = (mod != null && mod.durationSec > 0) ? mod.durationSec : 20;
            binding.tvComboTimer.setVisibility(View.VISIBLE);
            timerHelper.start(sec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvComboTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvComboTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        // Save favorite combo to Room
        binding.btnSaveCombo.setOnClickListener(v -> {
            if (!engine.isComboReady()) return;
            String summary = buildComboText();
            CubeFaceDto a = engine.getSelectedAction();
            CubeFaceDto s = engine.getSelectedSpot();
            int tier = Math.max(a.tier, s.tier);
            TaskCard comboCard = new TaskCard(
                    "cube_" + a.id + "_" + s.id,
                    "fate_cubes",
                    tier,
                    TaskType.DARE,
                    Collections.singletonList("physical_light"),
                    engine.getSelectedModifier() != null ? engine.getSelectedModifier().durationSec : 20,
                    summary,
                    false,
                    true
            );
            container.contentRepository.toggleFavorite(comboCard, saved -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.btnSaveCombo.setText(saved ? R.string.btn_favorited : R.string.btn_favorite);
                    }
                });
            });
        });

        // Complete round & swap giver/receiver roles
        binding.btnNextRound.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            timerHelper.cancel();
            engine.completeRoundAndPrepareNext();
            if (engine.getRoundsCompleted() > 0 && engine.getRoundsCompleted() % 4 == 0) {
                container.contentRepository.recordSessionStat("FATE_CUBES", engine.getRoundsCompleted(), engine.getRoundsCompleted(), 0);
            }
            resetCubesToIdle();
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadFateCubesAsync(bundle -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadBundle(bundle, container.consentFilter, maxTier);
                bundleLoaded = true;
                resetCubesToIdle();
            });
        });
    }

    private void spinTopActionCube() {
        if (!bundleLoaded || binding.cubeAction.isSpinning()) return;
        CubeFaceDto picked = engine.rollAction();
        if (picked == null) {
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        timerHelper.cancel();
        binding.tvComboTimer.setVisibility(View.GONE);
        binding.cubeAction.spinToFace(
                engine.getEligibleActions(),
                picked,
                getString(R.string.cubes_tap_reroll),
                this::refreshComboPanel
        );
    }

    private void spinBottomSpotCube() {
        if (!bundleLoaded || binding.cubeSpot.isSpinning()) return;
        CubeFaceDto picked = engine.rollSpot();
        if (picked == null) {
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        timerHelper.cancel();
        binding.tvComboTimer.setVisibility(View.GONE);
        binding.cubeSpot.spinToFace(
                engine.getEligibleSpots(),
                picked,
                getString(R.string.cubes_tap_reroll),
                this::refreshComboPanel
        );
    }

    private void resetCubesToIdle() {
        refreshRoleHeaders();
        String giver = container.profileManager.getPlayerName(engine.isGiverPlayerA());
        String receiver = container.profileManager.getPlayerName(!engine.isGiverPlayerA());

        binding.cubeAction.showIdle(
                "✦",
                getString(R.string.cubes_action_idle),
                getString(R.string.cubes_tap_by_partner, giver)
        );
        binding.cubeSpot.showIdle(
                "♥",
                getString(R.string.cubes_spot_idle),
                getString(R.string.cubes_tap_by_partner, receiver)
        );
        binding.cardComboResult.setVisibility(View.GONE);
        binding.tvComboTimer.setVisibility(View.GONE);
        binding.btnSaveCombo.setText(R.string.btn_favorite);
    }

    private void refreshRoleHeaders() {
        if (binding == null) return;
        String giver = container.profileManager.getPlayerName(engine.isGiverPlayerA());
        String receiver = container.profileManager.getPlayerName(!engine.isGiverPlayerA());

        binding.tvRoleFlow.setText(getString(R.string.cubes_role_flow, giver, receiver));
        binding.cubeAction.setHeaderText(getString(R.string.cubes_top_header, giver));
        binding.cubeSpot.setHeaderText(getString(R.string.cubes_bottom_header, receiver));
    }

    private void refreshComboPanel() {
        if (binding == null || !isAdded()) return;
        if (!engine.isComboReady() || binding.cubeAction.isSpinning() || binding.cubeSpot.isSpinning()) {
            return;
        }

        binding.cardComboResult.setVisibility(View.VISIBLE);
        binding.tvComboHeadline.setText(buildComboText());

        CubeModifierDto mod = engine.getSelectedModifier();
        if (mod != null) {
            binding.tvComboModifier.setVisibility(View.VISIBLE);
            binding.tvComboModifier.setText(getString(R.string.cubes_modifier_fmt, mod.label, mod.durationSec));
            binding.btnComboTimer.setText(getString(R.string.btn_start_timer, mod.durationSec));
        } else {
            binding.tvComboModifier.setVisibility(View.GONE);
            binding.btnComboTimer.setText(getString(R.string.btn_start_timer, 20));
        }

        binding.btnSaveCombo.setText(R.string.btn_favorite);
        CubeFaceDto a = engine.getSelectedAction();
        CubeFaceDto s = engine.getSelectedSpot();
        if (a != null && s != null) {
            String comboId = "cube_" + a.id + "_" + s.id;
            container.contentRepository.isFavoriteAsync(comboId, isSaved -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    CubeFaceDto curA = engine.getSelectedAction();
                    CubeFaceDto curS = engine.getSelectedSpot();
                    if (binding != null && curA != null && curS != null
                            && ("cube_" + curA.id + "_" + curS.id).equals(comboId)) {
                        binding.btnSaveCombo.setText(isSaved ? R.string.btn_favorited : R.string.btn_favorite);
                    }
                });
            });
        }
        PhysicsAnimator.popIn(binding.cardComboResult);
    }

    private String buildComboText() {
        String giver = container.profileManager.getPlayerName(engine.isGiverPlayerA());
        String receiver = container.profileManager.getPlayerName(!engine.isGiverPlayerA());
        CubeFaceDto action = engine.getSelectedAction();
        CubeFaceDto spot = engine.getSelectedSpot();
        if (action == null || spot == null) return "";
        return getString(R.string.cubes_combo_sentence, giver, action.label, receiver, spot.label);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHelper.cancel();
        binding = null;
    }
}
