package com.velvet.couplesgame.ui.bodymap;

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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BodyMapZoneDto;
import com.velvet.couplesgame.databinding.FragmentBodyMapBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import java.util.HashSet;
import java.util.Set;

public class BodyMapFragment extends Fragment {
    private static final String REQ_BODYMAP_HANDOFF = "req_bodymap_handoff";

    private FragmentBodyMapBinding binding;
    private AppContainer container;
    private final BodyMapEngine engine = new BodyMapEngine();
    private final DareTimerHelper timerHelper = new DareTimerHelper();
    private int lastDurationSec = 25;
    private static final String HISTORY_BUCKET = "bodymap_actions";
    private final Set<String> recentBodyActions = new HashSet<>();
    private int tapCounter = 0;
    private com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto generatorBundle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBodyMapBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        recentBodyActions.addAll(container.promptHistory.getRecentSet(HISTORY_BUCKET));

        getChildFragmentManager().setFragmentResultListener(REQ_BODYMAP_HANDOFF, getViewLifecycleOwner(), (k, r) -> {
            engine.beginExplore();
            if (binding != null) {
                binding.bodyMapView.setVisibility(View.VISIBLE);
                renderPhaseUi();
            }
        });

        binding.bodyMapView.applyTheme(container.themeManager.getCurrentTheme());
        binding.bodyMapView.bindEngine(engine);
        binding.bodyMapView.setOnZoneTapListener(this::onZoneTapped);

        binding.btnBodymapLockTraps.setOnClickListener(v -> {
            if (!engine.canBeginExplore()) {
                Toast.makeText(requireContext(), R.string.bodymap_pick_at_least_one, Toast.LENGTH_SHORT).show();
                return;
            }
            HapticHelper.confirm(v);
            binding.bodyMapView.setVisibility(View.INVISIBLE);
            String explorer = container.profileManager.getPlayerName(!engine.isReceiverPlayerA());
            PassPhoneDialog.newInstance(REQ_BODYMAP_HANDOFF, explorer)
                    .show(getChildFragmentManager(), "bodymap_handoff");
        });

        binding.btnBodymapTimer.setOnClickListener(v -> {
            binding.tvBodymapTimer.setVisibility(View.VISIBLE);
            timerHelper.start(lastDurationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvBodymapTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvBodymapTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnBodymapSwapRestart.setOnClickListener(v -> {
            HapticHelper.tick(v);
            timerHelper.cancel();
            engine.swapRoles();
            engine.resetRound();
            renderPhaseUi();
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadIntimateModesAsync(bundle -> {
            generatorBundle = container.assetLoader.loadGeneratorBundle();
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadZones(bundle.bodyMapZones, container.consentFilter, maxTier);
                renderPhaseUi();
            });
        });
    }

    private void onZoneTapped(BodyMapZoneDto zone) {
        if (engine.getPhase() == BodyMapEngine.Phase.SET_TRAPS) {
            if (engine.toggleSecretHotSpot(zone.id)) {
                HapticHelper.tick(binding.bodyMapView);
                renderPhaseUi();
            } else {
                Toast.makeText(requireContext(),
                        getString(R.string.bodymap_max_spots_toast, engine.getTargetHotSpotCount()),
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        timerHelper.cancel();
        BodyMapEngine.TapResult res = engine.exploreZone(zone.id);
        if (res == null) return;

        if (res.wasHotSpot) {
            HapticHelper.confirm(binding.bodyMapView);
        } else {
            HapticHelper.tick(binding.bodyMapView);
        }

        lastDurationSec = Math.max(15, zone.durationSec);
        binding.cardBodymapAction.setVisibility(View.VISIBLE);
        binding.tvBodymapTimer.setVisibility(View.GONE);
        binding.btnBodymapTimer.setText(getString(R.string.btn_start_timer, lastDurationSec));

        String header = res.wasHotSpot
                ? getString(R.string.bodymap_hot_hit_title, zone.label)
                : getString(R.string.bodymap_normal_hit_title, zone.label);
        binding.tvBodymapActionTitle.setText(header);
        tapCounter++;
        String fallbackText = res.wasHotSpot ? zone.hotAction : zone.normalAction;
        String actionText = (generatorBundle != null)
                ? container.modeSynthesizer.synthesizeBodyMapAction(
                        generatorBundle,
                        zone.label,
                        res.wasHotSpot,
                        container.consentFilter,
                        container.entitlementManager.getSelectedMaxTier(),
                        fallbackText,
                        recentBodyActions)
                : fallbackText;
        if (actionText != null && !actionText.isEmpty()) {
            recentBodyActions.add(actionText);
            container.promptHistory.recordShown(HISTORY_BUCKET, actionText, 120);
        }
        binding.tvBodymapActionBody.setText(actionText);
        PhysicsAnimator.popIn(binding.cardBodymapAction);

        if (res.allHotSpotsFound) {
            container.contentRepository.recordSessionStat("BODY_MAP", engine.getFoundHotSpotCount(), engine.getFoundHotSpotCount(), 0);
            Toast.makeText(requireContext(), R.string.bodymap_all_found_toast, Toast.LENGTH_LONG).show();
        }
        renderPhaseUi();
    }

    private void renderPhaseUi() {
        if (binding == null || !isAdded()) return;
        binding.bodyMapView.invalidate();
        if (engine.getZones().isEmpty()) {
            binding.tvBodymapBanner.setText(R.string.msg_no_eligible_cards);
            binding.btnBodymapLockTraps.setVisibility(View.GONE);
            binding.cardBodymapAction.setVisibility(View.GONE);
            binding.btnBodymapSwapRestart.setVisibility(View.GONE);
            return;
        }
        String receiver = container.profileManager.getPlayerName(engine.isReceiverPlayerA());
        String explorer = container.profileManager.getPlayerName(!engine.isReceiverPlayerA());

        if (engine.getPhase() == BodyMapEngine.Phase.SET_TRAPS) {
            binding.tvBodymapBanner.setText(getString(
                    R.string.bodymap_setup_banner,
                    receiver,
                    engine.getSelectedHotSpotCount(),
                    engine.getTargetHotSpotCount()
            ));
            binding.btnBodymapLockTraps.setVisibility(View.VISIBLE);
            binding.btnBodymapLockTraps.setText(getString(R.string.bodymap_btn_lock_pass, explorer));
            binding.cardBodymapAction.setVisibility(View.GONE);
            binding.btnBodymapSwapRestart.setVisibility(View.GONE);
        } else {
            binding.tvBodymapBanner.setText(getString(
                    R.string.bodymap_explore_banner,
                    explorer,
                    receiver,
                    engine.getFoundHotSpotCount(),
                    engine.getSelectedHotSpotCount()
            ));
            binding.btnBodymapLockTraps.setVisibility(View.GONE);
            binding.btnBodymapSwapRestart.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHelper.cancel();
        binding = null;
    }
}
