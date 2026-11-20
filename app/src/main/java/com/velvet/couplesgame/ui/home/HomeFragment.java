package com.velvet.couplesgame.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.databinding.FragmentHomeBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.onboarding.OnboardingDialogFragment;
import com.velvet.couplesgame.ui.paywall.PaywallDialogFragment;

public class HomeFragment extends Fragment {
    private static final String REQ_PAYWALL_HOME = "req_paywall_home";
    private static final String ACTION_THEME_PREFIX = "theme:";
    private static final String ACTION_TIER_PREFIX = "tier:";

    private FragmentHomeBinding binding;
    private AppContainer container;
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private ThemeSwatchAdapter themeAdapter;
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

        setupPaywallResultListener();
        setupThemePicker();
        setupIntensitySelector();
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
            updateIntensityButtonsUi();
        });

        binding.btnUnlockVipBanner.setOnClickListener(v -> {
            HapticHelper.tick(v);
            openPaywall(null);
        });

        // 5 Featured Game Mode Cards
        binding.cardModeChess.setOnClickListener(v -> navigateMode(v, R.id.action_home_to_couple_board));
        binding.cardModeBlindMatch.setOnClickListener(v -> navigateMode(v, R.id.action_home_to_blind_match));
        binding.cardModeWheel.setOnClickListener(v -> navigateMode(v, R.id.action_home_to_lucky_wheel));
        binding.cardModeMinesweeper.setOnClickListener(v -> navigateMode(v, R.id.action_home_to_couple_mines));
        binding.cardModeDrawGuess.setOnClickListener(v -> navigateMode(v, R.id.action_home_to_draw_guess));

        // Quick Tonight Draw Deck: Draw advances turn; Skip keeps same partner and decrements their quota
        binding.btnDrawCard.setOnClickListener(v -> {
            HapticHelper.tick(v);
            drawNextCard(currentCard != null);
        });

        binding.btnSkipCard.setOnClickListener(v -> {
            if (container.taskEngine.consumeSkip(isPlayerATurn)) {
                HapticHelper.tick(v);
                updateSkipButtonLabel();
                drawNextCard(false);
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
                            HapticHelper.confirm(binding.getRoot());
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
            }
        });
    }

    private void navigateMode(View v, int actionId) {
        HapticHelper.tick(v);
        Navigation.findNavController(v).navigate(actionId);
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
        if (!container.entitlementManager.canAccessTier(tier)) {
            openPaywall(ACTION_TIER_PREFIX + tier);
            return;
        }
        container.entitlementManager.setSelectedMaxTier(tier);
        updateIntensityButtonsUi();
    }

    private void updateIntensityButtonsUi() {
        if (binding == null) return;
        int current = container.entitlementManager.getSelectedMaxTier();
        boolean vip = container.entitlementManager.isPremiumUnlocked();

        binding.btnTier1.setAlpha(current == 1 ? 1.0f : 0.55f);
        binding.btnTier2.setAlpha(current == 2 ? 1.0f : 0.55f);
        binding.btnTier3.setAlpha(current == 3 ? 1.0f : 0.55f);
        binding.btnTier4.setAlpha(current == 4 ? 1.0f : 0.55f);

        binding.btnTier1.setText(R.string.tier_short_1);
        binding.btnTier2.setText(R.string.tier_short_2);
        binding.btnTier3.setText(vip ? getString(R.string.tier_short_3) : "🔒 " + getString(R.string.tier_short_3));
        binding.btnTier4.setText(vip ? getString(R.string.tier_short_4) : "🔒 " + getString(R.string.tier_short_4));
    }

    private void openPaywall(@Nullable String actionTag) {
        PaywallDialogFragment.newInstance(REQ_PAYWALL_HOME, actionTag)
                .show(getChildFragmentManager(), "velvet_paywall");
    }

    private void drawNextCard(boolean advanceTurn) {
        dareTimer.cancel();
        boolean enableProcedural = binding.switchProcedural.isChecked();
        int maxTier = container.entitlementManager.getSelectedMaxTier();

        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard next = container.taskEngine.drawNext(allCards, mutualMatches, maxTier, null, enableProcedural);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (next == null) {
                    Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (advanceTurn) {
                    isPlayerATurn = !isPlayerATurn;
                }
                updateSkipButtonLabel();
                PhysicsAnimator.flipCard3D(binding.cardPreviewContainer, () -> {
                    if (binding == null || !isAdded()) return;
                    currentCard = next;
                    renderCardMeta(next);
                    binding.tvCardText.setText(next.text);
                    binding.btnFavorite.setText(R.string.btn_favorite);
                    binding.tvTimerStatus.setVisibility(View.GONE);

                    container.contentRepository.isFavoriteAsync(next.id, isSaved -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (binding != null && currentCard != null && currentCard.id.equals(next.id)) {
                                binding.btnFavorite.setText(isSaved ? R.string.btn_favorited : R.string.btn_favorite);
                            }
                        });
                    });

                    if (next.durationSec > 0) {
                        binding.btnStartTimer.setVisibility(View.VISIBLE);
                        binding.btnStartTimer.setText(getString(R.string.btn_start_timer, next.durationSec));
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
    }

    private void updateSkipButtonLabel() {
        int left = container.taskEngine.getSkipsLeft(isPlayerATurn);
        binding.btnSkipCard.setText(getString(R.string.btn_skip_card, left));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        binding = null;
    }
}
