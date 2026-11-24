package com.velvet.couplesgame.ui.bomb;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.SimplePromptDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentVelvetBombBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.ArrayList;
import java.util.List;

public class VelvetBombFragment extends Fragment {
    private static final long TICK_MS = 80L;

    private FragmentVelvetBombBinding binding;
    private AppContainer container;
    private final VelvetBombEngine engine = new VelvetBombEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long elapsedMs = 0L;
    private long lastTickHapticMs = 0L;

    private final Runnable bombTicker = new Runnable() {
        @Override
        public void run() {
            if (!engine.isArmed() || binding == null || !isAdded()) return;
            elapsedMs += TICK_MS;
            long fuse = Math.max(1L, engine.getFuseDurationMs());
            float tension = Math.min(1f, (float) elapsedMs / fuse);

            // Haptic pulse & ring urgency speed up dramatically near hidden detonation
            long interval = Math.round(780L - 560L * tension);
            float phase = (elapsedMs % interval) / (float) interval;
            float beatScale = (float) Math.sin(phase * Math.PI);
            binding.bombPulseView.setPulseState(tension, beatScale, true);

            if (elapsedMs - lastTickHapticMs >= interval) {
                lastTickHapticMs = elapsedMs;
                HapticHelper.tick(binding.bombPulseView);
            }

            if (elapsedMs >= fuse) {
                detonateNow();
            } else {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVelvetBombBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.bombPulseView.applyTheme(container.themeManager.getCurrentTheme());

        binding.btnArmBomb.setOnClickListener(v -> startBombRound());

        binding.btnPassBomb.setOnClickListener(v -> {
            if (!engine.isArmed()) return;
            HapticHelper.confirm(v);
            SimplePromptDto nextRelay = engine.passBomb();
            if (nextRelay != null) {
                binding.tvBombRelayPrompt.setText(nextRelay.text);
                PhysicsAnimator.popIn(binding.tvBombRelayPrompt);
            }
            PhysicsAnimator.popIn(binding.tvBombHolder);
            updateHud();
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadIntimateModesAsync(bundle -> {
            List<SimplePromptDto> combined = new ArrayList<>();
            if (bundle.bombRelays != null) combined.addAll(bundle.bombRelays);
            combined.addAll(container.modeSynthesizer.synthesizeSimplePrompts(
                    container.assetLoader.loadGeneratorBundle(), "bomb", container.consentFilter, maxTier, 160
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadRelays(combined, container.consentFilter, maxTier);
                updateHud();
            });
        });
    }

    private void startBombRound() {
        SimplePromptDto relay = engine.armNewRound();
        if (relay == null) {
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        elapsedMs = 0L;
        lastTickHapticMs = 0L;
        binding.tvBombRelayPrompt.setText(relay.text);
        binding.btnArmBomb.setVisibility(View.GONE);
        binding.btnPassBomb.setVisibility(View.VISIBLE);
        PhysicsAnimator.popIn(binding.tvBombRelayPrompt);
        updateHud();

        handler.removeCallbacks(bombTicker);
        handler.post(bombTicker);
    }

    private void detonateNow() {
        handler.removeCallbacks(bombTicker);
        boolean loserA = engine.detonate();
        HapticHelper.countdownFinished(binding.getRoot());
        binding.bombPulseView.setPulseState(1f, 1.3f, true);
        binding.btnPassBomb.setVisibility(View.GONE);
        binding.btnArmBomb.setVisibility(View.VISIBLE);

        String victim = container.profileManager.getPlayerName(loserA);
        binding.tvBombHolder.setText(getString(R.string.bomb_exploded_banner, victim));
        updateScoreText();

        container.contentRepository.recordSessionStat("VELVET_BOMB", engine.getExplosionsA(), engine.getExplosionsB(), 0);

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard dare = container.taskEngine.drawNext(allCards, mutualMatches, maxTier, null, true);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (dare == null) {
                    Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                    return;
                }
                TaskPromptDialogHelper.show(
                        this,
                        container,
                        dareTimer,
                        loserA,
                        dare,
                        getString(R.string.bomb_boom_badge),
                        getString(R.string.btn_wheel_pass_turn),
                        new TaskPromptDialogHelper.Callback() {
                            @Override
                            public void onApproved() {}

                            @Override
                            public void onSecondaryAction() {}
                        }
                );
            });
        });
    }

    private void updateHud() {
        if (binding == null) return;
        String holder = container.profileManager.getPlayerName(engine.isHolderPlayerA());
        String next = container.profileManager.getPlayerName(!engine.isHolderPlayerA());
        if (engine.isArmed()) {
            binding.tvBombHolder.setText(getString(R.string.bomb_holder_banner, holder, engine.getPassCount()));
            binding.btnPassBomb.setText(getString(R.string.bomb_btn_pass_to, next));
        } else {
            binding.tvBombHolder.setText(R.string.bomb_idle_banner);
        }
        updateScoreText();
    }

    private void updateScoreText() {
        if (binding == null || !isAdded()) return;
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);
        binding.tvBombStats.setText(getString(
                R.string.bomb_stats_fmt,
                nameA, engine.getExplosionsA(),
                nameB, engine.getExplosionsB()
        ));
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(bombTicker);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (engine.isArmed() && binding != null) {
            handler.removeCallbacks(bombTicker);
            handler.postDelayed(bombTicker, TICK_MS);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(bombTicker);
        dareTimer.cancel();
        binding = null;
    }
}
