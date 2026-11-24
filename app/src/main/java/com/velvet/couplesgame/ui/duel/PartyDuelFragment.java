package com.velvet.couplesgame.ui.duel;

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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.NhieCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.WyrCardDto;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentPartyDuelBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PartyDuelRootDto;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.ArrayList;
import java.util.List;

public class PartyDuelFragment extends Fragment {
    private FragmentPartyDuelBinding binding;
    private AppContainer container;
    private final PartyDuelEngine engine = new PartyDuelEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private boolean alternateDareTargetA = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPartyDuelBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.btnTabNhie.setOnClickListener(v -> switchMode(PartyDuelEngine.Mode.NHIE));
        binding.btnTabWyr.setOnClickListener(v -> switchMode(PartyDuelEngine.Mode.WYR));

        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);
        binding.btnConfessA.setText(getString(R.string.duel_btn_confess_named, nameA));
        binding.btnConfessB.setText(getString(R.string.duel_btn_confess_named, nameB));

        binding.btnConfessA.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            engine.recordConfession(true);
            updateScoreHud();
            triggerOptionalDare(true, getString(R.string.duel_confess_badge));
        });

        binding.btnConfessB.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            engine.recordConfession(false);
            updateScoreHud();
            triggerOptionalDare(false, getString(R.string.duel_confess_badge));
        });

        binding.btnNhieNext.setOnClickListener(v -> {
            HapticHelper.tick(v);
            drawAndAnimateCurrentMode();
        });

        binding.btnWyrAgree.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            engine.recordWyrOutcome(true);
            updateScoreHud();
            drawAndAnimateCurrentMode();
        });

        binding.btnWyrDisagree.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.recordWyrOutcome(false);
            updateScoreHud();
            alternateDareTargetA = !alternateDareTargetA;
            triggerOptionalDare(alternateDareTargetA, getString(R.string.duel_disagree_badge));
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadPartyDuelAsync(bundle -> {
            PartyDuelRootDto enriched = new PartyDuelRootDto();
            List<NhieCardDto> allNhie = bundle.nhie != null ? new ArrayList<>(bundle.nhie) : new ArrayList<>();
            allNhie.addAll(container.modeSynthesizer.synthesizeNhieCards(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 120
            ));
            enriched.nhie = allNhie;
            List<WyrCardDto> allWyr = bundle.wyr != null ? new ArrayList<>(bundle.wyr) : new ArrayList<>();
            allWyr.addAll(container.modeSynthesizer.synthesizeWyrMatchups(
                    bundle.wyr, container.consentFilter, maxTier, 250
            ));
            enriched.wyr = allWyr;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadBundle(enriched, container.consentFilter, maxTier);
                updateModeUi();
                drawAndAnimateCurrentMode();
            });
        });
    }

    private void switchMode(PartyDuelEngine.Mode nextMode) {
        HapticHelper.tick(binding.getRoot());
        engine.setActiveMode(nextMode);
        updateModeUi();
        drawAndAnimateCurrentMode();
    }

    private void updateModeUi() {
        boolean isNhie = engine.getActiveMode() == PartyDuelEngine.Mode.NHIE;
        binding.btnTabNhie.setAlpha(isNhie ? 1.0f : 0.52f);
        binding.btnTabWyr.setAlpha(isNhie ? 0.52f : 1.0f);
        binding.layoutNhieStage.setVisibility(isNhie ? View.VISIBLE : View.GONE);
        binding.layoutWyrStage.setVisibility(isNhie ? View.GONE : View.VISIBLE);
        updateScoreHud();
    }

    private void drawAndAnimateCurrentMode() {
        if (engine.getActiveMode() == PartyDuelEngine.Mode.NHIE) {
            NhieCardDto next = engine.nextNhie();
            if (next == null) {
                Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                return;
            }
            PhysicsAnimator.flipCard3D(binding.cardNhie, () -> {
                if (binding == null || !isAdded()) return;
                binding.tvNhieMeta.setText(TagDisplayMapper.resolveTierNameRes(next.tier));
                binding.tvNhieText.setText(next.text);
            });
        } else {
            WyrCardDto next = engine.nextWyr();
            if (next == null) {
                Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
                return;
            }
            PhysicsAnimator.flipCard3D(binding.cardWyr, () -> {
                if (binding == null || !isAdded()) return;
                binding.tvWyrMeta.setText(TagDisplayMapper.resolveTierNameRes(next.tier));
                binding.tvWyrOptionA.setText("A · " + next.optionA);
                binding.tvWyrOptionB.setText("B · " + next.optionB);
            });
        }
    }

    private void triggerOptionalDare(boolean actorA, String badge) {
        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard dare = container.taskEngine.drawNext(allCards, mutualMatches, maxTier, null, true);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (dare == null) {
                    drawAndAnimateCurrentMode();
                    return;
                }
                TaskPromptDialogHelper.show(
                        this,
                        container,
                        dareTimer,
                        actorA,
                        dare,
                        badge,
                        getString(R.string.btn_wheel_pass_turn),
                        new TaskPromptDialogHelper.Callback() {
                            @Override
                            public void onApproved() {
                                drawAndAnimateCurrentMode();
                            }

                            @Override
                            public void onSecondaryAction() {
                                drawAndAnimateCurrentMode();
                            }
                        }
                );
            });
        });
    }

    private void updateScoreHud() {
        if (binding == null) return;
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);
        if (engine.getActiveMode() == PartyDuelEngine.Mode.NHIE) {
            binding.tvDuelScore.setText(getString(
                    R.string.duel_nhie_score_fmt,
                    nameA, engine.getConfessionsA(),
                    nameB, engine.getConfessionsB()
            ));
        } else {
            binding.tvDuelScore.setText(getString(
                    R.string.duel_wyr_score_fmt,
                    engine.getWyrMatches(),
                    engine.getWyrDisagrees()
            ));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        if (container != null && engine.getConfessionsA() + engine.getConfessionsB() + engine.getWyrMatches() > 0) {
            container.contentRepository.recordSessionStat(
                    "PARTY_DUEL",
                    engine.getConfessionsA() + engine.getWyrMatches(),
                    engine.getConfessionsB() + engine.getWyrMatches(),
                    0
            );
        }
        binding = null;
    }
}
