package com.velvet.couplesgame.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.LocaleManager;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import com.velvet.couplesgame.databinding.FragmentSettingsBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.onboarding.OnboardingDialogFragment;
import com.velvet.couplesgame.ui.paywall.PaywallDialogFragment;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private AppContainer container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        SessionStatAdapter statAdapter = new SessionStatAdapter();
        binding.rvSessionStats.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessionStats.setAdapter(statAdapter);

        container.profileManager.observeState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null) return;
            binding.tvCoupleSummary.setText(getString(
                    R.string.settings_couple_summary,
                    state.partnerA.name,
                    state.partnerB.name,
                    state.safeWord
            ));
            statAdapter.setPartnerNames(state.partnerA.name, state.partnerB.name);
        });

        binding.btnEditCoupleProfile.setOnClickListener(v -> {
            HapticHelper.tick(v);
            OnboardingDialogFragment.newInstance(false)
                    .show(getChildFragmentManager(), "edit_couple_settings");
        });

        updateLanguageButtonsUi();
        binding.btnLangEn.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_EN));
        binding.btnLangZh.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_ZH));
        binding.btnLangJa.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_JA));

        container.entitlementManager.observePremiumStatus().observe(getViewLifecycleOwner(), isVip -> {
            if (binding == null) return;
            boolean unlocked = Boolean.TRUE.equals(isVip);
            binding.tvVipStatus.setText(unlocked ? R.string.settings_vip_active : R.string.settings_vip_free);
            binding.btnUpgradeVip.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        });

        binding.btnUpgradeVip.setOnClickListener(v -> {
            HapticHelper.tick(v);
            PaywallDialogFragment.newInstance(null, null).show(getChildFragmentManager(), "paywall_settings");
        });

        binding.btnRestorePurchases.setOnClickListener(v -> {
            HapticHelper.tick(v);
            container.billingBridge.startConnectionAndSync();
            Toast.makeText(requireContext(), R.string.paywall_restoring, Toast.LENGTH_SHORT).show();
        });

        binding.switchScreenShield.setChecked(container.entitlementManager.isScreenShieldEnabled());
        binding.switchScreenShield.setOnCheckedChangeListener((btn, checked) -> {
            HapticHelper.tick(btn);
            container.entitlementManager.setScreenShieldEnabled(checked);
            if (checked) {
                requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        });

        binding.btnPrivacyPromise.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_privacy_title)
                        .setMessage(R.string.settings_privacy_body)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
        );

        binding.btnClearMatches.setOnClickListener(v ->
                container.contentRepository.clearBlindMatches(() -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.settings_matches_cleared, Toast.LENGTH_SHORT).show()
                    );
                })
        );

        binding.btnClearHistory.setOnClickListener(v ->
                container.contentRepository.clearSessionHistory(() -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.settings_history_cleared, Toast.LENGTH_SHORT).show()
                    );
                })
        );

        container.contentRepository.observeSessionStats().observe(getViewLifecycleOwner(), stats -> {
            if (binding == null) return;
            statAdapter.submitList(stats);
            int totalSessions = stats != null ? stats.size() : 0;
            int totalScore = 0;
            if (stats != null) {
                for (SessionStatEntity s : stats) {
                    totalScore += s.playerACompleted + s.playerBCompleted;
                }
            }
            binding.tvStatsSummary.setText(getString(R.string.settings_stats_summary, totalSessions, totalScore));
            binding.tvStatsEmpty.setVisibility(totalSessions == 0 ? View.VISIBLE : View.GONE);
        });
    }

    private void updateLanguageButtonsUi() {
        if (binding == null) return;
        String current = LocaleManager.resolveAssetLangDir();
        binding.btnLangEn.setAlpha("en".equals(current) ? 1.0f : 0.52f);
        binding.btnLangZh.setAlpha("zh".equals(current) ? 1.0f : 0.52f);
        binding.btnLangJa.setAlpha("ja".equals(current) ? 1.0f : 0.52f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
