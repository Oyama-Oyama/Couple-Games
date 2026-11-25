package com.velvet.couplesgame.ui.drawguess;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.model.DrawWord;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.databinding.FragmentDrawGuessBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;

public class DrawGuessFragment extends Fragment {
    private static final int ROUND_DURATION_SEC = 60;
    private static final String REQ_DRAW_SHIELD = "req_draw_guess_shield";

    private FragmentDrawGuessBinding binding;
    private AppContainer container;
    private final DrawGuessSessionManager session = new DrawGuessSessionManager();
    private final DareTimerHelper roundTimer = new DareTimerHelper();
    private final DareTimerHelper dareTimer = new DareTimerHelper();

    private boolean isWordRevealed = false;
    private boolean isRoundActive = false;
    private boolean isWideBrush = false;
    private View[] colorDots;
    private int[] dotColors;
    private static final int CUSTOM_COLOR_IDX = -1;
    private int selectedColorIdx = 0;
    private int customPickedColor = 0xFFFF9F68;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDrawGuessBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        session.attachHistoryStore(container.promptHistory);

        getChildFragmentManager().setFragmentResultListener(REQ_DRAW_SHIELD, getViewLifecycleOwner(), (key, res) ->
                startActiveDrawingTurn()
        );

        AppTheme theme = container.themeManager.getCurrentTheme();
        binding.drawingCanvas.applyTheme(theme);
        binding.drawingCanvas.setDrawingEnabled(false);
        binding.tvDrawTimer.setText(getString(R.string.timer_seconds_fmt, ROUND_DURATION_SEC));
        binding.btnToggleWord.setVisibility(View.GONE);

        setupBrushPalette(theme);

        binding.btnUndo.setOnClickListener(v -> {
            HapticHelper.tick(v);
            binding.drawingCanvas.undo();
        });
        binding.btnClear.setOnClickListener(v -> {
            HapticHelper.tick(v);
            binding.drawingCanvas.clear();
        });
        binding.btnBrushSize.setOnClickListener(v -> {
            HapticHelper.tick(v);
            isWideBrush = !isWideBrush;
            binding.drawingCanvas.setStrokeWidthDp(isWideBrush ? 11f : 5f);
            binding.btnBrushSize.setText(isWideBrush ? R.string.draw_brush_bold : R.string.draw_brush_fine);
        });

        binding.btnStartRound.setOnClickListener(v -> beginRoundWithPrivacyShield());
        binding.btnToggleWord.setOnClickListener(v -> {
            isWordRevealed = !isWordRevealed;
            updateWordMask();
        });
        binding.btnGuessed.setOnClickListener(v -> completeRound(true));
        binding.btnMissed.setOnClickListener(v -> completeRound(false));

        container.contentRepository.loadDrawWordsAsync(words -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                session.loadWords(words, container.consentFilter, container.getEffectiveMaxTier());
                updateHud();
            });
        });
    }

    private void setupBrushPalette(AppTheme theme) {
        int ivory = ContextCompat.getColor(requireContext(), theme.textPrimaryColorRes);
        int accent = ContextCompat.getColor(requireContext(), theme.accentColorRes);
        int primary = ContextCompat.getColor(requireContext(), theme.primaryColorRes);
        int rose = 0xFFFF7EB6;
        int mint = 0xFF64DFDF;

        colorDots = new View[]{
                binding.colorDot1, binding.colorDot2, binding.colorDot3, binding.colorDot4, binding.colorDot5
        };
        dotColors = new int[]{ivory, accent, primary, rose, mint};

        for (int i = 0; i < colorDots.length; i++) {
            final int index = i;
            colorDots[i].setOnClickListener(v -> {
                HapticHelper.tick(v);
                selectedColorIdx = index;
                binding.drawingCanvas.setBrushColor(dotColors[index]);
                refreshPaletteSelection();
            });
        }
        binding.btnColorPicker.setOnClickListener(v -> {
            HapticHelper.tick(v);
            int startColor = (selectedColorIdx == CUSTOM_COLOR_IDX)
                    ? customPickedColor
                    : dotColors[Math.max(0, selectedColorIdx)];
            ColorPickerDialogHelper.show(requireContext(), startColor, picked -> {
                if (binding == null || !isAdded()) return;
                customPickedColor = picked;
                selectedColorIdx = CUSTOM_COLOR_IDX;
                binding.drawingCanvas.setBrushColor(picked);
                refreshPaletteSelection();
            });
        });

        selectedColorIdx = 0;
        binding.drawingCanvas.setBrushColor(dotColors[0]);
        refreshPaletteSelection();
    }

    private void refreshPaletteSelection() {
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < colorDots.length; i++) {
            boolean selected = (i == selectedColorIdx);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(dotColors[i]);
            bg.setStroke(
                    Math.round((selected ? 2.6f : 1.2f) * density),
                    selected ? 0xFFFFFFFF : 0x66FFFFFF
            );
            colorDots[i].setBackground(bg);
            colorDots[i].setScaleX(selected ? 1.2f : 1.0f);
            colorDots[i].setScaleY(selected ? 1.2f : 1.0f);
        }

        boolean customSelected = (selectedColorIdx == CUSTOM_COLOR_IDX);
        GradientDrawable pickerBg = new GradientDrawable();
        pickerBg.setShape(GradientDrawable.OVAL);
        pickerBg.setColor(customSelected ? customPickedColor : 0x33FFFFFF);
        pickerBg.setStroke(
                Math.round((customSelected ? 2.6f : 1.4f) * density),
                customSelected ? 0xFFFFFFFF : 0xAAFFFFFF
        );
        binding.btnColorPicker.setBackground(pickerBg);
        binding.btnColorPicker.setScaleX(customSelected ? 1.2f : 1.0f);
        binding.btnColorPicker.setScaleY(customSelected ? 1.2f : 1.0f);
    }

    private void beginRoundWithPrivacyShield() {
        DrawWord nextWord = session.drawNextWord();
        if (nextWord == null) {
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }

        String drawerName = container.profileManager.getPlayerName(session.isPlayerADrawing());
        PassPhoneDialog.newInstance(REQ_DRAW_SHIELD, drawerName)
                .show(getChildFragmentManager(), "draw_guess_shield");
    }

    private void startActiveDrawingTurn() {
        if (binding == null || !isAdded()) return;
        binding.drawingCanvas.clear();
        binding.drawingCanvas.setDrawingEnabled(true);
        isWordRevealed = true;
        isRoundActive = true;
        updateHud();
        updateWordMask();

        binding.btnToggleWord.setVisibility(View.VISIBLE);
        binding.btnStartRound.setVisibility(View.GONE);
        binding.layoutActiveActions.setVisibility(View.VISIBLE);

        roundTimer.start(ROUND_DURATION_SEC, new DareTimerHelper.Listener() {
            @Override
            public void onTick(int secondsRemaining) {
                if (binding != null && isAdded()) {
                    binding.tvDrawTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                }
            }

            @Override
            public void onFinish() {
                if (binding != null && isRoundActive && isAdded()) {
                    HapticHelper.countdownFinished(binding.getRoot());
                    binding.tvDrawTimer.setText(R.string.timer_done);
                    completeRound(false);
                }
            }
        });
    }

    private void completeRound(boolean guessed) {
        if (!isRoundActive) return;
        isRoundActive = false;
        roundTimer.cancel();
        binding.drawingCanvas.setDrawingEnabled(false);

        boolean actingDrawerA = session.isPlayerADrawing();
        DrawWord word = session.getCurrentWord();

        if (guessed) {
            HapticHelper.confirm(binding.getRoot());
            session.markRoundGuessed();
            Toast.makeText(requireContext(), R.string.draw_guessed_toast, Toast.LENGTH_SHORT).show();
            resetToIdle();
        } else {
            session.markRoundMissed();
            resetToIdle();
            int rawPenaltyTier = word != null ? Math.min(4, Math.max(2, word.tier)) : 2;
            int cappedPenaltyTier = Math.min(rawPenaltyTier, container.getEffectiveMaxTier());
            triggerMissPenaltyDare(actingDrawerA, cappedPenaltyTier);
        }

        if (session.getRoundsPlayed() > 0 && session.getRoundsPlayed() % 4 == 0) {
            container.contentRepository.recordSessionStat(
                    "DRAW_GUESS", session.getScoreA(), session.getScoreB(), 0
            );
        }
    }

    private void triggerMissPenaltyDare(boolean actorA, int tier) {
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard dare = container.taskEngine.drawNext(allCards, mutualMatches, tier, null, true);
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
                        getString(R.string.draw_penalty_badge),
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

    private void resetToIdle() {
        isWordRevealed = false;
        binding.btnToggleWord.setVisibility(View.GONE);
        binding.btnStartRound.setVisibility(View.VISIBLE);
        binding.layoutActiveActions.setVisibility(View.GONE);
        binding.tvDrawTimer.setText(getString(R.string.timer_seconds_fmt, ROUND_DURATION_SEC));
        binding.tvSecretWord.setText(R.string.draw_ready_banner);
        updateHud();
    }

    private void updateWordMask() {
        DrawWord word = session.getCurrentWord();
        if (word == null) return;
        if (isWordRevealed) {
            binding.tvSecretWord.setText(word.word);
            binding.btnToggleWord.setText(R.string.draw_hide_word);
        } else {
            binding.tvSecretWord.setText("• • • • •");
            binding.btnToggleWord.setText(R.string.draw_peek_word);
        }
    }

    private void updateHud() {
        if (binding == null || !isAdded()) return;
        String drawer = container.profileManager.getPlayerName(session.isPlayerADrawing());
        binding.tvDrawRole.setText(getString(R.string.draw_role_banner, drawer));
        binding.tvDrawScore.setText(getString(R.string.draw_score_banner, session.getScoreA(), session.getRoundsPlayed()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        roundTimer.cancel();
        dareTimer.cancel();
        binding = null;
    }
}
