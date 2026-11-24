package com.velvet.couplesgame.ui.connect4;

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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.Connect4ActionDto;
import com.velvet.couplesgame.databinding.FragmentNaughtyConnect4Binding;
import java.util.ArrayList;
import java.util.List;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;

public class NaughtyConnect4Fragment extends Fragment {
    private FragmentNaughtyConnect4Binding binding;
    private AppContainer container;
    private final Connect4Engine engine = new Connect4Engine();
    private Connect4GridAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNaughtyConnect4Binding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        adapter = new Connect4GridAdapter(container.themeManager.getCurrentTheme(), this::handleDrop);
        binding.rvC4Grid.setLayoutManager(new GridLayoutManager(requireContext(), Connect4Engine.COLS));
        binding.rvC4Grid.setAdapter(adapter);

        binding.btnC4Reset.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.resetBoard();
            binding.tvC4LiveDare.setText(engine.hasEligibleActions()
                    ? R.string.c4_ready_hint
                    : R.string.msg_no_eligible_cards);
            refreshBoard();
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadExtremeModesAsync(bundle -> {
            List<Connect4ActionDto> combined = new ArrayList<>();
            if (bundle.connect4Actions != null) combined.addAll(bundle.connect4Actions);
            combined.addAll(container.modeSynthesizer.synthesizeConnect4Actions(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 120
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadActionsAndReset(combined, container.consentFilter, maxTier);
                if (!engine.hasEligibleActions()) {
                    binding.tvC4LiveDare.setText(R.string.msg_no_eligible_cards);
                }
                refreshBoard();
            });
        });
    }

    private void handleDrop(int col) {
        if (!engine.hasEligibleActions()) {
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        Connect4Engine.DropResult res = engine.dropInColumn(col);
        if (!res.valid) return;

        refreshBoard();
        String actor = container.profileManager.getPlayerName(res.droppedByA);
        String other = container.profileManager.getPlayerName(!res.droppedByA);

        if (res.wonMatch) {
            HapticHelper.confirm(binding.rvC4Grid);
            container.contentRepository.recordSessionStat("NAUGHTY_CONNECT4", engine.getWinsA(), engine.getWinsB(), 0);
            StringBuilder combo = new StringBuilder();
            int idx = 1;
            for (Connect4Engine.Cell wc : res.winningLine) {
                if (wc.action != null) {
                    combo.append(idx++).append(". ").append(wc.action.fullDare).append("\n\n");
                }
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.c4_victory_title, actor))
                    .setMessage(getString(R.string.c4_victory_combo_msg, other, combo.toString().trim()))
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_play_again, (d, w) -> {
                        if (binding == null) return;
                        engine.resetBoard();
                        binding.tvC4LiveDare.setText(R.string.c4_ready_hint);
                        refreshBoard();
                    })
                    .show();
        } else if (res.boardFullDraw) {
            HapticHelper.tick(binding.rvC4Grid);
            binding.tvC4LiveDare.setText(R.string.c4_draw_banner);
        } else {
            HapticHelper.tick(binding.rvC4Grid);
            String dareText = (res.landedCell != null && res.landedCell.action != null)
                    ? res.landedCell.action.fullDare
                    : "";
            binding.tvC4LiveDare.setText(getString(R.string.c4_move_dare_fmt, actor, dareText));
            PhysicsAnimator.popIn(binding.tvC4LiveDare);
        }
    }

    private void refreshBoard() {
        if (binding == null || !isAdded()) return;
        adapter.submitCells(engine.flattenCells());
        String turn = container.profileManager.getPlayerName(engine.isPlayerATurn());
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);
        binding.tvC4Turn.setText(getString(R.string.board_turn_indicator, turn));
        binding.tvC4Score.setText(getString(R.string.c4_score_fmt, nameA, engine.getWinsA(), nameB, engine.getWinsB()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
