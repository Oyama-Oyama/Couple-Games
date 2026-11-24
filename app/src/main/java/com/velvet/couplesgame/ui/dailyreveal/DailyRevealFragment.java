package com.velvet.couplesgame.ui.dailyreveal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentDailyRevealBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Daily Two-Player Reveal: draw one deep-talk prompt, let each partner answer privately on the
 * same device (with a pass-the-phone privacy shield), then reveal both answers side by side.
 */
public class DailyRevealFragment extends Fragment {
    private static final String REQ_HANDOFF = "daily_reveal_handoff";

    private FragmentDailyRevealBinding binding;
    private AppContainer container;
    private final List<TaskCard> pool = new ArrayList<>();
    private final Random random = new Random();
    private TaskCard current;
    private String answerA;
    private String answerB;
    private boolean awaitingB;
    /** AC-807 #1 · 揭晓前屏息停顿（450ms），可点击立即跳过。 */
    private Runnable pendingReveal;
    private boolean revealPaused;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDailyRevealBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        getChildFragmentManager().setFragmentResultListener(REQ_HANDOFF, getViewLifecycleOwner(),
                (key, bundle) -> {
                    if (bundle.getBoolean(PassPhoneDialog.RESULT_CONFIRMED, false)) {
                        startPlayerB();
                    }
                });

        binding.btnRevealNext.setOnClickListener(v -> onPrimaryClicked(v));
        binding.btnRevealAgain.setOnClickListener(v -> {
            HapticHelper.tick(v);
            nextPrompt();
        });

        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (allCards != null) {
                    for (TaskCard card : allCards) {
                        if (card != null && card.tags != null && card.tags.contains("deep_talk")) {
                            pool.add(card);
                        }
                    }
                }
                if (pool.isEmpty() && allCards != null) pool.addAll(allCards);
                nextPrompt();
            });
        });
    }

    private void nextPrompt() {
        if (pool.isEmpty()) return;
        current = pool.get(random.nextInt(pool.size()));
        answerA = null;
        answerB = null;
        awaitingB = false;

        binding.layoutRevealResult.setVisibility(View.GONE);
        binding.btnRevealAgain.setVisibility(View.GONE);
        binding.inputRevealAnswer.setVisibility(View.VISIBLE);
        binding.etRevealAnswer.setText("");
        binding.btnRevealNext.setVisibility(View.VISIBLE);

        int tierRes = TagDisplayMapper.resolveTierNameRes(current.tier);
        binding.tvRevealMeta.setText(getString(tierRes) + " · " +
                TagDisplayMapper.formatTagList(requireContext(), current.tags));
        String prompt = substituteNames(current.text);
        binding.tvRevealQuestion.setText(prompt);
        binding.tvRevealStep.setText(R.string.daily_reveal_turn_a);
        PhysicsAnimator.flipCard3D(binding.tvRevealQuestion, null);
    }

    private String substituteNames(String text) {
        if (text == null) return "";
        return text.replace("{p1}", container.profileManager.getPlayerName(true))
                .replace("{p2}", container.profileManager.getPlayerName(false))
                .replace("{partner}", container.profileManager.getPlayerName(false));
    }

    private void onPrimaryClicked(View v) {
        String answer = binding.etRevealAnswer.getText() != null
                ? binding.etRevealAnswer.getText().toString().trim() : "";
        if (answer.isEmpty()) {
            HapticHelper.tick(v);
            binding.inputRevealAnswer.setError(getString(R.string.daily_reveal_answer_required));
            return;
        }
        binding.inputRevealAnswer.setError(null);
        HapticHelper.tick(v);

        if (!awaitingB) {
            answerA = answer;
            String nameB = container.profileManager.getPlayerName(false);
            PassPhoneDialog.newInstance(REQ_HANDOFF, nameB)
                    .show(getChildFragmentManager(), "daily_reveal_handoff");
        } else {
            answerB = answer;
            reveal();
        }
    }

    private void startPlayerB() {
        awaitingB = true;
        binding.tvRevealStep.setText(R.string.daily_reveal_turn_b);
        binding.etRevealAnswer.setText("");
        binding.btnRevealNext.setText(R.string.daily_reveal_reveal);
    }

    /**
     * AC-807 #1 · 双人答案并排显示前插入 450ms「屏息停顿」（D1 延迟满足）。
     * 停顿不强制：点击页面任意处立即揭晓（RED-A3）；复用 View.postDelayed，不新增依赖。
     */
    private void reveal() {
        if (revealPaused || binding == null) return;
        revealPaused = true;
        HapticHelper.tick(binding.getRoot());
        pendingReveal = this::commitReveal;
        binding.getRoot().setOnClickListener(v -> skipRevealPause());
        binding.getRoot().postDelayed(pendingReveal, 450L);
    }

    private void skipRevealPause() {
        if (!revealPaused || binding == null) return;
        binding.getRoot().removeCallbacks(pendingReveal);
        pendingReveal = null;
        commitReveal();
    }

    private void commitReveal() {
        if (binding == null || !isAdded()) return;
        revealPaused = false;
        pendingReveal = null;
        binding.getRoot().setOnClickListener(null);

        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);
        binding.tvRevealAnswerA.setText(nameA + "\n" + answerA);
        binding.tvRevealAnswerB.setText(nameB + "\n" + answerB);

        binding.inputRevealAnswer.setVisibility(View.GONE);
        binding.btnRevealNext.setVisibility(View.GONE);
        binding.layoutRevealResult.setVisibility(View.VISIBLE);
        binding.btnRevealAgain.setVisibility(View.VISIBLE);
        binding.tvRevealStep.setText(R.string.daily_reveal_revealed);
        PhysicsAnimator.flipCard3D(binding.layoutRevealResult, null);
        HapticHelper.confirm(binding.getRoot());

        // Both partners answered -> record one shared session in the Date Night History.
        container.contentRepository.recordSessionStat("DAILY_REVEAL", 1, 1, 0);
    }

    @Override
    public void onDestroyView() {
        if (binding != null && pendingReveal != null) {
            binding.getRoot().removeCallbacks(pendingReveal);
        }
        pendingReveal = null;
        revealPaused = false;
        binding = null;
        super.onDestroyView();
    }
}
