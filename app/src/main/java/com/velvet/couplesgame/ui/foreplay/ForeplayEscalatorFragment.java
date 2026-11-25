package com.velvet.couplesgame.ui.foreplay;

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
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ForeplayStageDto;
import com.velvet.couplesgame.databinding.FragmentForeplayEscalatorBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.AftercareDialogHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import java.util.ArrayList;
import java.util.List;

public class ForeplayEscalatorFragment extends Fragment {
    private FragmentForeplayEscalatorBinding binding;
    private AppContainer container;
    private final ForeplayEscalatorEngine engine = new ForeplayEscalatorEngine();
    private final DareTimerHelper stageTimer = new DareTimerHelper();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForeplayEscalatorBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        binding.btnForeplaySwap.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.swapLead();
            renderStage();
        });

        binding.btnForeplayStartTimer.setOnClickListener(v -> {
            ForeplayStageDto s = engine.getCurrentStage();
            if (s == null || s.durationSec <= 0) return;
            binding.tvForeplayTimer.setVisibility(View.VISIBLE);
            stageTimer.start(s.durationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvForeplayTimer.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvForeplayTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnForeplayNext.setOnClickListener(v -> {
            stageTimer.cancel();
            HapticHelper.confirm(v);
            boolean completed = engine.nextStage();
            if (completed) {
                container.contentRepository.recordSessionStat("FOREPLAY_ESCALATOR", 5, 5, 0);
                Toast.makeText(requireContext(), R.string.foreplay_complete_toast, Toast.LENGTH_LONG).show();
            }
            PhysicsAnimator.flipCard3D(binding.cardForeplayStage, () -> {
                renderStage();
                if (completed) AftercareDialogHelper.show(this);
            });
        });

        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadExtremeModesAsync(bundle -> {
            List<ForeplayStageDto> combined = new ArrayList<>();
            if (container.ageGateManager.isAdultContentAllowed()
                    && maxTier >= 4
                    && bundle.foreplayStages != null) {
                combined.addAll(bundle.foreplayStages);
            }
            combined.addAll(container.modeSynthesizer.synthesizeForeplayStages(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 12
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadStages(combined, container.consentFilter);
                renderStage();
            });
        });
    }

    private void renderStage() {
        if (binding == null || !isAdded()) return;
        ForeplayStageDto stage = engine.getCurrentStage();
        if (stage == null) {
            binding.tvForeplayTitle.setText(R.string.msg_no_eligible_cards);
            binding.tvForeplayInstruction.setText("");
            binding.tvForeplaySoftLimitWarning.setVisibility(View.GONE);
            binding.checkForeplaySoftLimitAck.setVisibility(View.GONE);
            binding.btnForeplayStartTimer.setVisibility(View.GONE);
            binding.btnForeplayNext.setEnabled(false);
            return;
        }
        binding.btnForeplayNext.setEnabled(true);

        int stepNum = engine.getCurrentIndex() + 1;
        int total = engine.getTotalStages();
        binding.progressForeplay.setMax(total);
        binding.progressForeplay.setProgress(stepNum);

        String lead = container.profileManager.getPlayerName(engine.isLeadPlayerA());
        String receiver = container.profileManager.getPlayerName(!engine.isLeadPlayerA());
        binding.tvForeplayLead.setText(getString(R.string.foreplay_lead_fmt, lead, receiver));
        binding.tvForeplayStepBadge.setText(getString(R.string.foreplay_step_fmt, stepNum, total));
        binding.tvForeplayTitle.setText(stage.stageTitle);
        SoftLimitPromptHelper.bind(
                container.consentFilter,
                stage.tags,
                binding.tvForeplaySoftLimitWarning,
                binding.checkForeplaySoftLimitAck,
                binding.btnForeplayNext,
                binding.btnForeplayStartTimer
        );
        binding.tvForeplayInstruction.setText(stage.instruction);

        binding.tvForeplayTimer.setVisibility(View.GONE);
        if (stage.durationSec > 0) {
            binding.btnForeplayStartTimer.setVisibility(View.VISIBLE);
            binding.btnForeplayStartTimer.setText(getString(R.string.btn_start_timer, stage.durationSec));
        } else {
            binding.btnForeplayStartTimer.setVisibility(View.GONE);
        }

        binding.btnForeplayNext.setText(engine.isLastStage()
                ? R.string.foreplay_btn_restart
                : R.string.foreplay_btn_escalate);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stageTimer.cancel();
        binding = null;
    }
}
