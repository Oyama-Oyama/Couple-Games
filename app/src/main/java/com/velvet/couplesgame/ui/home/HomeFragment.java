package com.velvet.couplesgame.ui.home;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.dateplanner.DatePlanManager;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.profile.TogetherDateFormat;
import com.velvet.couplesgame.core.profile.IntimacyPreferenceManager;
import com.velvet.couplesgame.core.streak.BadgeCatalog;
import com.velvet.couplesgame.core.streak.StreakCalculator;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import com.velvet.couplesgame.databinding.DialogMoodCheckinBinding;
import com.velvet.couplesgame.databinding.FragmentHomeBinding;
import com.velvet.couplesgame.databinding.ItemGameModeCardBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.journal.TonightNoteDialogFragment;
import com.velvet.couplesgame.ui.onboarding.OnboardingDialogFragment;
import com.velvet.couplesgame.ui.paywall.PaywallDialogFragment;
import com.velvet.couplesgame.ui.reward.RewardVoucherWalletDialogFragment;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String REQ_PAYWALL_HOME = "req_paywall_home";
    private static final String ACTION_THEME_PREFIX = "theme:";
    private static final String ACTION_TIER_PREFIX = "tier:";
    private static final String ACTION_MODE_PREFIX = "mode:";
    private static final String STATE_MODES_EXPANDED = "home_modes_expanded";
    private static final String STATE_ACTIVE_SORT = "home_active_sort";
    private static final int COLLAPSED_MODE_COUNT = 4;

    private FragmentHomeBinding binding;
    private AppContainer container;
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private ThemeSwatchAdapter themeAdapter;
    private GameModeCatalog.SortFilter activeSort = GameModeCatalog.SortFilter.HOTTEST_FIRST;
    private boolean modesExpanded = false;
    /** Latest session-stat snapshot, used to tell whether tonight's Date Night is done. */
    private List<SessionStatEntity> sessionStats = new java.util.ArrayList<>();
    private TaskCard currentCard;
    private boolean isPlayerATurn = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        if (savedInstanceState != null) {
            modesExpanded = savedInstanceState.getBoolean(STATE_MODES_EXPANDED, false);
            int savedSortIdx = savedInstanceState.getInt(STATE_ACTIVE_SORT, 0);
            GameModeCatalog.SortFilter[] allSorts = GameModeCatalog.SortFilter.values();
            if (savedSortIdx >= 0 && savedSortIdx < allSorts.length) {
                activeSort = allSorts[savedSortIdx];
            }
        }

        setupPaywallResultListener();
        setupThemePicker();
        setupIntensitySelector();
        setupGameModesCatalog();
        updateSkipButtonLabel();

        container.profileManager.observeState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null) return;
            binding.tvCouplePill.setText(getString(
                    R.string.home_couple_pill,
                    state.partnerA.name,
                    state.partnerB.name
            ));
            if (currentCard != null) {
                renderCardMeta(currentCard);
            }
        });

        binding.tvCouplePill.setOnClickListener(v -> {
            HapticHelper.tick(v);
            OnboardingDialogFragment.newInstance(false)
                    .show(getChildFragmentManager(), "edit_couple_profile");
        });

        container.entitlementManager.observePremiumStatus().observe(getViewLifecycleOwner(), isVip -> {
            if (binding == null) return;
            boolean unlocked = Boolean.TRUE.equals(isVip);
            binding.btnUnlockVipBanner.setVisibility(unlocked ? View.GONE : View.VISIBLE);
            if (themeAdapter != null) themeAdapter.setPremiumUnlocked(unlocked);
            renderGameModesList();
            updateIntensityButtonsUi();
        });

        binding.btnUnlockVipBanner.setOnClickListener(v -> {
            HapticHelper.tick(v);
            openPaywall(null);
        });

        binding.cardRateUs.setOnClickListener(v -> openStoreRating(v));
        binding.btnRateUs.setOnClickListener(v -> openStoreRating(v));

        // New relationship features: daily reveal, mood check-in, milestones
        binding.btnDailyReveal.setOnClickListener(v -> {
            HapticHelper.tick(v);
            Navigation.findNavController(v).navigate(R.id.action_home_to_daily_reveal);
        });
        binding.btnMoodCheckin.setOnClickListener(v -> openMoodCheckin());
        container.moodCheckinManager.observeCheckedInToday().observe(getViewLifecycleOwner(), checked ->
                updateMoodStatus(Boolean.TRUE.equals(checked)));
        container.moodCheckinManager.observeStreak().observe(getViewLifecycleOwner(), ignored -> updateMilestones());
        container.contentRepository.observeSessionStats().observe(getViewLifecycleOwner(), ignored -> updateMilestones());
        container.profileManager.observeState().observe(getViewLifecycleOwner(), ignored -> updateMilestones());
        container.datePlanManager.observePlans().observe(getViewLifecycleOwner(), ignored -> updateDatePlan());
        updateMilestones();

        binding.btnDatePlanOpen.setOnClickListener(v -> {
            HapticHelper.tick(v);
            Navigation.findNavController(v).navigate(R.id.action_home_to_date_planner);
        });
        updateDatePlan();

        binding.btnLetterOpen.setOnClickListener(v -> {
            HapticHelper.tick(v);
            Navigation.findNavController(v).navigate(R.id.action_home_to_sealed_letter);
        });
        binding.btnLoveGraphOpen.setOnClickListener(v -> {
            HapticHelper.tick(v);
            Navigation.findNavController(v).navigate(R.id.action_home_to_love_graph);
        });
        container.letterManager.observeLetters().observe(getViewLifecycleOwner(), ignored -> updateLetterStatus());
        updateLetterStatus();

        // A item - guided Date Night session entry (new navigation node, not a new game mode)
        binding.btnDateNightOpen.setOnClickListener(v -> {
            HapticHelper.tick(v);
            Navigation.findNavController(v).navigate(R.id.action_home_to_date_night);
        });
        binding.btnRewardWallet.setOnClickListener(v -> {
            HapticHelper.tick(v);
            RewardVoucherWalletDialogFragment.newInstance()
                    .show(getChildFragmentManager(), RewardVoucherWalletDialogFragment.TAG);
        });
        binding.btnIntimacyPreferences.setOnClickListener(v -> {
            HapticHelper.tick(v);
            showIntimacyPreferenceDialog();
        });
        container.contentRepository.observeSessionStats().observe(getViewLifecycleOwner(), stats -> {
            sessionStats = stats != null ? stats : new java.util.ArrayList<>();
            updateDateNightCard();
        });
        updateDateNightCard();

        // C item - tonight's one line (same-day overwrite, no export / share)
        binding.btnTonightNoteOpen.setOnClickListener(v -> {
            HapticHelper.tick(v);
            if (container.tonightNoteManager.hasNoteToday()) {
                // Tonight already has a line -> open the read-back list
                Navigation.findNavController(v).navigate(R.id.action_home_to_tonight_note_history);
            } else {
                TonightNoteDialogFragment.newInstance()
                        .show(getChildFragmentManager(), TonightNoteDialogFragment.TAG);
            }
        });
        container.tonightNoteManager.observeNotes().observe(getViewLifecycleOwner(), ignored -> updateTonightNoteCard());
        updateTonightNoteCard();

        // Quick Tonight Draw Deck + direct Velvet Spark button
        binding.btnDrawCard.setOnClickListener(v -> {
            HapticHelper.tick(v);
            drawNextCard(currentCard != null, false);
        });

        binding.btnAiSparkNow.setOnClickListener(v -> {
            HapticHelper.tick(v);
            drawNextCard(currentCard != null, true);
        });

        binding.btnSkipCard.setOnClickListener(v -> {
            if (container.taskEngine.consumeSkip(isPlayerATurn)) {
                HapticHelper.tick(v);
                updateSkipButtonLabel();
                drawNextCard(false, false);
            } else {
                Toast.makeText(requireContext(), R.string.msg_no_skips, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnStartTimer.setOnClickListener(v -> {
            if (currentCard != null && currentCard.durationSec > 0) {
                binding.tvTimerStatus.setVisibility(View.VISIBLE);
                dareTimer.start(currentCard.durationSec, new DareTimerHelper.Listener() {
                    @Override
                    public void onTick(int secondsRemaining) {
                        if (binding != null && isAdded()) {
                            binding.tvTimerStatus.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                        }
                    }

                    @Override
                    public void onFinish() {
                        if (binding != null && isAdded()) {
                            HapticHelper.countdownFinished(binding.getRoot());
                            binding.tvTimerStatus.setText(R.string.timer_done);
                        }
                    }
                });
            }
        });

        binding.btnFavorite.setOnClickListener(v -> {
            if (currentCard == null) return;
            container.contentRepository.toggleFavorite(currentCard, isSaved -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.btnFavorite.setText(isSaved ? R.string.btn_favorited : R.string.btn_favorite);
                    }
                });
            });
        });
    }

    private void setupGameModesCatalog() {
        binding.btnSortHottest.setOnClickListener(v -> {
            HapticHelper.tick(v);
            applySortFilter(GameModeCatalog.SortFilter.HOTTEST_FIRST);
        });
        binding.btnSortGentle.setOnClickListener(v -> {
            HapticHelper.tick(v);
            applySortFilter(GameModeCatalog.SortFilter.GENTLE_TO_WILD);
        });
        binding.btnSortFree.setOnClickListener(v -> {
            HapticHelper.tick(v);
            applySortFilter(GameModeCatalog.SortFilter.FREE_ONLY);
        });
        binding.btnSortVip.setOnClickListener(v -> {
            HapticHelper.tick(v);
            applySortFilter(GameModeCatalog.SortFilter.VIP_ONLY);
        });

        binding.tvGameModesToggle.setOnClickListener(v -> toggleModesExpanded(false));
        binding.btnToggleModesExpand.setOnClickListener(v -> toggleModesExpanded(true));
        updateSortFilterButtons();
        renderGameModesList();
    }

    private void toggleModesExpanded(boolean scrollToHeaderOnCollapse) {
        if (binding == null) return;
        HapticHelper.tick(binding.getRoot());
        boolean wasExpanded = modesExpanded;
        modesExpanded = !modesExpanded;
        renderGameModesList();
        if (wasExpanded && !modesExpanded && scrollToHeaderOnCollapse) {
            binding.scrollHome.post(() -> {
                if (binding != null) {
                    binding.scrollHome.smoothScrollTo(0, Math.max(0, binding.layoutGameModesHeader.getTop() - 16));
                }
            });
        }
    }

    private void applySortFilter(GameModeCatalog.SortFilter filter) {
        activeSort = filter;
        updateSortFilterButtons();
        renderGameModesList();
    }

    private void updateSortFilterButtons() {
        binding.btnSortHottest.setAlpha(activeSort == GameModeCatalog.SortFilter.HOTTEST_FIRST ? 1.0f : 0.52f);
        binding.btnSortGentle.setAlpha(activeSort == GameModeCatalog.SortFilter.GENTLE_TO_WILD ? 1.0f : 0.52f);
        binding.btnSortFree.setAlpha(activeSort == GameModeCatalog.SortFilter.FREE_ONLY ? 1.0f : 0.52f);
        binding.btnSortVip.setAlpha(activeSort == GameModeCatalog.SortFilter.VIP_ONLY ? 1.0f : 0.52f);
    }

    private void renderGameModesList() {
        if (binding == null || !isAdded()) return;
        List<GameModeCatalog> allMatching = GameModeCatalog.queryModes(activeSort);
        // B item soft gate: after declining the 18+ confirmation, adult-oriented modes
        // (heatRating >= 3) are hidden in the UI only - GameModeCatalog stays at 36.
        boolean adultContentAllowed = container.ageGateManager.isAdultContentAllowed();
        if (!adultContentAllowed) {
            List<GameModeCatalog> filtered = new java.util.ArrayList<>();
            for (GameModeCatalog mode : allMatching) {
                if (mode.heatRating <= 2) filtered.add(mode);
            }
            allMatching = filtered;
        }
        List<GameModeCatalog> visibleModes = GameModeCatalog.sliceForDisplay(
                allMatching, modesExpanded, COLLAPSED_MODE_COUNT
        );
        boolean unlocked = container.entitlementManager.isPremiumUnlocked();
        boolean canCollapse = allMatching.size() > COLLAPSED_MODE_COUNT;
        int hiddenCount = Math.max(0, allMatching.size() - visibleModes.size());
        boolean noModes = allMatching.isEmpty();
        binding.tvGameModesToggle.setVisibility(noModes ? View.GONE : View.VISIBLE);

        if (noModes) {
            binding.btnToggleModesExpand.setVisibility(View.GONE);
        } else if (!canCollapse) {
            binding.tvGameModesToggle.setText(allMatching.size() + " / " + GameModeCatalog.values().length);
            binding.tvGameModesToggle.setClickable(false);
            binding.btnToggleModesExpand.setVisibility(View.GONE);
        } else if (modesExpanded) {
            binding.tvGameModesToggle.setText(getString(
                    R.string.modes_pill_collapse_fmt, visibleModes.size(), allMatching.size()
            ));
            binding.tvGameModesToggle.setClickable(true);
            binding.btnToggleModesExpand.setVisibility(View.VISIBLE);
            binding.btnToggleModesExpand.setText(R.string.btn_modes_collapse);
        } else {
            binding.tvGameModesToggle.setText(getString(
                    R.string.modes_pill_expand_fmt, visibleModes.size(), allMatching.size()
            ));
            binding.tvGameModesToggle.setClickable(true);
            binding.btnToggleModesExpand.setVisibility(View.VISIBLE);
            binding.btnToggleModesExpand.setText(getString(R.string.btn_modes_expand_fmt, hiddenCount));
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        binding.layoutGameModesList.removeAllViews();
        for (GameModeCatalog mode : visibleModes) {
            ItemGameModeCardBinding cardBinding = ItemGameModeCardBinding.inflate(
                    inflater, binding.layoutGameModesList, false
            );
            GameModeAdapter.bindCard(cardBinding, mode, unlocked, selected -> {
                HapticHelper.tick(binding.layoutGameModesList);
                if (!container.entitlementManager.canAccessGameMode(selected)) {
                    openPaywall(ACTION_MODE_PREFIX + selected.id);
                    return;
                }
                Navigation.findNavController(requireView()).navigate(selected.navActionId);
            });
            binding.layoutGameModesList.addView(cardBinding.getRoot());
        }
    }

    private void showIntimacyPreferenceDialog() {
        IntimacyPreferenceManager manager = container.intimacyPreferenceManager;
        java.util.Set<IntimacyPreferenceManager.Style> selected = manager.getSelectedStyles();
        IntimacyPreferenceManager.Style[] styles = IntimacyPreferenceManager.Style.values();
        int[] labelResources = {
                R.string.intimacy_style_romantic,
                R.string.intimacy_style_playful,
                R.string.intimacy_style_sensory,
                R.string.intimacy_style_imaginative
        };
        String[] labels = new String[styles.length];
        boolean[] checked = new boolean[styles.length];
        for (int i = 0; i < styles.length; i++) {
            labels[i] = getString(labelResources[i]);
            checked[i] = selected.contains(styles[i]);
        }

        int padding = Math.round(16f * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout content = new android.widget.LinearLayout(requireContext());
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, 0);

        android.widget.TextView description = new android.widget.TextView(requireContext());
        description.setText(R.string.intimacy_preferences_body);
        description.setTextColor(resolveThemeColor(R.attr.velvetTextMuted));
        description.setTextSize(14f);
        description.setPadding(padding, padding, padding, padding);
        description.setBackgroundResource(R.drawable.bg_card_velvet);
        content.addView(description, new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        MaterialCheckBox[] checkBoxes = new MaterialCheckBox[styles.length];
        for (int i = 0; i < styles.length; i++) {
            MaterialCheckBox checkBox = new MaterialCheckBox(requireContext());
            checkBox.setText(labels[i]);
            checkBox.setTextColor(resolveThemeColor(R.attr.velvetTextPrimary));
            checkBox.setChecked(checked[i]);
            checkBoxes[i] = checkBox;
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = Math.round(4f * getResources().getDisplayMetrics().density);
            content.addView(checkBox, params);
        }

        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        scrollView.addView(content);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.intimacy_preferences_title)
                .setView(scrollView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.intimacy_preferences_save, (dialog, which) -> {
                    java.util.EnumSet<IntimacyPreferenceManager.Style> chosen =
                            java.util.EnumSet.noneOf(IntimacyPreferenceManager.Style.class);
                    for (int i = 0; i < styles.length; i++) {
                        if (checkBoxes[i].isChecked()) chosen.add(styles[i]);
                    }
                    if (!manager.saveSelectedStyles(chosen)) {
                        Toast.makeText(requireContext(), R.string.intimacy_preferences_save_failed,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    renderGameModesList();
                    Toast.makeText(requireContext(), R.string.intimacy_preferences_saved,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private int resolveThemeColor(int attrId) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (requireContext().getTheme().resolveAttribute(attrId, value, true)) {
            if (value.resourceId != 0) {
                return androidx.core.content.ContextCompat.getColor(requireContext(), value.resourceId);
            }
            return value.data;
        }
        return 0xFFFFFFFF;
    }

    /** Mood check-in dialog: pick a mood and write one gratitude note per partner. */
    private void openMoodCheckin() {
        if (binding == null || !isAdded()) return;
        DialogMoodCheckinBinding dialogBinding =
                DialogMoodCheckinBinding.inflate(LayoutInflater.from(requireContext()));
        int today = container.moodCheckinManager.getTodayMood();
        if (today >= 1 && today <= 5) {
            checkMoodRadio(dialogBinding.rgMood, today);
        }
        dialogBinding.etGratitudeA.setText(container.moodCheckinManager.getTodayGratitude(true));
        dialogBinding.etGratitudeB.setText(container.moodCheckinManager.getTodayGratitude(false));

        // Speak to each partner by name so the note feels personal, not generic.
        dialogBinding.tvGratitudeLabelA.setText(getString(R.string.mood_gratitude_a_named,
                container.profileManager.getPlayerName(false)));
        dialogBinding.tvGratitudeLabelB.setText(getString(R.string.mood_gratitude_b_named,
                container.profileManager.getPlayerName(true)));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_mood_checkin_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.home_mood_save, (d, which) -> {
                    int mood = selectedMood(dialogBinding.rgMood);
                    if (mood < 1) {
                        Toast.makeText(requireContext(), R.string.mood_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    container.moodCheckinManager.checkIn(mood,
                            textOf(dialogBinding.etGratitudeA),
                            textOf(dialogBinding.etGratitudeB));
                    HapticHelper.confirm(binding.getRoot());
                    Toast.makeText(requireContext(), R.string.msg_mood_saved,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private static void checkMoodRadio(RadioGroup group, int mood) {
        int[] ids = {R.id.rb_mood_1, R.id.rb_mood_2, R.id.rb_mood_3, R.id.rb_mood_4, R.id.rb_mood_5};
        if (mood >= 1 && mood <= ids.length) group.check(ids[mood - 1]);
    }

    private static int selectedMood(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == R.id.rb_mood_1) return 1;
        if (id == R.id.rb_mood_2) return 2;
        if (id == R.id.rb_mood_3) return 3;
        if (id == R.id.rb_mood_4) return 4;
        if (id == R.id.rb_mood_5) return 5;
        return -1;
    }

    private static String textOf(EditText field) {
        return field.getText() != null ? field.getText().toString() : "";
    }

    private void updateMoodStatus(boolean checkedInToday) {
        if (binding == null) return;
        binding.tvMoodStatus.setText(checkedInToday
                ? R.string.mood_done_today : R.string.mood_pending_today);
        binding.btnMoodCheckin.setText(checkedInToday
                ? R.string.mood_edit_today : R.string.home_mood_btn);
    }

    /** Refresh streak, unlocked badges and the days-together line. */
    private void updateMilestones() {
        if (binding == null || container == null) return;
        Integer moodStreak = container.moodCheckinManager.observeStreak().getValue();
        int moodDays = moodStreak != null ? moodStreak : 0;
        binding.tvStreak.setText(getString(R.string.home_streak_fmt, moodDays));

        List<SessionStatEntity> history = container.contentRepository.observeSessionStats().getValue();
        StreakCalculator.Result playStats = StreakCalculator.compute(history);
        int totalSessions = history != null ? history.size() : 0;
        StringBuilder badges = new StringBuilder();
        int unlocked = 0;
        for (BadgeCatalog.Badge badge : BadgeCatalog.all()) {
            if (badge.isUnlocked(playStats, totalSessions)) {
                if (badges.length() > 0) badges.append("  ");
                badges.append(getString(badge.labelRes));
                unlocked++;
            }
        }
        binding.tvBadges.setText(getString(R.string.home_badges_fmt,
                unlocked, BadgeCatalog.all().size(), badges.toString()));

        int days = container.profileManager.getDaysTogether();
        if (days >= 0) {
            int anniversary = container.profileManager.getAnniversaryCountdown();
            binding.tvTogether.setText(anniversary == 0
                    ? getString(R.string.home_together_anniversary_today, days)
                    : getString(R.string.home_together_fmt, days, anniversary));
            String since = TogetherDateFormat.display(
                    container.profileManager.getTogetherSince(), currentLocale());
            binding.tvTogetherDate.setVisibility(View.VISIBLE);
            binding.tvTogetherDate.setText(since != null
                    ? getString(R.string.home_together_since_fmt, since)
                    : "");
        } else {
            binding.tvTogether.setText(R.string.home_together_unset);
            binding.tvTogetherDate.setVisibility(View.GONE);
        }
    }

    /** The locale the app is currently running in, used to localise the date. */
    private Locale currentLocale() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        return (!locales.isEmpty()) ? locales.get(0) : Locale.getDefault();
    }

    /** Show the sealed-letter mailbox summary: how many are ready, how many wait. */
    private void updateLetterStatus() {
        if (binding == null || container == null) return;
        int ready = container.letterManager.getReadyLetters().size();
        int sealed = container.letterManager.getSealedLetters().size();
        if (ready == 0 && sealed == 0) {
            binding.tvLetterHome.setText(R.string.home_letter_empty);
            binding.tvLetterHomeSub.setVisibility(View.GONE);
            return;
        }
        binding.tvLetterHomeSub.setVisibility(View.VISIBLE);
        binding.tvLetterHome.setText(ready > 0
                ? getString(R.string.home_letter_ready_fmt, ready)
                : getString(R.string.home_letter_locked_fmt, sealed));
        binding.tvLetterHomeSub.setText(ready > 0
                ? getString(R.string.home_letter_locked_fmt, sealed)
                : getString(R.string.letter_section_sealed));
    }

    /** A item: the Date Night button reflects whether tonight's session is already done. */
    private void updateDateNightCard() {
        if (binding == null || container == null) return;
        boolean doneToday = false;
        List<SessionStatEntity> stats = sessionStats;
        long startOfToday = java.util.Calendar.getInstance().getTimeInMillis()
                - (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) * 3600_000L)
                - (java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) * 60_000L)
                - (java.util.Calendar.getInstance().get(java.util.Calendar.SECOND) * 1000L);
        for (SessionStatEntity stat : stats) {
            if (stat != null && "DATE_NIGHT".equals(stat.gameMode)
                    && stat.playerACompleted > 0 && stat.playedAt >= startOfToday) {
                doneToday = true;
                break;
            }
        }
        // 按钮始终保持短动作标签；「今晚已完成」由副标题承载（datenight_finish 是整句状态语，
        // 不适合放在按钮上）。
        binding.btnDateNightOpen.setText(R.string.datenight_enter);
        binding.tvDateNightSub.setText(doneToday
                ? R.string.datenight_finish
                : R.string.datenight_subtitle);
    }

    /** C item: show whether tonight already has a line, or when the last one was. */
    private void updateTonightNoteCard() {
        if (binding == null || container == null) return;
        String today = container.tonightNoteManager.getTodayIsoDate();
        boolean writtenToday = container.tonightNoteManager.hasNoteToday();
        binding.tvTonightNoteSub.setText(writtenToday
                ? R.string.tonight_note_saved_toast
                : R.string.tonight_note_empty_today);
        binding.btnTonightNoteOpen.setText(writtenToday
                ? R.string.tonight_note_history_title
                : R.string.tonight_note_save);
    }

    /** Show the nearest upcoming date plan, or a prompt to create one. */
    private void updateDatePlan() {        if (binding == null || container == null) return;
        DatePlanManager.Plan plan = container.datePlanManager.getUpcomingPlan();
        if (plan == null) {
            binding.tvDatePlan.setText(R.string.home_date_plan_empty);
            binding.tvDatePlanSub.setVisibility(View.GONE);
            return;
        }
        binding.tvDatePlanSub.setVisibility(View.VISIBLE);
        long days = plan.daysUntilToday();
        String when;
        if (days == 0) {
            when = getString(R.string.home_date_plan_today, plan.title);
        } else if (days == 1) {
            when = getString(R.string.home_date_plan_tomorrow, plan.title);
        } else {
            when = getString(R.string.home_date_plan_days, days, plan.title);
        }
        binding.tvDatePlan.setText(when);
        binding.tvDatePlanSub.setText(plan.dateIso + (plan.note == null || plan.note.isEmpty()
                ? "" : " · " + plan.note));
    }

    /** Open the Google Play listing so happy couples can leave a review. */
    private void openStoreRating(View v) {
        HapticHelper.tick(v);
        String packageName = requireContext().getPackageName();
        String webUrl = "https://play.google.com/store/apps/details?id=" + packageName;
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
        try {
            // Prefer the Play Store app; fall back to any browser if it is unavailable.
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            marketIntent.setPackage("com.android.vending");
            if (marketIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(marketIntent);
            } else {
                startActivity(webIntent);
            }
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(webIntent);
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(requireContext(), R.string.rate_us_unavailable,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupPaywallResultListener() {
        getChildFragmentManager().setFragmentResultListener(REQ_PAYWALL_HOME, getViewLifecycleOwner(), (key, bundle) -> {
            if (!bundle.getBoolean(PaywallDialogFragment.RESULT_UNLOCKED, false)) return;
            String actionTag = bundle.getString(PaywallDialogFragment.RESULT_ACTION_TAG);
            if (actionTag == null) return;

            if (actionTag.startsWith(ACTION_THEME_PREFIX)) {
                AppTheme theme = AppTheme.fromId(actionTag.substring(ACTION_THEME_PREFIX.length()));
                container.themeManager.selectTheme(requireActivity(), theme);
            } else if (actionTag.startsWith(ACTION_TIER_PREFIX)) {
                try {
                    int tier = Integer.parseInt(actionTag.substring(ACTION_TIER_PREFIX.length()));
                    container.entitlementManager.setSelectedMaxTier(tier);
                    updateIntensityButtonsUi();
                } catch (NumberFormatException ignored) {
                }
            } else if (actionTag.startsWith(ACTION_MODE_PREFIX)) {
                GameModeCatalog mode = GameModeCatalog.fromId(actionTag.substring(ACTION_MODE_PREFIX.length()));
                if (mode != null && getView() != null) {
                    Navigation.findNavController(getView()).navigate(mode.navActionId);
                }
            }
        });
    }

    private void setupThemePicker() {
        AppTheme current = container.themeManager.getCurrentTheme();
        boolean vip = container.entitlementManager.isPremiumUnlocked();
        themeAdapter = new ThemeSwatchAdapter(current, vip, selected -> {
            HapticHelper.tick(binding.rvThemes);
            if (!container.entitlementManager.canAccessTheme(selected)) {
                openPaywall(ACTION_THEME_PREFIX + selected.id);
                return;
            }
            container.themeManager.selectTheme(requireActivity(), selected);
        });
        binding.rvThemes.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.rvThemes.setAdapter(themeAdapter);
    }

    private void setupIntensitySelector() {
        binding.btnTier1.setOnClickListener(v -> selectTier(1));
        binding.btnTier2.setOnClickListener(v -> selectTier(2));
        binding.btnTier3.setOnClickListener(v -> selectTier(3));
        binding.btnTier4.setOnClickListener(v -> selectTier(4));
        updateIntensityButtonsUi();
    }

    private void selectTier(int tier) {
        HapticHelper.tick(binding.getRoot());
        if (!container.ageGateManager.isAdultContentAllowed()
                && tier > com.velvet.couplesgame.core.billing.FeatureGate.FREE_MAX_TIER) {
            Toast.makeText(requireContext(), R.string.agegate_adult_content_unavailable,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!container.entitlementManager.canAccessTier(tier)) {
            openPaywall(ACTION_TIER_PREFIX + tier);
            return;
        }
        container.entitlementManager.setSelectedMaxTier(tier);
        updateIntensityButtonsUi();
    }

    private void updateIntensityButtonsUi() {
        if (binding == null) return;
        int current = container.getEffectiveMaxTier();
        boolean vip = container.entitlementManager.isPremiumUnlocked();
        boolean adultContentAllowed = container.ageGateManager.isAdultContentAllowed();

        binding.btnTier1.setAlpha(current == 1 ? 1.0f : 0.55f);
        binding.btnTier2.setAlpha(current == 2 ? 1.0f : 0.55f);
        binding.btnTier3.setEnabled(adultContentAllowed);
        binding.btnTier4.setEnabled(adultContentAllowed);
        binding.btnTier3.setAlpha(adultContentAllowed && current == 3 ? 1.0f : 0.35f);
        binding.btnTier4.setAlpha(adultContentAllowed && current == 4 ? 1.0f : 0.35f);

        binding.btnTier1.setText(R.string.tier_short_1);
        binding.btnTier2.setText(R.string.tier_short_2);
        binding.btnTier3.setText(vip ? getString(R.string.tier_short_3) : "🔒 " + getString(R.string.tier_short_3));
        binding.btnTier4.setText(vip ? getString(R.string.tier_short_4) : "🔒 " + getString(R.string.tier_short_4));
        binding.tvHeatExplanation.setText(getString(
                R.string.heat_scope_fmt,
                getString(TagDisplayMapper.resolveTierNameRes(current)),
                getString(TagDisplayMapper.resolveTierDescriptionRes(current))
        ));
    }

    private void openPaywall(@Nullable String actionTag) {
        PaywallDialogFragment.newInstance(REQ_PAYWALL_HOME, actionTag)
                .show(getChildFragmentManager(), "velvet_paywall");
    }

    private void drawNextCard(boolean advanceTurn, boolean forceOfflineAi) {
        dareTimer.cancel();
        boolean enableProcedural = binding.switchProcedural.isChecked();
        int maxTier = container.getEffectiveMaxTier();

        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard next = null;
            if (forceOfflineAi) {
                next = container.proceduralGenerator.generate(maxTier, null, container.consentFilter);
            }
            if (next == null) {
                next = container.taskEngine.drawNext(allCards, mutualMatches, maxTier, null, enableProcedural);
            }
            TaskCard finalNext = next;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (finalNext == null) {
                    Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (advanceTurn) {
                    isPlayerATurn = !isPlayerATurn;
                }
                updateSkipButtonLabel();
                PhysicsAnimator.flipCard3D(binding.cardPreviewContainer, () -> {
                    if (binding == null || !isAdded()) return;
                    currentCard = finalNext;
                    renderCardMeta(finalNext);
                    binding.tvCardText.setText(finalNext.text);
                    binding.btnFavorite.setText(R.string.btn_favorite);
                    binding.tvTimerStatus.setVisibility(View.GONE);

                    container.contentRepository.isFavoriteAsync(finalNext.id, isSaved -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (binding != null && currentCard != null && currentCard.id.equals(finalNext.id)) {
                                binding.btnFavorite.setText(isSaved ? R.string.btn_favorited : R.string.btn_favorite);
                            }
                        });
                    });

                    if (finalNext.durationSec > 0) {
                        binding.btnStartTimer.setVisibility(View.VISIBLE);
                        binding.btnStartTimer.setText(getString(R.string.btn_start_timer, finalNext.durationSec));
                    } else {
                        binding.btnStartTimer.setVisibility(View.GONE);
                    }
                });
            });
        });
    }

    private void renderCardMeta(TaskCard card) {
        String actor = container.profileManager.getPlayerName(isPlayerATurn);
        String meta = TagDisplayMapper.formatCardMeta(
                requireContext(), card.tier, card.type, card.isProcedural, card.isCustom
        );
        binding.tvCardMeta.setText(actor + " · " + meta);
        binding.tvSoftLimitWarning.setVisibility(
                container.consentFilter.hasSoftLimit(card.tags) ? View.VISIBLE : View.GONE
        );
    }

    private void updateSkipButtonLabel() {
        int left = container.taskEngine.getSkipsLeft(isPlayerATurn);
        binding.btnSkipCard.setText(getString(R.string.btn_skip_card, left));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MODES_EXPANDED, modesExpanded);
        outState.putInt(STATE_ACTIVE_SORT, activeSort.ordinal());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        binding = null;
    }
}
