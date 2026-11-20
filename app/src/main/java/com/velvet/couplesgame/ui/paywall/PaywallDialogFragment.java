package com.velvet.couplesgame.ui.paywall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.BuildConfig;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.billing.PlayBillingBridge;
import com.velvet.couplesgame.core.billing.PricingCatalog;
import com.velvet.couplesgame.databinding.DialogPaywallBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;

/**
 * Configuration-change safe Paywall dialog delivering unlock results via AndroidX Fragment Result API.
 */
public class PaywallDialogFragment extends DialogFragment implements PlayBillingBridge.BillingUiListener {
    public static final String RESULT_UNLOCKED = "paywall_unlocked";
    public static final String RESULT_ACTION_TAG = "paywall_action_tag";
    private static final String ARG_REQUEST_KEY = "request_key";
    private static final String ARG_ACTION_TAG = "action_tag";

    private DialogPaywallBinding binding;
    private AppContainer container;
    private String selectedSku = PlayBillingBridge.SKU_LIFETIME;

    public static PaywallDialogFragment newInstance(@Nullable String requestKey, @Nullable String actionTag) {
        PaywallDialogFragment dialog = new PaywallDialogFragment();
        Bundle args = new Bundle();
        if (requestKey != null) args.putString(ARG_REQUEST_KEY, requestKey);
        if (actionTag != null) args.putString(ARG_ACTION_TAG, actionTag);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        binding = DialogPaywallBinding.inflate(LayoutInflater.from(requireContext()));

        container.billingBridge.setUiListener(this);
        container.billingBridge.startConnectionAndSync();

        refreshPriceLabels();
        updateSelectedPlanVisuals();

        binding.cardPlanLifetime.setOnClickListener(v -> {
            HapticHelper.tick(v);
            selectedSku = PlayBillingBridge.SKU_LIFETIME;
            updateSelectedPlanVisuals();
        });
        binding.cardPlanAnnual.setOnClickListener(v -> {
            HapticHelper.tick(v);
            selectedSku = PlayBillingBridge.SKU_ANNUAL_PASS;
            updateSelectedPlanVisuals();
        });
        binding.cardPlanWeekly.setOnClickListener(v -> {
            HapticHelper.tick(v);
            selectedSku = PlayBillingBridge.SKU_WEEKLY_PASS;
            updateSelectedPlanVisuals();
        });

        binding.btnPaywallContinue.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            boolean launched = container.billingBridge.launchPurchaseFlow(requireActivity(), selectedSku);
            if (!launched) {
                Toast.makeText(requireContext(), R.string.paywall_play_unavailable, Toast.LENGTH_LONG).show();
            }
        });

        binding.btnPaywallRestore.setOnClickListener(v -> {
            HapticHelper.tick(v);
            container.billingBridge.queryActiveEntitlements();
            Toast.makeText(requireContext(), R.string.paywall_restoring, Toast.LENGTH_SHORT).show();
        });

        binding.btnPaywallClose.setOnClickListener(v -> dismiss());

        if (BuildConfig.DEBUG) {
            binding.btnDebugSandboxUnlock.setVisibility(View.VISIBLE);
            binding.btnDebugSandboxUnlock.setOnClickListener(v -> {
                HapticHelper.confirm(v);
                container.entitlementManager.setDebugOverride(true);
                Toast.makeText(requireContext(), R.string.paywall_unlocked_toast, Toast.LENGTH_SHORT).show();
                notifyUnlockedAndDismiss();
            });
        } else {
            binding.btnDebugSandboxUnlock.setVisibility(View.GONE);
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    private void notifyUnlockedAndDismiss() {
        String requestKey = getArguments() != null ? getArguments().getString(ARG_REQUEST_KEY) : null;
        if (requestKey != null && isAdded()) {
            Bundle result = new Bundle();
            result.putBoolean(RESULT_UNLOCKED, true);
            String actionTag = getArguments().getString(ARG_ACTION_TAG);
            if (actionTag != null) result.putString(RESULT_ACTION_TAG, actionTag);
            getParentFragmentManager().setFragmentResult(requestKey, result);
        }
        dismiss();
    }

    private void refreshPriceLabels() {
        if (binding == null) return;
        binding.tvPriceLifetime.setText(container.billingBridge.getFormattedPrice(
                PlayBillingBridge.SKU_LIFETIME, PricingCatalog.LABEL_LIFETIME));
        binding.tvPriceAnnual.setText(container.billingBridge.getFormattedPrice(
                PlayBillingBridge.SKU_ANNUAL_PASS, PricingCatalog.LABEL_ANNUAL));
        binding.tvPriceWeekly.setText(container.billingBridge.getFormattedPrice(
                PlayBillingBridge.SKU_WEEKLY_PASS, PricingCatalog.LABEL_WEEKLY));
    }

    private void updateSelectedPlanVisuals() {
        if (binding == null) return;
        stylePlanCard(binding.cardPlanLifetime, PlayBillingBridge.SKU_LIFETIME.equals(selectedSku));
        stylePlanCard(binding.cardPlanAnnual, PlayBillingBridge.SKU_ANNUAL_PASS.equals(selectedSku));
        stylePlanCard(binding.cardPlanWeekly, PlayBillingBridge.SKU_WEEKLY_PASS.equals(selectedSku));
    }

    private void stylePlanCard(View card, boolean selected) {
        card.setBackgroundResource(selected ? R.drawable.bg_card_velvet_selected : R.drawable.bg_card_velvet);
        card.setAlpha(selected ? 1.0f : 0.78f);
    }

    @Override
    public void onCatalogUpdated() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::refreshPriceLabels);
    }

    @Override
    public void onPurchaseSuccess() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), R.string.paywall_unlocked_toast, Toast.LENGTH_SHORT).show();
            notifyUnlockedAndDismiss();
        });
    }

    @Override
    public void onBillingMessage(String message) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), R.string.paywall_play_unavailable, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (container != null) container.billingBridge.setUiListener(null);
        binding = null;
    }
}
