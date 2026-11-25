package com.velvet.couplesgame.ui.social;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.text.InputType;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.engine.SocialGamesEngine;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentSocialGameBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Generic host Fragment for the 12 social games. The mode is selected via the
 * {@code ARG_MODE_ID} argument supplied by each navigation action, so all modes share one
 * well-tested loading / consent / anti-repeat pipeline while keeping distinct pacing.
 */
public class SocialGameFragment extends Fragment {

    public static final String ARG_MODE_ID = "social_mode_id";
    private static final String DEFAULT_MODE_ID = "daily_question";
    private static final String REQ_SOCIAL_HANDOFF = "social_game_handoff";

    private FragmentSocialGameBinding binding;
    private AppContainer container;
    private SocialGamesEngine engine;
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private SocialWishListStore wishListStore;

    private String modeId;
    private String currentPrompt;
    private String currentRiddleAnswer;
    private String answerA;
    private String answerB;
    private boolean awaitingPlayerB;
    private boolean sharedCountdownRunning;
    private boolean sharedCountdownFinished;
    private boolean forfeitRevealed;
    private boolean forfeitSpinCancelled;
    private ObjectAnimator forfeitAnimator;
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
        wishListStore = new SocialWishListStore(requireContext());

        modeId = getArguments() != null ? getArguments().getString(ARG_MODE_ID, DEFAULT_MODE_ID)
                : DEFAULT_MODE_ID;
        if (SocialGamesEngine.configFor(modeId) == null) modeId = DEFAULT_MODE_ID;
        engine = new SocialGamesEngine(modeId);

        getChildFragmentManager().setFragmentResultListener(REQ_SOCIAL_HANDOFF, getViewLifecycleOwner(),
                (key, bundle) -> {
                    if (bundle.getBoolean(PassPhoneDialog.RESULT_CONFIRMED, false)) {
                        showPlayerBResponse();
                    }
                });

        GameModeCatalog catalog = GameModeCatalog.fromId(modeId);
        int titleRes = catalog != null ? catalog.titleRes : R.string.mode_daily_question;
        binding.tvSocialPhaseBanner.setText(getString(titleRes));

        binding.btnSocialPrimary.setOnClickListener(v -> onPrimaryClicked(v));
        binding.btnSocialWishlist.setOnClickListener(v -> showWishlist());
        binding.btnSocialSkip.setOnClickListener(v -> {
            HapticHelper.tick(v);
            nextCard();
        });

        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadPool(getModeSpecificCards(allCards), container.consentFilter, maxTier);
                startRound();
            });
        });
    }

    private List<TaskCard> getModeSpecificCards(List<TaskCard> fallbackCards) {
        int promptArrayRes;
        if ("most_likely_to".equals(modeId)) {
            promptArrayRes = R.array.social_most_likely_prompts;
        } else if ("love_language".equals(modeId)) {
            promptArrayRes = R.array.social_love_language_prompts;
        } else if ("riddle_me".equals(modeId)) {
            promptArrayRes = R.array.social_riddle_prompts;
        } else {
            return fallbackCards;
        }

        String[] prompts = getResources().getStringArray(promptArrayRes);
        String tag = engine.getConfig().preferredTags.get(0);
        List<String> tags = Collections.singletonList(tag);
        List<TaskCard> cards = new ArrayList<>(prompts.length);
        for (int i = 0; i < prompts.length; i++) {
            cards.add(new TaskCard("social_" + modeId + "_" + i, modeId, 1,
                    "TRUTH", tags, 0, prompts[i], false, false));
        }
        return cards;
    }

    private void startRound() {
        nextCard();
    }

    private void nextCard() {
        if (binding == null || !isAdded()) return;
        dareTimer.cancel();
        if (forfeitAnimator != null) {
            forfeitSpinCancelled = true;
            forfeitAnimator.cancel();
            forfeitAnimator = null;
        }
        answerA = null;
        answerB = null;
        awaitingPlayerB = false;
        currentRiddleAnswer = null;
        sharedCountdownRunning = false;
        sharedCountdownFinished = false;
        forfeitRevealed = false;
        TaskCard card = engine.drawNext();

        if (card == null) {
            binding.tvSocialSoftLimitWarning.setVisibility(View.GONE);
            binding.checkSocialSoftLimitAck.setVisibility(View.GONE);
            binding.cardSocialPrompt.setVisibility(View.GONE);
            binding.tvSocialTimer.setVisibility(View.GONE);
            binding.btnSocialPrimary.setVisibility(View.GONE);
            binding.btnSocialWishlist.setVisibility(View.GONE);
            binding.btnSocialSkip.setVisibility(View.GONE);
            binding.tvSocialPhaseBanner.setText(R.string.social_result);
            binding.progressSocial.setMax(1);
            binding.progressSocial.setProgress(1);
            return;
        }
        binding.btnSocialSkip.setVisibility(View.VISIBLE);

        boolean requiresSoftLimitAck = SoftLimitPromptHelper.bind(
                container.consentFilter,
                card.tags,
                binding.tvSocialSoftLimitWarning,
                binding.checkSocialSoftLimitAck,
                binding.btnSocialPrimary
        );
        currentPrompt = card.text;
        binding.cardSocialPrompt.setVisibility(View.VISIBLE);
        binding.tvSocialTier.setText(getString(R.string.social_round) + " " +
                engine.getRoundIndex() + " " + getString(R.string.social_of) + " " +
                engine.getConfig().roundSize);
        binding.tvSocialPrompt.setText(card.text);
        binding.progressSocial.setMax(Math.max(1, engine.getConfig().roundSize));
        binding.progressSocial.setProgress(Math.min(engine.getRoundIndex(), engine.getConfig().roundSize));

        currentPrompt = substituteNames(currentPrompt);
        binding.tvSocialPrompt.setText(currentPrompt);
        if ("riddle_me".equals(modeId)) {
            int separator = card.id.lastIndexOf('_');
            int index = Integer.parseInt(card.id.substring(separator + 1));
            String[] answers = getResources().getStringArray(R.array.social_riddle_answers);
            if (index >= 0 && index < answers.length) currentRiddleAnswer = answers[index];
        }

        SocialGamesEngine.Interaction interaction = engine.getConfig().interaction;
        boolean forfeitWheel = "forfeit_wheel".equals(modeId);
        binding.tvSocialPrompt.setAlpha(forfeitWheel ? 0f : 1f);
        binding.tvSocialPrompt.setImportantForAccessibility(forfeitWheel
                ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        binding.btnSocialWishlist.setVisibility(
                interaction == SocialGamesEngine.Interaction.WISH_LIST ? View.VISIBLE : View.GONE);
        if (forfeitWheel) {
            binding.btnSocialPrimary.setText(R.string.social_spin_forfeit);
        } else if (interaction == SocialGamesEngine.Interaction.PRIVATE_ANSWERS) {
            binding.btnSocialPrimary.setText(R.string.social_answer_privately);
        } else if (interaction == SocialGamesEngine.Interaction.PRIVATE_CHOICES) {
            binding.btnSocialPrimary.setText(R.string.social_choose_privately);
        } else if (interaction == SocialGamesEngine.Interaction.TIMED_PRIVATE_ANSWERS) {
            binding.btnSocialPrimary.setText(R.string.social_answer_privately);
        } else if (interaction == SocialGamesEngine.Interaction.WISH_LIST) {
            binding.btnSocialPrimary.setText(R.string.social_add_wish);
        } else if (interaction == SocialGamesEngine.Interaction.SHARED_COUNTDOWN) {
            binding.btnSocialPrimary.setText(R.string.social_sync_start);
        } else {
            binding.btnSocialPrimary.setText(R.string.social_done);
        }

        if (forfeitWheel) {
            binding.tvSocialTimer.setVisibility(View.GONE);
            if (requiresSoftLimitAck) {
                binding.checkSocialSoftLimitAck.setOnCheckedChangeListener((button, checked) -> {
                    if (binding != null) binding.btnSocialPrimary.setEnabled(checked);
                });
            }
        } else if (engine.isTimed()) {
            if (requiresSoftLimitAck) {
                int durationSec = engine.getDurationSec();
                binding.tvSocialTimer.setVisibility(View.VISIBLE);
                binding.tvSocialTimer.setText(String.valueOf(durationSec));
                binding.checkSocialSoftLimitAck.setOnCheckedChangeListener((button, checked) -> {
                    if (binding == null) return;
                    binding.btnSocialPrimary.setEnabled(checked);
                    if (checked) {
                        if (!"forfeit_wheel".equals(modeId)) startTimer(durationSec);
                    } else {
                        dareTimer.cancel();
                        binding.tvSocialTimer.setText(String.valueOf(durationSec));
                    }
                });
            } else {
                startTimer(engine.getDurationSec());
            }
        } else if (interaction == SocialGamesEngine.Interaction.SHARED_COUNTDOWN) {
            binding.tvSocialTimer.setVisibility(View.GONE);
        } else {
            binding.tvSocialTimer.setVisibility(View.GONE);
        }
    }

    private String substituteNames(String text) {
        if (text == null) return "";
        return text.replace("{p1}", container.profileManager.getPlayerName(true))
                .replace("{p2}", container.profileManager.getPlayerName(false))
                .replace("{partner}", container.profileManager.getPlayerName(false));
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
        switch (engine.getConfig().interaction) {
            case PRIVATE_ANSWERS:
                if (awaitingPlayerB) showPlayerBResponse();
                else showAnswerDialog(false);
                break;
            case PRIVATE_CHOICES:
                if (awaitingPlayerB) showPlayerBResponse();
                else showChoiceDialog(false);
                break;
            case TIMED_PRIVATE_ANSWERS:
                if (awaitingPlayerB) showPlayerBResponse();
                else showAnswerDialog(false);
                break;
            case WISH_LIST:
                showAddWishDialog();
                break;
            case SHARED_COUNTDOWN:
                onSharedCountdownClicked(v);
                break;
            case TIMED_TASK:
                if ("forfeit_wheel".equals(modeId) && !forfeitRevealed) {
                    spinForfeitWheel(v);
                } else {
                    completeCurrent(v);
                }
                break;
            case MISSION:
                completeCurrent(v);
                break;
        }
    }

    private void showPlayerBResponse() {
        SocialGamesEngine.Interaction interaction = engine.getConfig().interaction;
        if (interaction == SocialGamesEngine.Interaction.PRIVATE_CHOICES) {
            showChoiceDialog(true);
        } else {
            if (interaction == SocialGamesEngine.Interaction.TIMED_PRIVATE_ANSWERS) {
                startTimer(engine.getDurationSec());
            }
            showAnswerDialog(true);
        }
    }

    private void showAnswerDialog(boolean playerB) {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.social_answer_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(2);
        input.setMaxLines(5);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        applyDialogInputPadding(input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(playerB ? R.string.social_turn_b : R.string.social_turn_a)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.social_save_answer, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String answer = input.getText().toString().trim();
                    if (answer.isEmpty()) {
                        input.setError(getString(R.string.social_answer_required));
                        return;
                    }
                    dialog.dismiss();
                    savePrivateAnswer(playerB, answer, false);
                }));
        dialog.show();
    }

    private void showChoiceDialog(boolean playerB) {
        int optionsRes = "love_language".equals(modeId)
                ? R.array.social_love_language_options : R.array.social_most_likely_options;
        String[] options = getResources().getStringArray(optionsRes);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(playerB ? R.string.social_turn_b : R.string.social_turn_a)
                .setItems(options, (dialog, which) -> savePrivateAnswer(playerB, options[which], true))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void savePrivateAnswer(boolean playerB, String answer, boolean compareAnswers) {
        if (engine.isTimed()) dareTimer.cancel();
        if (playerB) {
            answerB = answer;
            showPairedReveal(compareAnswers);
            return;
        }
        answerA = answer;
        awaitingPlayerB = true;
        String playerBName = container.profileManager.getPlayerName(false);
        PassPhoneDialog.newInstance(REQ_SOCIAL_HANDOFF, playerBName)
                .show(getChildFragmentManager(), "social_game_handoff");
    }

    private void showPairedReveal(boolean compareAnswers) {
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);
        String result = getString(R.string.social_revealed_answers, nameA, answerA, nameB, answerB);
        if (compareAnswers) {
            result = getString(answerA.equals(answerB) ? R.string.social_match : R.string.social_miss)
                    + "\n\n" + result;
        }
        if (currentRiddleAnswer != null) {
            result += "\n\n" + getString(R.string.social_riddle_solution, currentRiddleAnswer);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.social_reveal)
                .setMessage(result)
                .setCancelable(false)
                .setPositiveButton(R.string.social_next, (dialog, which) -> {
                    if (binding != null) completeCurrent(binding.btnSocialPrimary);
                })
                .show();
    }

    private void showAddWishDialog() {
        EditText input = new EditText(requireContext());
        input.setText(currentPrompt);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(2);
        input.setMaxLines(5);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        applyDialogInputPadding(input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.social_add_wish_title)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.social_save_wish, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String wish = input.getText().toString().trim();
                    if (wish.isEmpty()) {
                        input.setError(getString(R.string.social_wish_required));
                        return;
                    }
                    if (wish.length() > SocialWishListStore.MAX_WISH_LENGTH) {
                        input.setError(getString(R.string.social_wish_too_long,
                                SocialWishListStore.MAX_WISH_LENGTH));
                        return;
                    }
                    SocialWishListStore.AddResult result = wishListStore.addWish(wish);
                    dialog.dismiss();
                    int messageRes = result == SocialWishListStore.AddResult.ADDED
                            ? R.string.social_wish_saved
                            : result == SocialWishListStore.AddResult.DUPLICATE
                            ? R.string.social_wish_exists : R.string.social_wishlist_full;
                    showToast(messageRes);
                    if (binding != null) completeCurrent(binding.btnSocialPrimary);
                }));
        dialog.show();
    }

    private void showWishlist() {
        List<String> wishes = wishListStore.getWishes();
        if (wishes.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.social_wishlist_title)
                    .setMessage(R.string.social_wishlist_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        String[] items = wishes.toArray(new String[0]);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.social_wishlist_title)
                .setItems(items, (dialog, which) -> new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(getString(R.string.social_remove_wish_prompt, items[which]))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.social_remove_wish,
                                (confirmDialog, button) -> wishListStore.removeWish(items[which]))
                        .show())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void onSharedCountdownClicked(View v) {
        if (sharedCountdownFinished) {
            completeCurrent(v);
            return;
        }
        if (sharedCountdownRunning) {
            dareTimer.cancel();
            sharedCountdownRunning = false;
            completeCurrent(v);
            return;
        }

        sharedCountdownRunning = true;
        int seconds = engine.getDurationSec();
        binding.btnSocialPrimary.setText(R.string.social_sync_stop);
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
                    sharedCountdownRunning = false;
                    sharedCountdownFinished = true;
                    binding.tvSocialTimer.setText(R.string.social_timeup);
                    binding.btnSocialPrimary.setText(R.string.social_next);
                    HapticHelper.countdownFinished(binding.getRoot());
                });
            }
        });
    }

    private void spinForfeitWheel(View v) {
        if (forfeitAnimator != null || binding == null) return;
        HapticHelper.tick(v);
        forfeitSpinCancelled = false;
        forfeitAnimator = ObjectAnimator.ofFloat(binding.cardSocialPrompt, View.ROTATION, 0f, 1440f);
        forfeitAnimator.setDuration(1100L);
        forfeitAnimator.setInterpolator(new DecelerateInterpolator());
        forfeitAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (forfeitSpinCancelled) {
                    forfeitSpinCancelled = false;
                    return;
                }
                if (binding == null || !isAdded()) return;
                binding.cardSocialPrompt.setRotation(0f);
                binding.tvSocialPrompt.setAlpha(1f);
                binding.tvSocialPrompt.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                binding.btnSocialPrimary.setText(R.string.social_done);
                forfeitRevealed = true;
                forfeitAnimator = null;
                HapticHelper.confirm(binding.getRoot());
                startTimer(engine.getDurationSec());
            }
        });
        forfeitAnimator.start();
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

    private void showToast(int messageRes) {
        if (isAdded()) {
            android.widget.Toast.makeText(requireContext(), messageRes,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void applyDialogInputPadding(EditText input) {
        float density = getResources().getDisplayMetrics().density;
        int horizontal = (int) (20 * density + 0.5f);
        int vertical = (int) (12 * density + 0.5f);
        input.setPadding(horizontal, vertical, horizontal, vertical);
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
        if (forfeitAnimator != null) {
            forfeitSpinCancelled = true;
            forfeitAnimator.cancel();
            forfeitAnimator = null;
        }
        // Flush any partially completed round so early exits are still recorded.
        if (container != null) writeHistory();
        binding = null;
        super.onDestroyView();
    }
}
