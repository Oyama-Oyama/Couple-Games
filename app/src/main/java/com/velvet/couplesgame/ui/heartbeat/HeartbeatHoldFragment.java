package com.velvet.couplesgame.ui.heartbeat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.SimplePromptDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentHeartbeatHoldBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Dual-Finger Heartbeat Hold & Tease:
 * Partner A holds top pad, Partner B holds bottom pad simultaneously.
 * Pulling away early before the 15s pulse completes triggers a bold forfeit dare!
 */
public class HeartbeatHoldFragment extends Fragment {
    private static final long TARGET_DURATION_MS = HeartbeatEngine.DEFAULT_HOLD_SECONDS * 1000L;
    private static final long TICK_INTERVAL_MS = 60L;

    private FragmentHeartbeatHoldBinding binding;
    private AppContainer container;
    private final HeartbeatEngine engine = new HeartbeatEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean holdingA = false;
    private boolean holdingB = false;
    private boolean sessionRunning = false;
    private long elapsedMs = 0L;
    private long lastBeatHapticMs = 0L;
    private final Runnable delayedNextPrompt = this::presentNextPrompt;

    private final Runnable pulseTicker = new Runnable() {
        @Override
        public void run() {
            if (!sessionRunning || binding == null || !isAdded()) return;
            elapsedMs += TICK_INTERVAL_MS;
            float ratio = Math.min(1f, (float) elapsedMs / TARGET_DURATION_MS);

            // Heartbeat accelerates as progress approaches 100%
            long beatIntervalMs = Math.round(620L - 360L * ratio);
            float phase = (elapsedMs % beatIntervalMs) / (float) beatIntervalMs;
            float beatScale = (float) Math.sin(phase * Math.PI);

            if (elapsedMs - lastBeatHapticMs >= beatIntervalMs) {
                lastBeatHapticMs = elapsedMs;
                HapticHelper.tick(binding.pulseRing);
            }

            binding.pulseRing.setPulseState(ratio, beatScale, true);
            int remaining = Math.max(0, (int) Math.ceil((TARGET_DURATION_MS - elapsedMs) / 1000.0));
            binding.tvPulseStatus.setText(getString(R.string.heartbeat_holding_fmt, remaining));

            if (elapsedMs >= TARGET_DURATION_MS) {
                onSyncCompleted();
            } else {
                handler.postDelayed(this, TICK_INTERVAL_MS);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHeartbeatHoldBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.pulseRing.applyTheme(container.themeManager.getCurrentTheme());

        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);
        binding.padPartnerA.setText(getString(R.string.heartbeat_pad_label, nameA));
        binding.padPartnerB.setText(getString(R.string.heartbeat_pad_label, nameB));

        binding.padPartnerA.setOnTouchListener((v, event) -> handlePadTouch(true, event));
        binding.padPartnerB.setOnTouchListener((v, event) -> handlePadTouch(false, event));

        binding.btnHeartbeatNextPrompt.setOnClickListener(v -> {
            HapticHelper.tick(v);
            handler.removeCallbacks(delayedNextPrompt);
            stopPulseLoop();
            resetHoldStates();
            presentNextPrompt();
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadIntimateModesAsync(bundle -> {
            List<SimplePromptDto> combined = new ArrayList<>();
            if (bundle.heartbeatPrompts != null) combined.addAll(bundle.heartbeatPrompts);
            combined.addAll(container.modeSynthesizer.synthesizeSimplePrompts(
                    container.assetLoader.loadGeneratorBundle(), "heartbeat", container.consentFilter, maxTier, 120
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadPrompts(combined, container.consentFilter, maxTier);
                presentNextPrompt();
                updateScoreHud();
            });
        });
    }

    private boolean handlePadTouch(boolean isPlayerA, MotionEvent event) {
        if (engine.getCurrentPrompt() == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isPlayerA) holdingA = true;
                else holdingB = true;
                updatePadVisuals();
                HapticHelper.tick(binding.getRoot());
                if (holdingA && holdingB && !sessionRunning) {
                    startPulseLoop();
                } else if (!sessionRunning) {
                    String waitingFor = container.profileManager.getPlayerName(!isPlayerA);
                    binding.tvPulseStatus.setText(getString(R.string.heartbeat_waiting_partner, waitingFor));
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean wasRunning = sessionRunning;
                if (isPlayerA) holdingA = false;
                else holdingB = false;
                updatePadVisuals();
                if (wasRunning && elapsedMs < TARGET_DURATION_MS) {
                    onEarlyReleaseForfeit(isPlayerA);
                } else if (!holdingA && !holdingB && !sessionRunning) {
                    binding.tvPulseStatus.setText(R.string.heartbeat_ready_hint);
                }
                return true;

            default:
                return true;
        }
    }

    private void startPulseLoop() {
        sessionRunning = true;
        elapsedMs = 0L;
        lastBeatHapticMs = 0L;
        handler.removeCallbacks(pulseTicker);
        handler.post(pulseTicker);
    }

    private void stopPulseLoop() {
        sessionRunning = false;
        handler.removeCallbacks(pulseTicker);
        if (binding != null) {
            binding.pulseRing.setPulseState(0f, 0f, false);
        }
    }

    private void onSyncCompleted() {
        stopPulseLoop();
        resetHoldStates();
        engine.recordSyncSuccess();
        HapticHelper.countdownFinished(binding.pulseRing);
        binding.pulseRing.setPulseState(1f, 1f, true);
        binding.tvPulseStatus.setText(R.string.heartbeat_sync_success);
        updateScoreHud();
        Toast.makeText(requireContext(), R.string.heartbeat_sync_toast, Toast.LENGTH_SHORT).show();
        if (engine.getSyncedRounds() % 3 == 0) {
            container.contentRepository.recordSessionStat("HEARTBEAT_HOLD", engine.getSyncedRounds(), engine.getSyncedRounds(), 0);
        }
        handler.removeCallbacks(delayedNextPrompt);
        handler.postDelayed(delayedNextPrompt, 1250L);
    }

    private void onEarlyReleaseForfeit(boolean playerABroke) {
        stopPulseLoop();
        resetHoldStates();
        engine.recordForfeit(playerABroke);
        updateScoreHud();
        String breaker = container.profileManager.getPlayerName(playerABroke);
        binding.tvPulseStatus.setText(getString(R.string.heartbeat_forfeit_status, breaker));

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard dare = container.taskEngine.drawNext(allCards, mutualMatches, maxTier, null, true);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (dare == null) {
                    Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                    presentNextPrompt();
                    return;
                }
                TaskPromptDialogHelper.show(
                        this,
                        container,
                        dareTimer,
                        playerABroke,
                        dare,
                        getString(R.string.heartbeat_forfeit_badge),
                        getString(R.string.btn_wheel_pass_turn),
                        new TaskPromptDialogHelper.Callback() {
                            @Override
                            public void onApproved() {
                                presentNextPrompt();
                            }

                            @Override
                            public void onSecondaryAction() {
                                presentNextPrompt();
                            }
                        }
                );
            });
        });
    }

    private void presentNextPrompt() {
        if (binding == null) return;
        SimplePromptDto next = engine.drawNextPrompt();
        if (next == null) {
            binding.tvHeartbeatPrompt.setText(R.string.msg_no_eligible_cards);
            return;
        }
        binding.tvHeartbeatPrompt.setText(next.text);
        binding.tvPulseStatus.setText(R.string.heartbeat_ready_hint);
        PhysicsAnimator.popIn(binding.tvHeartbeatPrompt);
    }

    private void resetHoldStates() {
        holdingA = false;
        holdingB = false;
        updatePadVisuals();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(delayedNextPrompt);
        stopPulseLoop();
        resetHoldStates();
    }

    private void updatePadVisuals() {
        if (binding == null) return;
        binding.padPartnerA.setBackgroundResource(holdingA ? R.drawable.bg_card_velvet_selected : R.drawable.bg_card_velvet);
        binding.padPartnerB.setBackgroundResource(holdingB ? R.drawable.bg_card_velvet_selected : R.drawable.bg_card_velvet);
    }

    private void updateScoreHud() {
        if (binding == null) return;
        binding.tvHeartbeatStats.setText(getString(
                R.string.heartbeat_stats_fmt,
                engine.getSyncedRounds(),
                engine.getForfeitsA() + engine.getForfeitsB()
        ));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(delayedNextPrompt);
        stopPulseLoop();
        dareTimer.cancel();
        binding = null;
    }
}
