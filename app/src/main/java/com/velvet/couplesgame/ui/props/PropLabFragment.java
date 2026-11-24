package com.velvet.couplesgame.ui.props;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PropChallengeDto;
import com.velvet.couplesgame.databinding.FragmentPropLabBinding;
import java.util.ArrayList;
import java.util.List;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;

public class PropLabFragment extends Fragment {
    private FragmentPropLabBinding binding;
    private AppContainer container;
    private final PropLabEngine engine = new PropLabEngine();
    private final DareTimerHelper dareTimer = new DareTimerHelper();
    private boolean actorIsPlayerA = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPropLabBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        bindPropToggle(binding.btnPropIce, "ice");
        bindPropToggle(binding.btnPropBlindfold, "blindfold");
        bindPropToggle(binding.btnPropOil, "oil");
        bindPropToggle(binding.btnPropTie, "tie");
        bindPropToggle(binding.btnPropHoney, "honey");
        bindPropToggle(binding.btnPropMirror, "mirror");
        bindPropToggle(binding.btnPropFeather, "feather");
        refreshPropChipStates();

        binding.btnPropTimer.setOnClickListener(v -> {
            PropChallengeDto c = engine.getCurrentChallenge();
            if (c == null || c.durationSec <= 0) return;
            binding.tvPropTimer.setVisibility(View.VISIBLE);
            dareTimer.start(c.durationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvPropTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvPropTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnPropDraw.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            dareTimer.cancel();
            if (engine.getCurrentChallenge() != null) {
                engine.markCompleted();
                if (engine.getCompletedCount() % 3 == 0) {
                    container.contentRepository.recordSessionStat("PROP_LAB", engine.getCompletedCount(), engine.getCompletedCount(), 0);
                }
            }
            actorIsPlayerA = !actorIsPlayerA;
            drawNext();
        });

        int maxTier = container.entitlementManager.getSelectedMaxTier();
        container.contentRepository.loadExtremeModesAsync(bundle -> {
            List<PropChallengeDto> combined = new ArrayList<>();
            if (bundle.propChallenges != null) combined.addAll(bundle.propChallenges);
            combined.addAll(container.modeSynthesizer.synthesizePropChallenges(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 48
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadChallenges(combined, container.consentFilter, maxTier);
                drawNext();
            });
        });
    }

    private void bindPropToggle(MaterialButton btn, String key) {
        btn.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.toggleProp(key);
            refreshPropChipStates();
        });
    }

    private void refreshPropChipStates() {
        styleChip(binding.btnPropIce, "ice");
        styleChip(binding.btnPropBlindfold, "blindfold");
        styleChip(binding.btnPropOil, "oil");
        styleChip(binding.btnPropTie, "tie");
        styleChip(binding.btnPropHoney, "honey");
        styleChip(binding.btnPropMirror, "mirror");
        styleChip(binding.btnPropFeather, "feather");
    }

    private void styleChip(MaterialButton btn, String key) {
        boolean on = engine.isPropSelected(key);
        btn.setAlpha(on ? 1.0f : 0.42f);
    }

    private void drawNext() {
        PropChallengeDto c = engine.drawNextChallenge();
        if (c == null) {
            binding.tvPropMeta.setText("");
            binding.tvPropBody.setText(R.string.msg_no_eligible_cards);
            binding.tvPropTimer.setVisibility(View.GONE);
            binding.btnPropTimer.setVisibility(View.GONE);
            Toast.makeText(requireContext(), R.string.msg_no_eligible_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        String actor = container.profileManager.getPlayerName(actorIsPlayerA);
        PhysicsAnimator.flipCard3D(binding.cardPropChallenge, () -> {
            if (binding == null || !isAdded()) return;
            binding.tvPropMeta.setText(actor + "  ·  " + resolvePropLabel(c.propKey));
            binding.tvPropBody.setText(c.text);
            binding.tvPropTimer.setVisibility(View.GONE);
            if (c.durationSec > 0) {
                binding.btnPropTimer.setVisibility(View.VISIBLE);
                binding.btnPropTimer.setText(getString(R.string.btn_start_timer, c.durationSec));
            } else {
                binding.btnPropTimer.setVisibility(View.GONE);
            }
        });
    }

    private String resolvePropLabel(String key) {
        switch (key) {
            case "ice": return getString(R.string.prop_ice);
            case "blindfold": return getString(R.string.prop_blindfold);
            case "oil": return getString(R.string.prop_oil);
            case "tie": return getString(R.string.prop_tie);
            case "honey": return getString(R.string.prop_honey);
            case "mirror": return getString(R.string.prop_mirror);
            default: return getString(R.string.prop_feather);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dareTimer.cancel();
        binding = null;
    }
}
