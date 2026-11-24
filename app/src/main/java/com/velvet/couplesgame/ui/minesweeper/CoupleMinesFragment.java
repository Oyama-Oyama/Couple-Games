package com.velvet.couplesgame.ui.minesweeper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentCoupleMinesBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;

public class CoupleMinesFragment extends Fragment {
    private FragmentCoupleMinesBinding binding;
    private AppContainer container;
    private final CoupleMinesEngine engine = new CoupleMinesEngine(8);
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private MinesweeperGridAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCoupleMinesBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        adapter = new MinesweeperGridAdapter(
                container.themeManager.getCurrentTheme(),
                new MinesweeperGridAdapter.CellListener() {
                    @Override
                    public void onReveal(MineCell cell) {
                        handleCellReveal(cell);
                    }

                    @Override
                    public void onToggleFlag(MineCell cell) {
                        if (engine.toggleFlag(cell.row, cell.col)) {
                            HapticHelper.tick(binding.rvMinesGrid);
                            refreshBoard();
                        }
                    }
                }
        );

        binding.rvMinesGrid.setLayoutManager(new GridLayoutManager(requireContext(), CoupleMinesEngine.GRID_SIZE));
        binding.rvMinesGrid.setAdapter(adapter);

        binding.btnMinesEasy.setOnClickListener(v -> restartGame(6));
        binding.btnMinesNormal.setOnClickListener(v -> restartGame(8));
        binding.btnMinesSpicy.setOnClickListener(v -> restartGame(10));
        binding.btnMinesReset.setOnClickListener(v -> restartGame(engine.getMineCount()));

        refreshBoard();
    }

    private void restartGame(int mineCount) {
        HapticHelper.tick(binding.getRoot());
        engine.reset(mineCount);
        binding.tvMinesStatus.setText(R.string.mines_ready_hint);
        refreshBoard();
    }

    private void handleCellReveal(MineCell cell) {
        CoupleMinesEngine.MoveOutcome outcome = engine.revealCell(cell.row, cell.col);
        if (outcome.type == CoupleMinesEngine.RevealOutcome.IGNORED) return;

        refreshBoard();

        if (outcome.type == CoupleMinesEngine.RevealOutcome.SAFE) {
            HapticHelper.tick(binding.rvMinesGrid);
            binding.tvMinesStatus.setText(getString(R.string.mines_safe_status, outcome.newlyRevealedCount));
        } else if (outcome.type == CoupleMinesEngine.RevealOutcome.HEART_MINE_TRIGGERED) {
            HapticHelper.confirm(binding.rvMinesGrid);
            String actor = container.profileManager.getPlayerName(outcome.actingPlayerA);
            binding.tvMinesStatus.setText(getString(R.string.mines_hit_status, actor));
            triggerHeartMineDare(outcome.actingPlayerA, outcome.suggestedTier);
        } else if (outcome.type == CoupleMinesEngine.RevealOutcome.BOARD_CLEARED) {
            HapticHelper.confirm(binding.rvMinesGrid);
            showClearedDialog();
        }
    }

    private void triggerHeartMineDare(boolean actorA, int tier) {
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            int allowedTier = Math.min(tier, container.entitlementManager.getSelectedMaxTier());
            TaskCard dare = container.taskEngine.drawNext(allCards, mutualMatches, allowedTier, null, true);
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
                        actorA,
                        dare,
                        getString(R.string.mines_badge_label),
                        getString(R.string.btn_wheel_pass_turn),
                        new TaskPromptDialogHelper.Callback() {
                            @Override
                            public void onApproved() {
                                refreshBoard();
                            }

                            @Override
                            public void onSecondaryAction() {
                                refreshBoard();
                            }
                        }
                );
            });
        });
    }

    private void showClearedDialog() {
        container.contentRepository.recordSessionStat(
                "COUPLE_MINES",
                engine.getPlayerASafeScore(),
                engine.getPlayerBSafeScore(),
                0
        );

        String winner = container.profileManager.getPlayerName(engine.getPlayerASafeScore() >= engine.getPlayerBSafeScore());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.mines_cleared_title, winner))
                .setMessage(getString(
                        R.string.mines_cleared_summary,
                        container.profileManager.getPlayerName(true),
                        engine.getPlayerASafeScore(),
                        engine.getPlayerAMinesHit(),
                        container.profileManager.getPlayerName(false),
                        engine.getPlayerBSafeScore(),
                        engine.getPlayerBMinesHit()
                ))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_play_again, (d, w) -> restartGame(engine.getMineCount()))
                .show();
    }

    private void refreshBoard() {
        if (binding == null || !isAdded()) return;
        adapter.submitCells(engine.flattenCells());
        String turnName = container.profileManager.getPlayerName(engine.isPlayerATurn());
        binding.tvMinesTurn.setText(getString(R.string.board_turn_indicator, turnName));
        binding.tvMinesScoreA.setText(getString(
                R.string.mines_player_score,
                container.profileManager.getPlayerName(true),
                engine.getPlayerASafeScore(),
                engine.getPlayerAMinesHit()
        ));
        binding.tvMinesScoreB.setText(getString(
                R.string.mines_player_score,
                container.profileManager.getPlayerName(false),
                engine.getPlayerBSafeScore(),
                engine.getPlayerBMinesHit()
        ));
        int mines = engine.getMineCount();
        binding.btnMinesEasy.setAlpha(mines == 6 ? 1.0f : 0.52f);
        binding.btnMinesNormal.setAlpha(mines == 8 ? 1.0f : 0.52f);
        binding.btnMinesSpicy.setAlpha(mines == 10 ? 1.0f : 0.52f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        binding = null;
    }
}
