package com.velvet.couplesgame.ui.chess;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.databinding.FragmentCoupleBoardBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import com.velvet.couplesgame.ui.paywall.PaywallDialogFragment;

public class CoupleBoardFragment extends Fragment {
    private static final String REQ_PAYWALL_BOARD = "req_paywall_board";

    private FragmentCoupleBoardBinding binding;
    private AppContainer container;
    private CoupleBoardEngine engine;
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private ValueAnimator diceAnimator;
    private boolean isRolling = false;
    private AppTheme currentBoardTheme;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCoupleBoardBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        CoupleBoardViewModel vm = new ViewModelProvider(this).get(CoupleBoardViewModel.class);
        engine = vm.getEngine();
        engine.initDefaultRewardsIfUnmodified(
                getString(R.string.board_default_reward_a),
                getString(R.string.board_default_reward_b)
        );

        getChildFragmentManager().setFragmentResultListener(REQ_PAYWALL_BOARD, getViewLifecycleOwner(), (key, bundle) -> {
            if (!bundle.getBoolean(PaywallDialogFragment.RESULT_UNLOCKED, false)) return;
            String themeId = bundle.getString(PaywallDialogFragment.RESULT_ACTION_TAG);
            if (themeId != null && binding != null) {
                applyBoardThemeNonDestructive(AppTheme.fromId(themeId));
            }
        });

        currentBoardTheme = container.themeManager.getCurrentTheme();
        binding.boardCanvas.setBoardTiles(engine.getTiles());
        binding.boardCanvas.applyTheme(currentBoardTheme);
        binding.boardCanvas.setTokenLabels(
                container.profileManager.getPlayerInitial(true),
                container.profileManager.getPlayerInitial(false)
        );
        binding.boardCanvas.syncPositionsInstant(engine.getPlayerAPos(), engine.getPlayerBPos());

        updateHud();

        // Non-destructive live theme switching: preserves match progress, positions, and custom prizes
        binding.btnCycleTheme.setOnClickListener(v -> {
            HapticHelper.tick(v);
            AppTheme[] all = AppTheme.values();
            AppTheme candidate = all[(currentBoardTheme.ordinal() + 1) % all.length];
            if (!container.entitlementManager.canAccessTheme(candidate)) {
                PaywallDialogFragment.newInstance(REQ_PAYWALL_BOARD, candidate.id)
                        .show(getChildFragmentManager(), "board_theme_paywall");
                return;
            }
            applyBoardThemeNonDestructive(candidate);
        });

        binding.btnEditRewards.setOnClickListener(v -> showRewardsDialog());

        binding.btnRollDice.setOnClickListener(v -> {
            if (isRolling) return;
            executeTurn();
        });
    }

    private void applyBoardThemeNonDestructive(AppTheme nextTheme) {
        currentBoardTheme = nextTheme;
        binding.boardCanvas.applyTheme(currentBoardTheme);
        container.themeManager.saveThemePreferenceOnly(currentBoardTheme);
        updateHud();
    }

    private void executeTurn() {
        isRolling = true;
        binding.btnRollDice.setEnabled(false);

        CoupleBoardEngine.MoveResult move = engine.rollAndAdvance();
        String actorName = container.profileManager.getPlayerName(move.movedPlayerA);
        binding.tvBoardStatus.setText(getString(R.string.board_rolling_status, actorName));

        diceAnimator = PhysicsAnimator.rollDiceSuspense(binding.tvDiceBadge, move.diceValue, () -> {
            if (binding == null) return;
            binding.tvBoardStatus.setText(getString(R.string.board_rolled_status, actorName, move.diceValue));

            binding.boardCanvas.animateTokenMove(move.movedPlayerA, move.fromIndex, move.landedIndex, () -> {
                if (binding == null) return;
                if (move.swapped) {
                    binding.boardCanvas.syncPositionsInstant(engine.getPlayerAPos(), engine.getPlayerBPos());
                    binding.tvBoardStatus.setText(R.string.board_event_swap);
                    finishMoveStep(move);
                } else if (move.finalIndex != move.landedIndex) {
                    binding.tvBoardStatus.setText(
                            move.finalIndex > move.landedIndex
                                    ? R.string.board_event_boost
                                    : R.string.board_event_back
                    );
                    binding.boardCanvas.animateTokenMove(move.movedPlayerA, move.landedIndex, move.finalIndex, () -> {
                        if (binding != null) finishMoveStep(move);
                    });
                } else {
                    finishMoveStep(move);
                }
            });
        });
    }

    private void finishMoveStep(CoupleBoardEngine.MoveResult move) {
        updateHud();
        if (move.wonGame) {
            isRolling = false;
            binding.btnRollDice.setEnabled(true);
            showVictoryDialog(move.movedPlayerA);
            return;
        }

        if (move.grantExtraRoll) {
            binding.tvBoardStatus.setText(R.string.board_event_extra_roll);
        }

        boolean prioritizeDesireMatch = (move.triggeredType == BoardTileType.DESIRE_WILDCARD);
        if (prioritizeDesireMatch) {
            binding.tvBoardStatus.setText(R.string.board_event_desire_wildcard);
        }

        drawAndPresentTask(move.movedPlayerA, move.landedTier, prioritizeDesireMatch);
    }

    private void drawAndPresentTask(boolean actingPlayerA, int tier, boolean prioritizeDesireMatch) {
        int maxAllowed = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard picked = null;
            if (prioritizeDesireMatch && mutualMatches != null && !mutualMatches.isEmpty()) {
                picked = container.taskEngine.drawNext(mutualMatches, mutualMatches, maxAllowed, null, false);
            }
            if (picked == null) {
                int cappedTier = Math.min(tier, maxAllowed);
                picked = container.taskEngine.drawNext(allCards, mutualMatches, cappedTier, null, true);
            }

            TaskCard finalPicked = picked;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                isRolling = false;
                binding.btnRollDice.setEnabled(true);
                if (finalPicked == null) {
                    Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                } else {
                    String badge = prioritizeDesireMatch ? getString(R.string.badge_mutual_desire) : "";
                    TaskPromptDialogHelper.show(
                            this,
                            container,
                            dareTimer,
                            actingPlayerA,
                            finalPicked,
                            badge,
                            getString(R.string.btn_partner_redo),
                            new TaskPromptDialogHelper.Callback() {
                                @Override
                                public void onApproved() {
                                    engine.recordTaskApproved(actingPlayerA);
                                    updateHud();
                                }

                                @Override
                                public void onSecondaryAction() {
                                    engine.applyStepBackPenalty(actingPlayerA);
                                    binding.boardCanvas.syncPositionsInstant(engine.getPlayerAPos(), engine.getPlayerBPos());
                                    updateHud();
                                }

                                @Override
                                public void onSkipped() {
                                    engine.recordTaskSkipped();
                                }
                            }
                    );
                }
            });
        });
    }

    private void showRewardsDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_board_setup, null);
        TextView lblA = view.findViewById(R.id.tv_reward_a_label);
        TextView lblB = view.findViewById(R.id.tv_reward_b_label);
        EditText etA = view.findViewById(R.id.et_reward_a);
        EditText etB = view.findViewById(R.id.et_reward_b);

        lblA.setText(getString(R.string.board_reward_named_label, container.profileManager.getPlayerName(true)));
        lblB.setText(getString(R.string.board_reward_named_label, container.profileManager.getPlayerName(false)));
        etA.setText(engine.getPlayerAReward());
        etB.setText(engine.getPlayerBReward());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.board_rewards_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    engine.setRewards(etA.getText().toString(), etB.getText().toString());
                    Toast.makeText(requireContext(), R.string.board_rewards_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showVictoryDialog(boolean playerAWon) {
        HapticHelper.confirm(binding.getRoot());
        container.contentRepository.recordSessionStat(
                "COUPLE_BOARD",
                engine.getPlayerACompleted(),
                engine.getPlayerBCompleted(),
                engine.getSkipsUsed()
        );

        String winnerName = container.profileManager.getPlayerName(playerAWon);
        String reward = playerAWon ? engine.getPlayerAReward() : engine.getPlayerBReward();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.board_victory_title, winnerName))
                .setMessage(getString(R.string.board_victory_message, reward))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_play_again, (d, which) -> {
                    engine.resetMatch();
                    container.taskEngine.resetSkipsOnly();
                    binding.boardCanvas.syncPositionsInstant(0, 0);
                    updateHud();
                })
                .show();
    }

    private void updateHud() {
        if (binding == null || !isAdded()) return;
        String turnLabel = container.profileManager.getPlayerName(engine.isPlayerATurn());
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);

        binding.tvTurnIndicator.setText(getString(R.string.board_turn_indicator, turnLabel));
        binding.tvPlayerAPos.setText(getString(R.string.board_player_pos, nameA, engine.getPlayerAPos() + 1, CoupleBoardEngine.TOTAL_TILES));
        binding.tvPlayerBPos.setText(getString(R.string.board_player_pos, nameB, engine.getPlayerBPos() + 1, CoupleBoardEngine.TOTAL_TILES));
        binding.btnCycleTheme.setText(getString(currentBoardTheme.titleRes));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diceAnimator != null) diceAnimator.cancel();
        dareTimer.cancel();
        binding = null;
    }
}
