package com.velvet.couplesgame.ui.social;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.engine.SocialGamesEngine;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentSocialGameBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import java.util.List;

/**
 * Generic host Fragment for the 12 social games. The mode is selected via the
 * {@code ARG_MODE_ID} argument supplied by each navigation action, so all modes share one
 * well-tested loading / consent / anti-repeat pipeline while keeping distinct pacing.
 */
public class SocialGameFragment extends Fragment {

    public static final String ARG_MODE_ID = "social_mode_id";
    private static final String DEFAULT_MODE_ID = "daily_question";

    private FragmentSocialGameBinding binding;
    private AppContainer container;
    private SocialGamesEngine engine;
    private final DareTimerHelper dareTimer = new DareTimerHelper();

    private String modeId;
    private String currentPrompt;
    /** Number of prompts actually completed since the last history write (survives card draws). */
    private int completedSinceLastWrite;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSocialGameBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        modeId = getArguments() != null ? getArguments().getString(ARG_MODE_ID, DEFAULT_MODE_ID)
                : DEFAULT_MODE_ID;
        if (SocialGamesEngine.configFor(modeId) == null) modeId = DEFAULT_MODE_ID;
        engine = new SocialGamesEngine(modeId);

        GameModeCatalog catalog = GameModeCatalog.fromId(modeId);
        int titleRes = catalog != null ? catalog.titleRes : R.string.mode_daily_question;
        binding.tvSocialPhaseBanner.setText(getString(titleRes));

        binding.btnSocialPrimary.setOnClickListener(v -> onPrimaryClicked(v));
        binding.btnSocialSkip.setOnClickListener(v -> {
            HapticHelper.tick(v);
            nextCard();
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadPool(allCards, container.consentFilter, maxTier);
                startRound();
            });
        });
    }

    private void startRound() {
        nextCard();
    }

    private void nextCard() {
        if (binding == null || !isAdded()) return;
        dareTimer.cancel();
        TaskCard card = engine.drawNext();

        if (card == null) {
            binding.cardSocialPrompt.setVisibility(View.GONE);
            binding.tvSocialTimer.setVisibility(View.GONE);
            binding.btnSocialPrimary.setVisibility(View.GONE);
            binding.btnSocialSkip.setVisibility(View.GONE);
            binding.tvSocialPhaseBanner.setText(R.string.social_result);
            binding.progressSocial.setMax(1);
            binding.progressSocial.setProgress(1);
            return;
        }
        binding.btnSocialSkip.setVisibility(View.VISIBLE);

        currentPrompt = card.text;
        binding.cardSocialPrompt.setVisibility(View.VISIBLE);
        binding.tvSocialTier.setText(getString(R.string.social_round) + " " +
                engine.getRoundIndex() + " " + getString(R.string.social_of) + " " +
                engine.getConfig().roundSize);
        binding.tvSocialPrompt.setText(card.text);
        binding.progressSocial.setMax(Math.max(1, engine.getConfig().roundSize));
        binding.progressSocial.setProgress(Math.min(engine.getRoundIndex(), engine.getConfig().roundSize));

        // Player name substitution if the official card mentions {p1}/{p2}
        if (currentPrompt != null && (currentPrompt.contains("{p1}") || currentPrompt.contains("{p2}"))) {
            String p1 = container.profileManager.getPlayerName(true);
            String p2 = container.profileManager.getPlayerName(false);
            currentPrompt = currentPrompt.replace("{p1}", p1).replace("{p2}", p2);
            binding.tvSocialPrompt.setText(currentPrompt);
        }

        // Open-ended prompt: the couple responds together, then marks it done.
        // No multiple-choice answers - official cards are open questions/actions.
        binding.btnSocialPrimary.setText(R.string.social_done);

        if (engine.isTimed()) {
            startTimer(engine.getDurationSec());
        } else {
            binding.tvSocialTimer.setVisibility(View.GONE);
        }
    }

    private void startTimer(int seconds) {
        if (seconds <= 0) {
            binding.tvSocialTimer.setVisibility(View.GONE);
            return;
        }
        binding.tvSocialTimer.setVisibility(View.VISIBLE);
        binding.tvSocialTimer.setText(String.valueOf(seconds));
        dareTimer.start(seconds, new DareTimerHelper.Listener() {
            @Override
            public void onTick(int secondsRemaining) {
                if (binding != null) binding.tvSocialTimer.setText(String.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                if (binding == null || !isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    HapticHelper.countdownFinished(binding.getRoot());
                    if (binding.tvSocialTimer != null) {
                        binding.tvSocialTimer.setText(R.string.social_timeup);
                    }
                });
            }
        });
    }

    private void onPrimaryClicked(View v) {
        if (binding == null) return;
        completeCurrent(v);
    }

    private void completeCurrent(View v) {
        boolean roundComplete = engine.markDone()
                || engine.getRoundIndex() >= engine.getConfig().roundSize;
        completedSinceLastWrite++;
        if (roundComplete) {
            HapticHelper.confirm(v);
            writeHistory();
            engine.resetRound();
        } else {
            HapticHelper.tick(v);
        }
        nextCard();
    }

    /** Persist the round to the Date Night History with correct, meaningful counts. */
    private void writeHistory() {
        if (container == null || completedSinceLastWrite <= 0) return;
        int roundSize = engine.getConfig().roundSize;
        // Both partners complete each prompt together, so each side gets the full count.
        container.contentRepository.recordSessionStat(
                engine.getModeId().toUpperCase(),
                completedSinceLastWrite,
                completedSinceLastWrite,
                0
        );
        completedSinceLastWrite = 0;
    }

    @Override
    public void onDestroyView() {
        dareTimer.cancel();
        // Flush any partially completed round so early exits are still recorded.
        if (container != null) writeHistory();
        binding = null;
        super.onDestroyView();
    }
}
