package com.velvet.couplesgame.ui.positions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PositionDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import com.velvet.couplesgame.databinding.FragmentPositionsRouletteBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import java.util.ArrayList;
import java.util.List;

public class PositionsRouletteFragment extends Fragment {
    private FragmentPositionsRouletteBinding binding;
    private AppContainer container;
    private final PositionsEngine engine = new PositionsEngine();
    private final DareTimerHelper timerHelper = new DareTimerHelper();
    private ValueAnimator spinAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPositionsRouletteBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.btnDiffAll.setOnClickListener(v -> pickDifficulty(0));
        binding.btnDiff1.setOnClickListener(v -> pickDifficulty(1));
        binding.btnDiff2.setOnClickListener(v -> pickDifficulty(2));
        binding.btnDiff3.setOnClickListener(v -> pickDifficulty(3));

        binding.btnSpinPosition.setOnClickListener(v -> spinAnimated());

        binding.btnPosTried.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            boolean tried = engine.toggleTriedCurrent();
            binding.btnPosTried.setText(tried ? R.string.pos_btn_tried_done : R.string.pos_btn_mark_tried);
            updateProgressHud();
            if (tried) {
                container.contentRepository.recordSessionStat("POSITIONS_ROULETTE", engine.getTriedCount(), engine.getTriedCount(), 0);
            }
        });

        binding.btnPosFavorite.setOnClickListener(v -> {
            PositionDto p = engine.getCurrentPosition();
            if (p == null) return;
            TaskCard tc = new TaskCard(p.id, "positions", 4, TaskType.CONNECTION, p.tags, p.durationSec,
                    p.name + ": " + p.setup + " — " + p.tip, false, false);
            container.contentRepository.toggleFavorite(tc, saved -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.btnPosFavorite.setText(saved ? R.string.btn_favorited : R.string.btn_favorite);
                    }
                });
            });
        });

        binding.btnPosTimer.setOnClickListener(v -> {
            PositionDto p = engine.getCurrentPosition();
            int sec = (p != null && p.durationSec > 0) ? p.durationSec : 60;
            binding.tvPosTimer.setVisibility(View.VISIBLE);
            timerHelper.start(sec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvPosTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvPosTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadExtremeModesAsync(bundle -> {
            List<PositionDto> combined = new ArrayList<>();
            if (container.ageGateManager.isAdultContentAllowed() && bundle.positions != null) {
                combined.addAll(bundle.positions);
            }
            combined.addAll(container.modeSynthesizer.synthesizePositions(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 60
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadPositions(combined, container.consentFilter, maxTier);
                updateDiffButtonsUi();
                updateProgressHud();
                PositionDto first = engine.spinRandom();
                if (first != null) {
                    bindPosition(first);
                } else {
                    binding.tvPosName.setText(R.string.msg_no_eligible_cards);
                    binding.tvPosSetup.setText("");
                    binding.tvPosTip.setVisibility(View.GONE);
                    binding.tvPosSoftLimitWarning.setVisibility(View.GONE);
                    binding.checkPosSoftLimitAck.setVisibility(View.GONE);
                    binding.btnPosTimer.setVisibility(View.GONE);
                    binding.btnPosTried.setEnabled(false);
                }
            });
        });
    }

    private void pickDifficulty(int diff) {
        HapticHelper.tick(binding.getRoot());
        engine.setDifficultyFilter(diff);
        updateDiffButtonsUi();
        spinAnimated();
    }

    private void updateDiffButtonsUi() {
        int d = engine.getDifficultyFilter();
        binding.btnDiffAll.setAlpha(d == 0 ? 1.0f : 0.52f);
        binding.btnDiff1.setAlpha(d == 1 ? 1.0f : 0.52f);
        binding.btnDiff2.setAlpha(d == 2 ? 1.0f : 0.52f);
        binding.btnDiff3.setAlpha(d == 3 ? 1.0f : 0.52f);
    }

    private void spinAnimated() {
        timerHelper.cancel();
        if (spinAnimator != null) spinAnimator.cancel();

        List<PositionDto> pool = engine.getFilteredPool();
        PositionDto target = engine.spinRandom();
        if (target == null) {
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSpinPosition.setEnabled(false);
        final int[] lastStep = {-1};
        spinAnimator = ValueAnimator.ofInt(0, 10);
        spinAnimator.setDuration(600L);
        spinAnimator.setInterpolator(new DecelerateInterpolator(1.6f));
        spinAnimator.addUpdateListener(anim -> {
            if (binding == null) return;
            int step = (int) anim.getAnimatedValue();
            if (step != lastStep[0]) {
                lastStep[0] = step;
                PositionDto preview = (step >= 10 || pool.isEmpty()) ? target : pool.get(step % pool.size());
                binding.tvPosName.setText(preview.name);
                HapticHelper.tick(binding.cardPosition);
            }
        });
        final boolean[] canceled = {false};
        spinAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                canceled[0] = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (canceled[0] || binding == null || !isAdded()) return;
                binding.btnSpinPosition.setEnabled(true);
                HapticHelper.confirm(binding.cardPosition);
                bindPosition(target);
                PhysicsAnimator.popIn(binding.cardPosition);
            }
        });
        spinAnimator.start();
    }

    private void bindPosition(PositionDto p) {
        StringBuilder flames = new StringBuilder();
        for (int i = 0; i < p.heat; i++) flames.append("🔥");
        String diffLabel = p.difficulty == 1
                ? getString(R.string.pos_diff_1)
                : (p.difficulty == 2 ? getString(R.string.pos_diff_2) : getString(R.string.pos_diff_3));

        binding.tvPosMeta.setText(flames + "  ·  " + diffLabel);
        binding.tvPosName.setText(p.name);
        binding.tvPosSetup.setText(p.setup);
        binding.tvPosTip.setVisibility(View.VISIBLE);
        binding.tvPosTip.setText(getString(R.string.pos_tip_fmt, p.tip));
        binding.tvPosTimer.setVisibility(View.GONE);
        binding.btnPosTimer.setVisibility(View.VISIBLE);
        binding.btnPosTimer.setText(getString(R.string.btn_start_timer, p.durationSec > 0 ? p.durationSec : 60));
        binding.btnPosTried.setEnabled(true);
        SoftLimitPromptHelper.bind(
                container.consentFilter,
                p.tags,
                binding.tvPosSoftLimitWarning,
                binding.checkPosSoftLimitAck,
                binding.btnPosTimer,
                binding.btnPosTried
        );
        binding.btnPosTried.setText(engine.isCurrentTried() ? R.string.pos_btn_tried_done : R.string.pos_btn_mark_tried);
        binding.btnPosFavorite.setText(R.string.btn_favorite);
        container.contentRepository.isFavoriteAsync(p.id, isSaved -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                PositionDto cur = engine.getCurrentPosition();
                if (binding != null && cur != null && p.id.equals(cur.id)) {
                    binding.btnPosFavorite.setText(isSaved ? R.string.btn_favorited : R.string.btn_favorite);
                }
            });
        });
    }

    private void updateProgressHud() {
        if (binding == null || !isAdded()) return;
        binding.tvPosProgress.setText(getString(
                R.string.pos_progress_fmt,
                engine.getTriedCount(),
                engine.getTotalEligibleCount()
        ));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (spinAnimator != null) spinAnimator.cancel();
        timerHelper.cancel();
        binding = null;
    }
}
