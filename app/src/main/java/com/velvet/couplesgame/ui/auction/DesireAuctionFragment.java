package com.velvet.couplesgame.ui.auction;

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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.AuctionContractDto;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.FragmentDesireAuctionBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.TaskPromptDialogHelper;
import java.util.ArrayList;
import java.util.List;

public class DesireAuctionFragment extends Fragment {
    private FragmentDesireAuctionBinding binding;
    private AppContainer container;
    private final DesireAuctionEngine engine = new DesireAuctionEngine();
    private final DareTimerHelper timerHelper = new DareTimerHelper();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDesireAuctionBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.btnBidPlus5.setOnClickListener(v -> tryBid(5));
        binding.btnBidPlus15.setOnClickListener(v -> tryBid(15));
        binding.btnBidAllIn.setOnClickListener(v -> {
            if (engine.bidAllIn()) {
                HapticHelper.confirm(v);
                renderUi();
                PhysicsAnimator.popIn(binding.tvAuctionBidAmount);
            } else {
                Toast.makeText(requireContext(), R.string.auction_insufficient_chips, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnBidFold.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            engine.foldCurrentBidder();
            renderUi();
            PhysicsAnimator.popIn(binding.cardAuctionContract);
        });

        binding.btnAuctionTimer.setOnClickListener(v -> {
            AuctionContractDto c = engine.getActiveContract();
            if (c == null || c.durationSec <= 0) return;
            binding.tvAuctionTimer.setVisibility(View.VISIBLE);
            timerHelper.start(c.durationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvAuctionTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvAuctionTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnAuctionNextLot.setOnClickListener(v -> {
            HapticHelper.tick(v);
            timerHelper.cancel();
            engine.nextContract();
            PhysicsAnimator.flipCard3D(binding.cardAuctionContract, this::renderUi);
        });

        binding.btnAuctionBailout.setOnClickListener(v -> {
            HapticHelper.tick(v);
            boolean targetA = engine.isBidderTurnA();
            launchBailoutDare(targetA);
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadIntimateModesAsync(bundle -> {
            List<AuctionContractDto> combined = new ArrayList<>();
            if (bundle.auctionContracts != null) combined.addAll(bundle.auctionContracts);
            combined.addAll(container.modeSynthesizer.synthesizeAuctionContracts(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 140
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadContracts(combined, container.consentFilter, maxTier);
                renderUi();
            });
        });
    }

    private void tryBid(int inc) {
        if (engine.placeBidIncrement(inc)) {
            HapticHelper.tick(binding.tvAuctionBidAmount);
            renderUi();
            PhysicsAnimator.popIn(binding.tvAuctionBidAmount);
        } else {
            Toast.makeText(requireContext(), R.string.auction_insufficient_chips, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchBailoutDare(boolean forPlayerA) {
        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            TaskCard dare = container.taskEngine.drawNext(allCards, mutualMatches, maxTier, null, true);
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
                        timerHelper,
                        forPlayerA,
                        dare,
                        getString(R.string.auction_bailout_badge),
                        getString(R.string.btn_wheel_pass_turn),
                        new TaskPromptDialogHelper.Callback() {
                            @Override
                            public void onApproved() {
                                engine.grantBailoutChips(forPlayerA);
                                renderUi();
                            }

                            @Override
                            public void onSecondaryAction() {}
                        }
                );
            });
        });
    }

    private void renderUi() {
        if (binding == null) return;
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);

        binding.tvAuctionChipsA.setText(getString(R.string.auction_wallet_fmt, nameA, engine.getChipsA()));
        binding.tvAuctionChipsB.setText(getString(R.string.auction_wallet_fmt, nameB, engine.getChipsB()));

        AuctionContractDto c = engine.getActiveContract();
        if (c == null) {
            binding.tvAuctionTitle.setText(R.string.msg_no_eligible_cards);
            binding.tvAuctionDesc.setText("");
            binding.layoutAuctionBidding.setVisibility(View.GONE);
            binding.layoutAuctionSold.setVisibility(View.GONE);
            return;
        }

        binding.tvAuctionTier.setText(TagDisplayMapper.resolveTierNameRes(c.tier));
        binding.tvAuctionTitle.setText(c.title);
        binding.tvAuctionDesc.setText(c.text);

        Boolean topA = engine.getHighestBidderIsA();
        if (topA == null) {
            binding.tvAuctionBidAmount.setText(R.string.auction_opening_bid);
        } else {
            String leader = topA ? nameA : nameB;
            binding.tvAuctionBidAmount.setText(getString(R.string.auction_current_bid_fmt, engine.getCurrentBid(), leader));
        }

        boolean sold = (engine.getPhase() == DesireAuctionEngine.BidPhase.SOLD);
        binding.layoutAuctionBidding.setVisibility(sold ? View.GONE : View.VISIBLE);
        binding.layoutAuctionSold.setVisibility(sold ? View.VISIBLE : View.GONE);

        if (!sold) {
            String turnName = engine.isBidderTurnA() ? nameA : nameB;
            binding.tvAuctionTurn.setText(getString(R.string.auction_turn_fmt, turnName));
            int currentWallet = engine.isBidderTurnA() ? engine.getChipsA() : engine.getChipsB();
            binding.btnAuctionBailout.setVisibility(currentWallet <= engine.getCurrentBid() ? View.VISIBLE : View.GONE);
            binding.btnBidPlus5.setAlpha(currentWallet >= engine.getCurrentBid() + 5 ? 1.0f : 0.45f);
            binding.btnBidPlus15.setAlpha(currentWallet >= engine.getCurrentBid() + 15 ? 1.0f : 0.45f);
            binding.btnBidAllIn.setAlpha(currentWallet > engine.getCurrentBid() ? 1.0f : 0.45f);
            binding.tvAuctionTimer.setVisibility(View.GONE);
        } else {
            String winner = Boolean.TRUE.equals(topA) ? nameA : nameB;
            binding.tvAuctionTurn.setText(getString(R.string.auction_sold_banner, winner, engine.getCurrentBid()));
            if (c.durationSec > 0) {
                binding.btnAuctionTimer.setVisibility(View.VISIBLE);
                binding.btnAuctionTimer.setText(getString(R.string.btn_start_timer, c.durationSec));
            } else {
                binding.btnAuctionTimer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHelper.cancel();
        if (container != null && engine.getContractsWonA() + engine.getContractsWonB() > 0) {
            container.contentRepository.recordSessionStat("DESIRE_AUCTION", engine.getContractsWonA(), engine.getContractsWonB(), 0);
        }
        binding = null;
    }
}
