package com.velvet.couplesgame.ui.scratch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ScratchCardDto;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import com.velvet.couplesgame.databinding.FragmentScratchAdventureBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import java.util.ArrayList;
import java.util.List;

public class ScratchAdventureFragment extends Fragment {
    private FragmentScratchAdventureBinding binding;
    private AppContainer container;
    private final ScratchAdventureEngine engine = new ScratchAdventureEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private boolean cardRevealed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScratchAdventureBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.scratchOverlay.applyTheme(container.themeManager.getCurrentTheme());
        binding.scratchOverlay.setOnScratchListener(new ScratchCardView.OnScratchListener() {
            @Override
            public void onProgress(int percent) {
                if (binding != null && isAdded()) {
                    binding.tvScratchProgress.setText(getString(R.string.scratch_progress_fmt, percent));
                }
            }

            @Override
            public void onRevealed() {
                if (binding == null || !isAdded()) return;
                cardRevealed = true;
                binding.tvScratchProgress.setText(R.string.scratch_revealed_status);
                binding.btnScratchRevealNow.setVisibility(View.GONE);
                binding.layoutScratchActions.setVisibility(View.VISIBLE);
                PhysicsAnimator.popIn(binding.cardScratchUnderneath);
            }
        });

        binding.btnCatAll.setOnClickListener(v -> selectCategory(ScratchAdventureEngine.CAT_ALL));
        binding.btnCatDare.setOnClickListener(v -> selectCategory(ScratchAdventureEngine.CAT_DARE));
        binding.btnCatLocation.setOnClickListener(v -> selectCategory(ScratchAdventureEngine.CAT_LOCATION));
        binding.btnCatFantasy.setOnClickListener(v -> selectCategory(ScratchAdventureEngine.CAT_FANTASY));

        binding.btnScratchRevealNow.setOnClickListener(v -> binding.scratchOverlay.revealInstantly());

        binding.btnScratchTimer.setOnClickListener(v -> {
            ScratchCardDto c = engine.getCurrentCard();
            if (c == null || c.durationSec <= 0) return;
            binding.tvScratchTimer.setVisibility(View.VISIBLE);
            dareTimer.start(c.durationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvScratchTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvScratchTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnScratchFavorite.setOnClickListener(v -> {
            ScratchCardDto c = engine.getCurrentCard();
            if (c == null) return;
            TaskCard tc = new TaskCard(c.id, "scratch", c.tier, TaskType.DARE, c.tags, c.durationSec,
                    c.title + " — " + c.text, false, false);
            container.contentRepository.toggleFavorite(tc, saved -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.btnScratchFavorite.setText(saved ? R.string.btn_favorited : R.string.btn_favorite);
                    }
                });
            });
        });

        binding.btnScratchNext.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            if (cardRevealed) {
                ScratchCardDto current = engine.getCurrentCard();
                boolean canCountAsCompleted = current == null
                        || !container.consentFilter.hasSoftLimit(current.tags)
                        || binding.checkScratchSoftLimitAck.isChecked();
                if (canCountAsCompleted) {
                    engine.markCurrentCompleted();
                    if (engine.getCompletedCount() % 3 == 0) {
                        container.contentRepository.recordSessionStat("SCRATCH_ADVENTURE", engine.getCompletedCount(), engine.getCompletedCount(), 0);
                    }
                }
            }
            presentNextScratchCard();
        });

        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadScratchCardsAsync(bundle -> {
            List<ScratchCardDto> combined = new ArrayList<>();
            if (bundle.cards != null) combined.addAll(bundle.cards);
            combined.addAll(container.modeSynthesizer.synthesizeScratchCards(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 240
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadCards(combined, container.consentFilter, maxTier);
                updateCategoryButtonsUi();
                presentNextScratchCard();
            });
        });
    }

    private void selectCategory(String category) {
        HapticHelper.tick(binding.getRoot());
        engine.setCategory(category);
        updateCategoryButtonsUi();
        presentNextScratchCard();
    }

    private void updateCategoryButtonsUi() {
        String cat = engine.getActiveCategory();
        binding.btnCatAll.setAlpha(ScratchAdventureEngine.CAT_ALL.equals(cat) ? 1.0f : 0.52f);
        binding.btnCatDare.setAlpha(ScratchAdventureEngine.CAT_DARE.equals(cat) ? 1.0f : 0.52f);
        binding.btnCatLocation.setAlpha(ScratchAdventureEngine.CAT_LOCATION.equals(cat) ? 1.0f : 0.52f);
        binding.btnCatFantasy.setAlpha(ScratchAdventureEngine.CAT_FANTASY.equals(cat) ? 1.0f : 0.52f);
    }

    private void presentNextScratchCard() {
        dareTimer.cancel();
        cardRevealed = false;
        ScratchCardDto card = engine.drawNextCard();
        if (card == null) {
            binding.tvScratchMeta.setText("");
            binding.tvScratchTitle.setText(R.string.msg_no_eligible_cards);
            binding.tvScratchBody.setText("");
            binding.tvScratchProgress.setText("");
            binding.tvScratchTimer.setVisibility(View.GONE);
            binding.tvScratchSoftLimitWarning.setVisibility(View.GONE);
            binding.checkScratchSoftLimitAck.setVisibility(View.GONE);
            binding.btnScratchRevealNow.setVisibility(View.GONE);
            binding.layoutScratchActions.setVisibility(View.GONE);
            binding.scratchOverlay.setVisibility(View.GONE);
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }

        String tierName = getString(TagDisplayMapper.resolveTierNameRes(card.tier));
        binding.tvScratchMeta.setText(tierName + " · " + resolveCategoryLabel(card.category));
        binding.tvScratchTitle.setText(card.title);
        binding.tvScratchBody.setText(card.text);
        SoftLimitPromptHelper.bind(
                container.consentFilter,
                card.tags,
                binding.tvScratchSoftLimitWarning,
                binding.checkScratchSoftLimitAck,
                binding.btnScratchTimer
        );
        binding.tvScratchProgress.setText(getString(R.string.scratch_progress_fmt, 0));
        binding.tvScratchTimer.setVisibility(View.GONE);
        binding.btnScratchRevealNow.setVisibility(View.VISIBLE);
        binding.layoutScratchActions.setVisibility(View.GONE);
        binding.btnScratchFavorite.setText(R.string.btn_favorite);
        container.contentRepository.isFavoriteAsync(card.id, isSaved -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                ScratchCardDto cur = engine.getCurrentCard();
                if (binding != null && cur != null && card.id.equals(cur.id)) {
                    binding.btnScratchFavorite.setText(isSaved ? R.string.btn_favorited : R.string.btn_favorite);
                }
            });
        });

        if (card.durationSec > 0) {
            binding.btnScratchTimer.setVisibility(View.VISIBLE);
            binding.btnScratchTimer.setText(getString(R.string.btn_start_timer, card.durationSec));
        } else {
            binding.btnScratchTimer.setVisibility(View.GONE);
        }

        binding.scratchOverlay.setVisibility(View.VISIBLE);
        binding.scratchOverlay.resetFoil();
    }

    private String resolveCategoryLabel(String cat) {
        if (ScratchAdventureEngine.CAT_LOCATION.equals(cat)) return getString(R.string.scratch_cat_location);
        if (ScratchAdventureEngine.CAT_FANTASY.equals(cat)) return getString(R.string.scratch_cat_fantasy);
        return getString(R.string.scratch_cat_dare);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        binding = null;
    }
}
