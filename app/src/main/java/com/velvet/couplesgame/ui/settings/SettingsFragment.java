package com.velvet.couplesgame.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.LocaleManager;
import com.velvet.couplesgame.core.profile.TogetherDateFormat;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import com.velvet.couplesgame.databinding.FragmentSettingsBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.onboarding.OnboardingDialogFragment;
import com.velvet.couplesgame.ui.paywall.PaywallDialogFragment;
import java.time.LocalDate;
import java.util.Locale;

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
        binding.rvSessionStats.setLayoutManager(new LinearLayoutManager(requireContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
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
            updateTogetherDateUi();
        });

        binding.btnEditCoupleProfile.setOnClickListener(v -> {
            HapticHelper.tick(v);
            OnboardingDialogFragment.newInstance(false)
                    .show(getChildFragmentManager(), "edit_couple_settings");
        });

        binding.btnSetTogetherDate.setOnClickListener(v -> {
            HapticHelper.tick(v);
            openTogetherDatePicker();
        });

        updateTogetherDateUi();

        updateLanguageButtonsUi();
        binding.btnLangEn.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_EN));
        binding.btnLangZh.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_ZH_CN));
        binding.btnLangZhTw.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_ZH_TW));
        binding.btnLangJa.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_JA));
        binding.btnLangKo.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_KO));
        binding.btnLangEs.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_ES));
        binding.btnLangFr.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_FR));
        binding.btnLangPt.setOnClickListener(v -> LocaleManager.switchLanguage(LocaleManager.LANG_PT_BR));

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

    /** Let the couple pick (or clear) the date they got together. */
    private void openTogetherDatePicker() {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.settings_together_date_picker_title)
                .setCalendarConstraints(buildDateConstraints());

        // Pre-select the stored date so editing does not start from scratch.
        LocalDate stored = TogetherDateFormat.parse(container.profileManager.getTogetherSince());
        if (stored != null) {
            builder.setSelection(TogetherDateFormat.toUtcMillis(stored));
        }

        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(millis -> {
            LocalDate picked = TogetherDateFormat.fromUtcMillis(millis);
            if (picked == null) return;
            if (picked.isAfter(LocalDate.now())) {
                Toast.makeText(requireContext(), R.string.settings_together_date_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            container.profileManager.saveTogetherSince(TogetherDateFormat.toIso(picked));
            HapticHelper.confirm(binding.getRoot());
            Toast.makeText(requireContext(), R.string.settings_together_date_saved,
                    Toast.LENGTH_SHORT).show();
        });

        picker.show(getChildFragmentManager(), "together_date_picker");
    }

    /** Only dates up to today are selectable: the future cannot be "together since". */
    private static CalendarConstraints buildDateConstraints() {
        return new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();
    }

    /** Show the saved together-since date, or a prompt when it has not been set. */
    private void updateTogetherDateUi() {
        if (binding == null) return;
        String iso = container.profileManager.getTogetherSince();
        String display = TogetherDateFormat.display(iso, currentLocale());
        if (display == null) {
            binding.tvTogetherDateValue.setText(R.string.settings_together_date_unset);
            binding.btnSetTogetherDate.setText(R.string.settings_together_date_set);
            binding.tvTogetherDateValue.setOnClickListener(null);
        } else {
            binding.tvTogetherDateValue.setText(display);
            binding.btnSetTogetherDate.setText(R.string.settings_together_date_edit);
            // Tapping the saved date offers to clear it.
            binding.tvTogetherDateValue.setOnClickListener(v -> confirmClearTogetherDate());
        }
    }

    /** Ask before wiping the together-since date. */
    private void confirmClearTogetherDate() {
        HapticHelper.tick(binding.getRoot());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_together_date_clear)
                .setMessage(R.string.settings_together_date_clear_msg)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_together_date_clear, (d, which) -> {
                    container.profileManager.saveTogetherSince("");
                    Toast.makeText(requireContext(), R.string.settings_together_date_cleared,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** The locale the app is currently running in, used to localise the date. */
    private Locale currentLocale() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        Locale locale = (!locales.isEmpty()) ? locales.get(0) : Locale.getDefault();
        return locale;
    }

    private void updateLanguageButtonsUi() {
        if (binding == null) return;
        String current = LocaleManager.resolveAssetLangDir();
        binding.btnLangEn.setAlpha("en".equals(current) ? 1.0f : 0.50f);
        binding.btnLangZh.setAlpha("zh".equals(current) ? 1.0f : 0.50f);
        binding.btnLangZhTw.setAlpha("zh_tw".equals(current) ? 1.0f : 0.50f);
        binding.btnLangJa.setAlpha("ja".equals(current) ? 1.0f : 0.50f);
        binding.btnLangKo.setAlpha("ko".equals(current) ? 1.0f : 0.50f);
        binding.btnLangEs.setAlpha("es".equals(current) ? 1.0f : 0.50f);
        binding.btnLangFr.setAlpha("fr".equals(current) ? 1.0f : 0.50f);
        binding.btnLangPt.setAlpha("pt".equals(current) ? 1.0f : 0.50f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
