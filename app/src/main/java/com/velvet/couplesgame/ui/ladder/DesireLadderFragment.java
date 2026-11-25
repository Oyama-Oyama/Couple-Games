package com.velvet.couplesgame.ui.ladder;

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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.LadderStepDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentDesireLadderBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.AftercareDialogHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.ArrayList;
import java.util.List;

public class DesireLadderFragment extends Fragment {
    private FragmentDesireLadderBinding binding;
    private AppContainer container;
    private final DesireLadderEngine engine = new DesireLadderEngine();
    private final DareTimerHelper timerHelper = new DareTimerHelper();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDesireLadderBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.btnLadderSwap.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.swapRoles();
            renderStep();
        });

        binding.btnLadderTimer.setOnClickListener(v -> {
            LadderStepDto step = engine.getCurrentStep();
            int sec = (step != null && step.durationSec > 0) ? step.durationSec : 35;
            binding.tvLadderTimer.setVisibility(View.VISIBLE);
            timerHelper.start(sec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvLadderTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvLadderTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnLadderBroke.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            timerHelper.cancel();
            boolean answererIsA = !engine.isTeaserPlayerA();
            int maxTier = container.getEffectiveMaxTier();
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
                            timerHelper,
                            answererIsA,
                            dare,
                            getString(R.string.ladder_broke_badge),
                            getString(R.string.btn_wheel_pass_turn),
                            new TaskPromptDialogHelper.Callback() {
                                @Override
                                public void onApproved() {
                                    advanceLadder();
                                }

                                @Override
                                public void onSecondaryAction() {}
                            }
                    );
                });
            });
        });

        binding.btnLadderClimb.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            timerHelper.cancel();
            advanceLadder();
        });

        binding.btnLadderSkip.setOnClickListener(v -> {
            HapticHelper.tick(v);
            timerHelper.cancel();
            advanceLadder();
        });

        int initMaxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadExtremeModesAsync(bundle -> {
            List<LadderStepDto> combined = new ArrayList<>();
            if (bundle.ladderSteps != null) combined.addAll(bundle.ladderSteps);
            for (int arc = 0; arc < 3; arc++) {
                combined.addAll(container.modeSynthesizer.synthesizeLadderSteps(
                        container.assetLoader.loadGeneratorBundle(), container.consentFilter, initMaxTier, 10
                ));
            }
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadSteps(combined, container.consentFilter);
                renderStep();
            });
        });
    }

    private void advanceLadder() {
        boolean finished = engine.climbNext();
        if (finished) {
            container.contentRepository.recordSessionStat("DESIRE_LADDER", engine.getTotalSteps(), engine.getTotalSteps(), 0);
            Toast.makeText(requireContext(), R.string.ladder_summit_toast, Toast.LENGTH_LONG).show();
            int maxTier = container.getEffectiveMaxTier();
            List<LadderStepDto> freshSteps = container.modeSynthesizer.synthesizeLadderSteps(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 10
            );
            if (!freshSteps.isEmpty()) {
                engine.loadSteps(freshSteps, container.consentFilter);
            }
        }
        PhysicsAnimator.flipCard3D(binding.cardLadderStage, () -> {
            renderStep();
            if (finished) AftercareDialogHelper.show(this);
        });
    }

    private void renderStep() {
        if (binding == null || !isAdded()) return;
        LadderStepDto s = engine.getCurrentStep();
        if (s == null) {
            binding.tvLadderProgress.setText(R.string.msg_no_eligible_cards);
            binding.tvLadderTeaseBody.setText(R.string.msg_no_eligible_cards);
            binding.tvLadderTruthBody.setText("");
            binding.btnLadderTimer.setVisibility(View.GONE);
            binding.tvLadderSoftLimitWarning.setVisibility(View.GONE);
            binding.checkLadderSoftLimitAck.setVisibility(View.GONE);
            binding.btnLadderSkip.setVisibility(View.GONE);
            binding.btnLadderTimer.setEnabled(true);
            binding.btnLadderClimb.setEnabled(false);
            binding.btnLadderBroke.setEnabled(false);
            return;
        }
        binding.btnLadderClimb.setEnabled(true);
        binding.btnLadderBroke.setEnabled(true);

        int num = engine.getCurrentIndex() + 1;
        int total = engine.getTotalSteps();
        binding.progressLadder.setMax(total);
        binding.progressLadder.setProgress(num);

        String teaser = container.profileManager.getPlayerName(engine.isTeaserPlayerA());
        String answerer = container.profileManager.getPlayerName(!engine.isTeaserPlayerA());

        binding.tvLadderProgress.setText(getString(R.string.ladder_step_fmt, num, total));
        binding.tvLadderTeaseHeader.setText(getString(R.string.ladder_teaser_header, teaser));
        binding.tvLadderTeaseBody.setText(s.foreplayAction);
        binding.tvLadderTruthHeader.setText(getString(R.string.ladder_answerer_header, answerer));
        binding.tvLadderTruthBody.setText(s.truthQuestion);

        boolean requiresAcknowledgment = SoftLimitPromptHelper.bind(
                container.consentFilter,
                s.tags,
                binding.tvLadderSoftLimitWarning,
                binding.checkLadderSoftLimitAck,
                binding.btnLadderTimer
        );
        binding.btnLadderSkip.setVisibility(requiresAcknowledgment ? View.VISIBLE : View.GONE);
        binding.btnLadderClimb.setEnabled(!requiresAcknowledgment);
        if (requiresAcknowledgment) {
            binding.checkLadderSoftLimitAck.setOnCheckedChangeListener((button, checked) -> {
                binding.btnLadderTimer.setEnabled(checked);
                binding.btnLadderClimb.setEnabled(checked);
            });
        }

        binding.tvLadderTimer.setVisibility(View.GONE);
        binding.btnLadderTimer.setText(getString(R.string.btn_start_timer, s.durationSec > 0 ? s.durationSec : 35));
        binding.btnLadderClimb.setText(engine.isSummit()
                ? R.string.ladder_btn_finish
                : R.string.ladder_btn_climb_next);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHelper.cancel();
        binding = null;
    }
}
