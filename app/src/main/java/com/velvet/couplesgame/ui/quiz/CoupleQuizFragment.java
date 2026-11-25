package com.velvet.couplesgame.ui.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.QuizQuestionDto;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentCoupleQuizBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.List;

public class CoupleQuizFragment extends Fragment {
    private static final String REQ_QUIZ_HANDOFF = "req_couple_quiz_handoff";

    private FragmentCoupleQuizBinding binding;
    private AppContainer container;
    private final CoupleQuizEngine engine = new CoupleQuizEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private List<QuizQuestionDto> loadedQuestions;
    private boolean isFlipping = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCoupleQuizBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        getChildFragmentManager().setFragmentResultListener(REQ_QUIZ_HANDOFF, getViewLifecycleOwner(), (k, r) -> {
            engine.beginGuesserTurn();
            if (binding != null) {
                binding.layoutQuizPlay.setVisibility(View.VISIBLE);
                renderPhase();
            }
        });

        MaterialButton[] optionButtons = new MaterialButton[]{
                binding.btnQuizOpt0, binding.btnQuizOpt1, binding.btnQuizOpt2, binding.btnQuizOpt3
        };
        for (int i = 0; i < optionButtons.length; i++) {
            final int choiceIdx = i;
            optionButtons[i].setOnClickListener(v -> {
                HapticHelper.tick(v);
                handleOptionPicked(choiceIdx);
            });
        }

        binding.btnQuizPenaltyDare.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            triggerMissPenaltyDare();
        });

        binding.btnQuizNextRound.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.swapSubjectRole();
            startFreshQuizRound();
        });

        container.contentRepository.loadCoupleQuizAsync(bundle -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                loadedQuestions = bundle.questions;
                startFreshQuizRound();
            });
        });
    }

    private void startFreshQuizRound() {
        int maxTier = container.getEffectiveMaxTier();
        engine.startRound(loadedQuestions, container.consentFilter, maxTier);
        renderPhase();
    }

    private void handleOptionPicked(int optionIdx) {
        if (isFlipping) return;
        CoupleQuizEngine.Phase next = engine.recordSelection(optionIdx);
        if (next == CoupleQuizEngine.Phase.HANDOFF_TO_GUESSER) {
            binding.layoutQuizPlay.setVisibility(View.INVISIBLE);
            String guesser = container.profileManager.getPlayerName(!engine.isSubjectPlayerA());
            PassPhoneDialog.newInstance(REQ_QUIZ_HANDOFF, guesser)
                    .show(getChildFragmentManager(), "quiz_handoff");
        } else if (next == CoupleQuizEngine.Phase.RESULTS) {
            HapticHelper.confirm(binding.getRoot());
            container.contentRepository.recordSessionStat(
                    "COUPLE_QUIZ",
                    engine.getCorrectCount(),
                    engine.getTotalQuestions(),
                    0
            );
            renderPhase();
        } else {
            isFlipping = true;
            PhysicsAnimator.flipCard3D(binding.cardQuizQuestion, () -> {
                isFlipping = false;
                renderPhase();
            });
        }
    }

    private void renderPhase() {
        if (binding == null) return;
        CoupleQuizEngine.Phase phase = engine.getPhase();
        String subject = container.profileManager.getPlayerName(engine.isSubjectPlayerA());
        String guesser = container.profileManager.getPlayerName(!engine.isSubjectPlayerA());

        if (phase == CoupleQuizEngine.Phase.EMPTY) {
            binding.layoutQuizPlay.setVisibility(View.GONE);
            binding.layoutQuizResult.setVisibility(View.VISIBLE);
            binding.tvQuizChemistryPct.setText("—");
            binding.tvQuizResultSummary.setText(R.string.msg_no_eligible_cards);
            binding.tvQuizBreakdown.setText("");
            binding.btnQuizPenaltyDare.setVisibility(View.GONE);
            return;
        }

        if (phase == CoupleQuizEngine.Phase.RESULTS) {
            binding.layoutQuizPlay.setVisibility(View.GONE);
            binding.layoutQuizResult.setVisibility(View.VISIBLE);

            int pct = engine.getChemistryPercent();
            int hits = engine.getCorrectCount();
            int total = engine.getTotalQuestions();
            int misses = engine.getMissCount();

            binding.tvQuizChemistryPct.setText(pct + "%");
            binding.tvQuizResultSummary.setText(getString(R.string.quiz_result_summary, guesser, hits, total, subject));

            StringBuilder sb = new StringBuilder();
            for (CoupleQuizEngine.QuizComparison c : engine.buildComparisons(subject)) {
                sb.append(c.matched ? "✓ " : "✕ ").append(c.questionText).append("\n")
                  .append("   ").append(subject).append(": ").append(c.subjectAnswer);
                if (!c.matched) {
                    sb.append("\n   ").append(guesser).append(": ").append(c.guesserPick);
                }
                sb.append("\n\n");
            }
            binding.tvQuizBreakdown.setText(sb.toString().trim());
            binding.btnQuizPenaltyDare.setVisibility(misses > 0 ? View.VISIBLE : View.GONE);
            binding.btnQuizPenaltyDare.setText(getString(R.string.quiz_btn_miss_penalty, guesser, misses));
            PhysicsAnimator.popIn(binding.layoutQuizResult);
            return;
        }

        binding.layoutQuizResult.setVisibility(View.GONE);
        binding.layoutQuizPlay.setVisibility(View.VISIBLE);

        QuizQuestionDto q = engine.getCurrentQuestion();
        if (q == null) return;

        int step = engine.getCurrentIndex() + 1;
        int total = engine.getTotalQuestions();
        binding.progressQuiz.setMax(total);
        binding.progressQuiz.setProgress(step);

        if (phase == CoupleQuizEngine.Phase.SUBJECT_ANSWERING) {
            binding.tvQuizPhaseBanner.setText(getString(R.string.quiz_phase_subject, subject, step, total));
        } else {
            binding.tvQuizPhaseBanner.setText(getString(R.string.quiz_phase_guesser, guesser, subject, step, total));
        }

        binding.tvQuizTier.setText(TagDisplayMapper.resolveTierNameRes(q.tier));
        binding.tvQuizPrompt.setText(q.prompt.replace("{p1}", subject));

        MaterialButton[] btns = new MaterialButton[]{
                binding.btnQuizOpt0, binding.btnQuizOpt1, binding.btnQuizOpt2, binding.btnQuizOpt3
        };
        for (int i = 0; i < btns.length; i++) {
            if (i < q.options.size()) {
                btns[i].setVisibility(View.VISIBLE);
                btns[i].setText(q.options.get(i));
            } else {
                btns[i].setVisibility(View.GONE);
            }
        }
    }

    private void triggerMissPenaltyDare() {
        int maxTier = container.getEffectiveMaxTier();
        boolean guesserIsPlayerA = !engine.isSubjectPlayerA();
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
                        guesserIsPlayerA,
                        dare,
                        getString(R.string.quiz_penalty_badge),
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        binding = null;
    }
}
