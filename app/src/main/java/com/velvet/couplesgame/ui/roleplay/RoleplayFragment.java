package com.velvet.couplesgame.ui.roleplay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayActDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayScenarioDto;
import com.velvet.couplesgame.databinding.FragmentRoleplayBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.AftercareDialogHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import java.util.ArrayList;
import java.util.List;

public class RoleplayFragment extends Fragment {
    private FragmentRoleplayBinding binding;
    private AppContainer container;
    private final RoleplayEngine engine = new RoleplayEngine();
    private final DareTimerHelper sceneTimer = new DareTimerHelper();
    private RoleplayScenarioAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoleplayBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine.attachHistoryStore(container.promptHistory);

        adapter = new RoleplayScenarioAdapter(scenario -> {
            HapticHelper.tick(binding.rvRoleplayList);
            engine.startScenario(scenario);
            renderStage();
        });
        binding.rvRoleplayList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRoleplayList.setAdapter(adapter);

        binding.btnRpSwapRoles.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.swapRoles();
            renderActContent();
        });

        binding.btnRpSceneTimer.setOnClickListener(v -> {
            RoleplayActDto act = engine.getCurrentAct();
            if (act == null || act.durationSec <= 0) return;
            binding.tvRpTimerStatus.setVisibility(View.VISIBLE);
            sceneTimer.start(act.durationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (binding != null && isAdded()) {
                        binding.tvRpTimerStatus.setText(getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (binding != null && isAdded()) {
                        HapticHelper.countdownFinished(binding.getRoot());
                        binding.tvRpTimerStatus.setText(R.string.timer_done);
                    }
                }
            });
        });

        binding.btnRpNextAct.setOnClickListener(v -> {
            sceneTimer.cancel();
            HapticHelper.confirm(v);
            boolean finished = engine.advanceAct();
            if (finished) {
                container.contentRepository.recordSessionStat("ROLEPLAY", engine.getCompletedScenarios(), engine.getCompletedScenarios(), 0);
                Toast.makeText(requireContext(), R.string.roleplay_finale_toast, Toast.LENGTH_LONG).show();
                engine.exitScenario();
                renderStage();
                AftercareDialogHelper.show(this);
            } else {
                PhysicsAnimator.flipCard3D(binding.cardRpAct, this::renderActContent);
            }
        });

        binding.btnRpBackList.setOnClickListener(v -> {
            sceneTimer.cancel();
            HapticHelper.tick(v);
            engine.exitScenario();
            renderStage();
        });

        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadRoleplayAsync(bundle -> {
            List<RoleplayScenarioDto> combined = new ArrayList<>();
            if (bundle.scenarios != null) combined.addAll(bundle.scenarios);
            combined.addAll(container.modeSynthesizer.synthesizeRoleplayScenarios(
                    container.assetLoader.loadGeneratorBundle(), container.consentFilter, maxTier, 90
            ));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                localizeGeneratedScenarios(combined);
                engine.loadScenarios(combined, container.consentFilter, maxTier);
                adapter.submitList(engine.getAvailableScenarios());
                binding.tvRpEmpty.setVisibility(engine.getAvailableScenarios().isEmpty() ? View.VISIBLE : View.GONE);
                renderStage();
            });
        });
    }

    private void localizeGeneratedScenarios(List<RoleplayScenarioDto> scenarios) {
        for (RoleplayScenarioDto scenario : scenarios) {
            if (scenario.id == null || !scenario.id.startsWith("syn_rp_")) continue;
            scenario.leadRole = getString(R.string.roleplay_role_lead);
            scenario.partnerRole = getString(R.string.roleplay_role_partner);
            for (int i = 0; i < scenario.acts.size(); i++) {
                switch (i) {
                    case 0:
                        scenario.acts.get(i).actTitle = getString(R.string.roleplay_act_1);
                        break;
                    case 1:
                        scenario.acts.get(i).actTitle = getString(R.string.roleplay_act_2);
                        break;
                    case 2:
                        scenario.acts.get(i).actTitle = getString(R.string.roleplay_act_3);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void renderStage() {
        if (binding == null) return;
        boolean inScene = engine.getActiveScenario() != null;
        binding.layoutRpBrowser.setVisibility(inScene ? View.GONE : View.VISIBLE);
        binding.layoutRpStage.setVisibility(inScene ? View.VISIBLE : View.GONE);
        if (inScene) {
            renderActContent();
            PhysicsAnimator.popIn(binding.cardRpAct);
        }
    }

    private void renderActContent() {
        RoleplayScenarioDto s = engine.getActiveScenario();
        RoleplayActDto act = engine.getCurrentAct();
        if (s == null || act == null || binding == null) return;

        String leadName = container.profileManager.getPlayerName(engine.isLeadPlayerA());
        String partnerName = container.profileManager.getPlayerName(!engine.isLeadPlayerA());

        binding.tvRpStageTitle.setText(s.title);
        binding.tvRpCastBanner.setText(getString(R.string.roleplay_cast_fmt, leadName, s.leadRole, partnerName, s.partnerRole));
        binding.tvRpActHeader.setText(act.actTitle);
        SoftLimitPromptHelper.bind(
                container.consentFilter,
                s.tags,
                binding.tvRpSoftLimitWarning,
                binding.checkRpSoftLimitAck,
                binding.btnRpNextAct,
                binding.btnRpSceneTimer
        );
        binding.tvRpDirection.setText(engine.renderDirection(act.direction, leadName, partnerName));
        binding.tvRpWhisperLine.setText(act.whisperLine);

        binding.tvRpTimerStatus.setVisibility(View.GONE);
        if (act.durationSec > 0) {
            binding.btnRpSceneTimer.setVisibility(View.VISIBLE);
            binding.btnRpSceneTimer.setText(getString(R.string.btn_start_timer, act.durationSec));
        } else {
            binding.btnRpSceneTimer.setVisibility(View.GONE);
        }

        binding.btnRpNextAct.setText(engine.isLastAct()
                ? R.string.roleplay_btn_finish_scene
                : R.string.roleplay_btn_next_act);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sceneTimer.cancel();
        binding = null;
    }
}
