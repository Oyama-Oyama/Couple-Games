package com.velvet.couplesgame.ui.blindmatch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentBlindMatchBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import java.util.ArrayList;
import java.util.List;

public class BlindMatchFragment extends Fragment {
    private static final String REQ_BLIND_HANDOFF = "req_blind_match_handoff";

    private FragmentBlindMatchBinding binding;
    private AppContainer container;
    private final BlindMatchSessionManager session = new BlindMatchSessionManager();
    private BlindMatchResultAdapter resultAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBlindMatchBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        session.attachHistoryStore(container.promptHistory);

        getChildFragmentManager().setFragmentResultListener(REQ_BLIND_HANDOFF, getViewLifecycleOwner(), (key, res) -> {
            session.beginPlayerBTurn();
            if (binding != null) {
                binding.layoutSwipeStage.setVisibility(View.VISIBLE);
                renderCurrentPhase();
            }
        });

        resultAdapter = new BlindMatchResultAdapter();
        binding.rvMatchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMatchResults.setAdapter(resultAdapter);

        binding.swipeCard.setOnSwipeListener(new SwipeableCardView.OnSwipeListener() {
            @Override
            public void onSwipeProgress(float progress) {
                if (binding == null) return;
                binding.tvBadgeYes.setAlpha(Math.max(0f, progress));
                binding.tvBadgePass.setAlpha(Math.max(0f, -progress));

                float absP = Math.min(1f, Math.abs(progress));
                float scale = 0.93f + 0.07f * absP;
                float alpha = 0.55f + 0.45f * absP;
                float ty = (1f - absP) * 14f * getResources().getDisplayMetrics().density;
                binding.cardStackUnderlay.setScaleX(scale);
                binding.cardStackUnderlay.setScaleY(scale);
                binding.cardStackUnderlay.setAlpha(alpha);
                binding.cardStackUnderlay.setTranslationY(ty);
            }

            @Override
            public void onSwiped(boolean liked) {
                handleSwipeRecorded(liked);
            }
        });

        binding.btnPass.setOnClickListener(v -> recordButtonVote(false));
        binding.btnYes.setOnClickListener(v -> recordButtonVote(true));

        binding.btnPlayBoardWithMatches.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            Navigation.findNavController(v).navigate(R.id.action_blind_match_to_couple_board);
        });

        binding.btnRestartBlindMatch.setOnClickListener(v -> {
            HapticHelper.tick(v);
            loadAndStartSession();
        });

        loadAndStartSession();
    }

    private void loadAndStartSession() {
        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadDesireMatchCardsAsync(cards -> {
            List<TaskCard> combined = new ArrayList<>();
            if (cards != null) combined.addAll(cards);
            combined.addAll(container.modeSynthesizer.synthesizeDesireMatchCards(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 180
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                session.startShuffledRound(combined, container.consentFilter, maxTier, BlindMatchSessionManager.DEFAULT_ROUND_SIZE);
                renderCurrentPhase();
            });
        });
    }

    private void handleSwipeRecorded(boolean liked) {
        BlindMatchSessionManager.Phase nextPhase = session.recordSwipe(liked);
        if (nextPhase == BlindMatchSessionManager.Phase.HANDOFF_TO_B) {
            binding.layoutSwipeStage.setVisibility(View.INVISIBLE);
            String nameB = container.profileManager.getPlayerName(false);
            PassPhoneDialog.newInstance(REQ_BLIND_HANDOFF, nameB)
                    .show(getChildFragmentManager(), "blind_match_handoff");
        } else if (nextPhase == BlindMatchSessionManager.Phase.REVEAL) {
            persistAndReveal();
        } else {
            renderCurrentPhase();
        }
    }

    private void recordButtonVote(boolean liked) {
        BlindMatchSessionManager.Phase phase = session.getPhase();
        if (phase != BlindMatchSessionManager.Phase.PLAYER_A
                && phase != BlindMatchSessionManager.Phase.PLAYER_B) return;
        binding.btnPass.setEnabled(false);
        binding.btnYes.setEnabled(false);
        HapticHelper.confirm(liked ? binding.btnYes : binding.btnPass);
        handleSwipeRecorded(liked);
    }

    private void persistAndReveal() {
        List<TaskCard> mutualMatches = session.getMutualMatches();
        container.contentRepository.saveBlindMatchResults(
                session.buildPersistenceEntities(System.currentTimeMillis()),
                () -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null) return;
                        HapticHelper.confirm(binding.getRoot());
                        binding.layoutSwipeStage.setVisibility(View.GONE);
                        binding.layoutRevealStage.setVisibility(View.VISIBLE);

                        binding.tvRevealSummary.setText(getString(R.string.blind_match_reveal_count, mutualMatches.size()));
                        binding.tvEmptyReveal.setVisibility(mutualMatches.isEmpty() ? View.VISIBLE : View.GONE);
                        resultAdapter.submitList(mutualMatches);
                    });
                }
        );
    }

    private void renderCurrentPhase() {
        BlindMatchSessionManager.Phase phase = session.getPhase();
        binding.tvSwipeSoftLimitWarning.setVisibility(View.GONE);
        if (phase == BlindMatchSessionManager.Phase.EMPTY) {
            binding.layoutSwipeStage.setVisibility(View.GONE);
            binding.layoutRevealStage.setVisibility(View.VISIBLE);
            binding.tvRevealSummary.setText(R.string.msg_no_eligible_cards);
            binding.tvEmptyReveal.setVisibility(View.VISIBLE);
            resultAdapter.submitList(null);
            return;
        }

        if (phase == BlindMatchSessionManager.Phase.PLAYER_A || phase == BlindMatchSessionManager.Phase.PLAYER_B) {
            binding.layoutRevealStage.setVisibility(View.GONE);
            binding.layoutSwipeStage.setVisibility(View.VISIBLE);

            String playerLabel = container.profileManager.getPlayerName(phase == BlindMatchSessionManager.Phase.PLAYER_A);
            int step = session.getCurrentIndex() + 1;
            int total = session.getTotalCount();

            binding.tvPhaseBanner.setText(getString(R.string.blind_match_turn_banner, playerLabel, step, total));
            binding.progressBlindMatch.setMax(total);
            binding.progressBlindMatch.setProgress(step);
            binding.btnPass.setEnabled(true);
            binding.btnYes.setEnabled(true);

            boolean hasNext = step < total;
            binding.cardStackUnderlay.setVisibility(hasNext ? View.VISIBLE : View.INVISIBLE);

            TaskCard card = session.getCurrentCard();
            if (card != null) {
                String tierLabel = getString(TagDisplayMapper.resolveTierNameRes(card.tier));
                String tagStr = TagDisplayMapper.formatTagList(requireContext(), card.tags);
                binding.tvSwipeTier.setText(tagStr.isEmpty() ? tierLabel : tierLabel + " · " + tagStr);
                binding.tvSwipeText.setText(card.text);
                binding.tvSwipeSoftLimitWarning.setVisibility(
                        container.consentFilter.hasSoftLimit(card.tags) ? View.VISIBLE : View.GONE
                );
                binding.tvBadgeYes.setAlpha(0f);
                binding.tvBadgePass.setAlpha(0f);
                binding.swipeCard.resetInstant();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
