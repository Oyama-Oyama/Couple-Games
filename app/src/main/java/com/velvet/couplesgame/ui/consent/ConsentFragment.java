package com.velvet.couplesgame.ui.consent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.databinding.FragmentConsentBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import java.util.List;

public class ConsentFragment extends Fragment {
    private static final String REQ_CONSENT_PRIVATE_A = "req_consent_private_a";
    private static final String REQ_CONSENT_PRIVATE_B = "req_consent_private_b";

    private enum PrivateStage {
        TOGETHER,
        PRIVATE_A,
        PRIVATE_B,
        PRIVATE_SUMMARY
    }

    private FragmentConsentBinding binding;
    private AppContainer container;
    private BoundaryAdapter adapter;
    private PrivateStage stage = PrivateStage.TOGETHER;
    private List<BoundaryRuleEntity> latestRules;
    /** AUDIT-002: 规则尚未载入 / 为空时显示暖色行动指引卡（默认 true，避免首帧空白列表）。 */
    private boolean rulesEmpty = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConsentBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        getChildFragmentManager().setFragmentResultListener(REQ_CONSENT_PRIVATE_A, getViewLifecycleOwner(), (k, r) -> {
            stage = PrivateStage.PRIVATE_A;
            renderStageUi();
        });

        getChildFragmentManager().setFragmentResultListener(REQ_CONSENT_PRIVATE_B, getViewLifecycleOwner(), (k, r) -> {
            stage = PrivateStage.PRIVATE_B;
            if (binding != null) {
                binding.rvBoundaries.setVisibility(View.VISIBLE);
                renderStageUi();
            }
        });

        adapter = new BoundaryAdapter((rule, isPlayerA) -> {
            HapticHelper.tick(view);
            container.consentRepository.cyclePlayerStatus(rule, isPlayerA);
        });

        binding.rvBoundaries.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBoundaries.setAdapter(adapter);

        container.profileManager.observeState().observe(getViewLifecycleOwner(), state -> {
            adapter.setPartnerNames(state.partnerA.name, state.partnerB.name);
            renderStageUi();
        });

        container.consentRepository.observeRules().observe(getViewLifecycleOwner(), rules -> {
            latestRules = rules;
            rulesEmpty = rules == null || rules.isEmpty();
            adapter.submitList(rules);
            updateSummaryBanner();
            renderStageUi();
        });

        binding.btnModeTogether.setOnClickListener(v -> {
            HapticHelper.tick(v);
            stage = PrivateStage.TOGETHER;
            renderStageUi();
        });

        binding.btnModePrivate.setOnClickListener(v -> {
            HapticHelper.tick(v);
            binding.rvBoundaries.setVisibility(View.INVISIBLE);
            binding.layoutConsentEmpty.setVisibility(View.GONE);
            String nameA = container.profileManager.getPlayerName(true);
            PassPhoneDialog.newInstance(REQ_CONSENT_PRIVATE_A, nameA)
                    .show(getChildFragmentManager(), "consent_private_a");
        });

        binding.btnPrivateNextStep.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            if (stage == PrivateStage.PRIVATE_A) {
                binding.rvBoundaries.setVisibility(View.INVISIBLE);
                String nameB = container.profileManager.getPlayerName(false);
                PassPhoneDialog.newInstance(REQ_CONSENT_PRIVATE_B, nameB)
                        .show(getChildFragmentManager(), "consent_private_b");
            } else if (stage == PrivateStage.PRIVATE_B) {
                stage = PrivateStage.PRIVATE_SUMMARY;
                renderStageUi();
            } else {
                stage = PrivateStage.TOGETHER;
                renderStageUi();
            }
        });

        renderStageUi();
    }

    private void renderStageUi() {
        if (binding == null) return;
        String nameA = container.profileManager.getPlayerName(true);
        String nameB = container.profileManager.getPlayerName(false);

        binding.btnModeTogether.setAlpha(stage == PrivateStage.TOGETHER ? 1.0f : 0.55f);
        binding.btnModePrivate.setAlpha(stage != PrivateStage.TOGETHER ? 1.0f : 0.55f);

        if (stage == PrivateStage.TOGETHER) {
            adapter.setDisplayMode(BoundaryAdapter.DisplayMode.TOGETHER);
            binding.rvBoundaries.setVisibility(View.VISIBLE);
            binding.layoutPrivateSummary.setVisibility(View.GONE);
            binding.btnPrivateNextStep.setVisibility(View.GONE);
            binding.tvConsentStageHint.setText(R.string.consent_desc);
        } else if (stage == PrivateStage.PRIVATE_A) {
            adapter.setDisplayMode(BoundaryAdapter.DisplayMode.PRIVATE_A_ONLY);
            binding.rvBoundaries.setVisibility(View.VISIBLE);
            binding.layoutPrivateSummary.setVisibility(View.GONE);
            binding.btnPrivateNextStep.setVisibility(View.VISIBLE);
            binding.tvConsentStageHint.setText(getString(R.string.consent_private_prompt, nameA));
            binding.btnPrivateNextStep.setText(getString(R.string.consent_pass_to_partner, nameB));
        } else if (stage == PrivateStage.PRIVATE_B) {
            adapter.setDisplayMode(BoundaryAdapter.DisplayMode.PRIVATE_B_ONLY);
            binding.rvBoundaries.setVisibility(View.VISIBLE);
            binding.layoutPrivateSummary.setVisibility(View.GONE);
            binding.btnPrivateNextStep.setVisibility(View.VISIBLE);
            binding.tvConsentStageHint.setText(getString(R.string.consent_private_prompt, nameB));
            binding.btnPrivateNextStep.setText(R.string.consent_finish_private);
        } else {
            binding.rvBoundaries.setVisibility(View.GONE);
            binding.layoutPrivateSummary.setVisibility(View.VISIBLE);
            binding.btnPrivateNextStep.setVisibility(View.VISIBLE);
            binding.btnPrivateNextStep.setText(R.string.consent_review_together);
            binding.tvConsentStageHint.setText(R.string.consent_private_done_hint);
            updateSummaryBanner();
        }
        updateEmptyState();
    }

    /**
     * AUDIT-002 · 空状态：复用既有 consent_title / consent_desc 组成「下一步行动指引」，
     * 零新增 string key（8 语言 key 集合保持 684）。私密汇总页优先展示汇总卡，
     * 因此不再叠加空状态卡。
     */
    private void updateEmptyState() {
        if (binding == null) return;
        boolean showEmpty = rulesEmpty && stage != PrivateStage.PRIVATE_SUMMARY;
        binding.layoutConsentEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        if (showEmpty) binding.rvBoundaries.setVisibility(View.GONE);
    }

    private void updateSummaryBanner() {
        if (binding == null) return;
        int blocked = countBlockedRules(latestRules);
        int soft = countSoftRules(latestRules);
        binding.tvPrivateSummaryCount.setText(getString(R.string.consent_private_summary_text, blocked, soft));
    }

    public static int countBlockedRules(List<BoundaryRuleEntity> rules) {
        if (rules == null) return 0;
        int c = 0;
        for (BoundaryRuleEntity r : rules) {
            if (r.isBlocked()) c++;
        }
        return c;
    }

    public static int countSoftRules(List<BoundaryRuleEntity> rules) {
        if (rules == null) return 0;
        int c = 0;
        for (BoundaryRuleEntity r : rules) {
            if (r.isSoftLimit()) c++;
        }
        return c;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
