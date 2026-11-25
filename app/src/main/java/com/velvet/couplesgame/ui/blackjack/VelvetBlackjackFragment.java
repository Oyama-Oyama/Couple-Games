package com.velvet.couplesgame.ui.blackjack;

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
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BlackjackStakeDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import java.util.ArrayList;
import java.util.Collections;
import com.velvet.couplesgame.databinding.FragmentVelvetBlackjackBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.List;

public class VelvetBlackjackFragment extends Fragment {
    private FragmentVelvetBlackjackBinding binding;
    private AppContainer container;
    private final BlackjackEngine engine = new BlackjackEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private int handsPlayed = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVelvetBlackjackBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.btnBjHit.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.hitCurrentPlayer();
            renderState();
        });

        binding.btnBjStand.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.standCurrentPlayer();
            renderState();
        });

        binding.btnBjDouble.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            engine.doubleDownCurrentPlayer();
            renderState();
        });

        binding.btnBjClaimDare.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            boolean loserIsA = (engine.getLastOutcome() == BlackjackEngine.Outcome.PLAYER_B_WINS);
            launchLoserPunishment(loserIsA);
        });

        binding.btnBjNextHand.setOnClickListener(v -> {
            HapticHelper.tick(v);
            if (engine.isMatchOver()) {
                container.contentRepository.recordSessionStat("VELVET_BLACKJACK", engine.getChipsA(), engine.getChipsB(), 0);
                handsPlayed = 0;
                engine.resetMatch();
            } else {
                handsPlayed++;
                engine.startNewHand();
            }
            renderState();
            PhysicsAnimator.popIn(binding.cardBjTable);
        });

        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadIntimateModesAsync(bundle -> {
            List<BlackjackStakeDto> combined = new ArrayList<>();
            if (bundle.blackjackStakes != null) combined.addAll(bundle.blackjackStakes);
            combined.addAll(container.modeSynthesizer.synthesizeBlackjackStakes(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 120
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadStakes(combined, container.consentFilter, maxTier);
                engine.resetMatch();
                renderState();
            });
        });
    }

    private void renderState() {
        if (binding == null) return;
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);

        binding.tvBjChipsA.setText(getString(R.string.bj_chips_fmt, nameA, engine.getChipsA()));
        binding.tvBjChipsB.setText(getString(R.string.bj_chips_fmt, nameB, engine.getChipsB()));

        BlackjackStakeDto stake = engine.getActiveStake();
        String stakeText = stake != null ? stake.label : getString(R.string.bj_default_stake);
        String doubleTag = engine.getCurrentBet() > 1 ? " (×2!)" : "";
        binding.tvBjStakeBanner.setText(getString(R.string.bj_stake_banner, stakeText + doubleTag));

        BlackjackEngine.Phase phase = engine.getPhase();
        boolean revealA = (phase == BlackjackEngine.Phase.TURN_A || phase == BlackjackEngine.Phase.SHOWDOWN);
        boolean revealB = (phase == BlackjackEngine.Phase.TURN_B || phase == BlackjackEngine.Phase.SHOWDOWN);

        binding.tvBjHandALabel.setText(revealA
                ? getString(R.string.bj_hand_score_fmt, nameA, engine.getScoreA())
                : getString(R.string.bj_hand_hidden_fmt, nameA));
        binding.tvBjHandACards.setText(formatHand(engine.getHandA(), revealA));

        binding.tvBjHandBLabel.setText(revealB
                ? getString(R.string.bj_hand_score_fmt, nameB, engine.getScoreB())
                : getString(R.string.bj_hand_hidden_fmt, nameB));
        binding.tvBjHandBCards.setText(formatHand(engine.getHandB(), revealB));

        boolean showdown = (phase == BlackjackEngine.Phase.SHOWDOWN);
        binding.layoutBjControls.setVisibility(showdown ? View.GONE : View.VISIBLE);
        binding.layoutBjShowdown.setVisibility(showdown ? View.VISIBLE : View.GONE);

        if (!showdown) {
            String active = (phase == BlackjackEngine.Phase.TURN_A) ? nameA : nameB;
            binding.tvBjStatus.setText(getString(R.string.bj_turn_prompt, active));
            boolean allowDouble = engine.canDoubleDown();
            binding.btnBjDouble.setEnabled(allowDouble);
            binding.btnBjDouble.setAlpha(allowDouble ? 1.0f : 0.45f);
        } else {
            HapticHelper.confirm(binding.cardBjTable);
            BlackjackEngine.Outcome out = engine.getLastOutcome();
            if (out == BlackjackEngine.Outcome.PLAYER_A_WINS) {
                binding.tvBjStatus.setText(getString(R.string.bj_win_status, nameA, nameB, engine.getLastTransferredChips()));
                binding.btnBjClaimDare.setVisibility(View.VISIBLE);
                binding.btnBjClaimDare.setText(getString(R.string.bj_btn_punish_loser, nameB));
            } else if (out == BlackjackEngine.Outcome.PLAYER_B_WINS) {
                binding.tvBjStatus.setText(getString(R.string.bj_win_status, nameB, nameA, engine.getLastTransferredChips()));
                binding.btnBjClaimDare.setVisibility(View.VISIBLE);
                binding.btnBjClaimDare.setText(getString(R.string.bj_btn_punish_loser, nameA));
            } else {
                binding.tvBjStatus.setText(R.string.bj_push_status);
                binding.btnBjClaimDare.setVisibility(View.GONE);
            }
            binding.btnBjNextHand.setText(engine.isMatchOver()
                    ? R.string.bj_btn_new_match
                    : R.string.bj_btn_next_hand);
        }
    }

    private String formatHand(List<BlackjackEngine.PlayingCard> hand, boolean revealed) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append("   ");
            if (revealed || i == 0) {
                sb.append("[").append(hand.get(i).display()).append("]");
            } else {
                sb.append("[🂠]");
            }
        }
        return sb.toString();
    }

    private void launchLoserPunishment(boolean loserIsA) {
        BlackjackStakeDto stake = engine.getActiveStake();
        boolean doubled = engine.getCurrentBet() > 1;
        String badge = doubled
                ? getString(R.string.bj_double_forfeit_badge)
                : getString(R.string.bj_forfeit_badge);
        if (stake != null && stake.label != null && !stake.label.isEmpty()) {
            String stakeText = doubled ? ("2× · " + stake.label) : stake.label;
            TaskCard stakeCard = new TaskCard(
                    stake.id != null ? stake.id : "bj_stake",
                    "velvet_blackjack",
                    Math.min(stake.tier, container.getEffectiveMaxTier()),
                    TaskType.DARE,
                    stake.tags != null ? stake.tags : Collections.singletonList("physical_light"),
                    doubled ? 45 : 25,
                    stakeText,
                    false,
                    false
            );
            TaskPromptDialogHelper.show(
                    this,
                    container,
                    dareTimer,
                    loserIsA,
                    stakeCard,
                    badge,
                    getString(R.string.btn_wheel_pass_turn),
                    new TaskPromptDialogHelper.Callback() {
                        @Override
                        public void onApproved() {}

                        @Override
                        public void onSecondaryAction() {}
                    }
            );
            return;
        }
        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard dare = container.taskEngine.drawNext(allCards, mutualMatches, maxTier, null, true);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (dare == null) {
                    android.widget.Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                TaskPromptDialogHelper.show(
                        this,
                        container,
                        dareTimer,
                        loserIsA,
                        dare,
                        badge,
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
        if (container != null && handsPlayed > 0) {
            container.contentRepository.recordSessionStat("VELVET_BLACKJACK", engine.getChipsA(), engine.getChipsB(), 0);
        }
        binding = null;
    }
}
