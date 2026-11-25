package com.velvet.couplesgame.ui.reward;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.reward.RewardVoucher;
import java.util.ArrayList;
import java.util.List;

public class RewardVoucherWalletDialogFragment extends DialogFragment {
    public static final String TAG = "reward_voucher_wallet";

    public static RewardVoucherWalletDialogFragment newInstance() {
        return new RewardVoucherWalletDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AppContainer container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        if (!container.consentRepository.isInitialized()) {
            return new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reward_wallet_title)
                    .setMessage(R.string.reward_wallet_rules_loading)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
        List<RewardVoucher> vouchers = container.rewardVoucherManager.getAvailable(
                container.consentFilter, container.getEffectiveMaxTier());
        List<RewardVoucher> history = new ArrayList<>();
        for (RewardVoucher voucher : container.rewardVoucherManager.getAll()) {
            if (voucher.isRedeemed()) history.add(voucher);
        }
        if (vouchers.isEmpty()) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reward_wallet_title)
                    .setView(createEmptyState())
                    .setPositiveButton(android.R.string.ok, null);
            if (!history.isEmpty()) {
                builder.setNeutralButton(R.string.reward_wallet_history,
                        (dialog, which) -> showHistory(history));
            }
            return builder.create();
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reward_wallet_title)
                .setView(createVoucherList(vouchers, container, false))
                .setNegativeButton(android.R.string.cancel, null);
        if (!history.isEmpty()) {
            builder.setNeutralButton(R.string.reward_wallet_history,
                    (dialog, which) -> showHistory(history));
        }
        return builder.create();
    }

    private void showHistory(List<RewardVoucher> history) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reward_wallet_history)
                .setView(createVoucherList(history, null, true))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private View createEmptyState() {
        TextView empty = new TextView(requireContext());
        empty.setText(R.string.reward_wallet_empty);
        empty.setTextColor(resolveThemeColor(R.attr.velvetTextMuted));
        empty.setTextSize(14f);
        empty.setLineSpacing(0f, 1.2f);
        empty.setPadding(dp(18), dp(18), dp(18), dp(18));
        empty.setBackgroundResource(R.drawable.bg_card_velvet);
        return empty;
    }

    private View createVoucherList(List<RewardVoucher> vouchers, AppContainer container,
                                   boolean history) {
        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(12), dp(16), dp(8));
        for (RewardVoucher voucher : vouchers) {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackgroundResource(R.drawable.bg_card_velvet_glow);
            card.setClickable(true);
            card.setFocusable(true);

            TextView title = new TextView(requireContext());
            title.setText(getString(R.string.reward_wallet_item_fmt,
                    voucher.winnerName, voucher.title));
            title.setTextColor(resolveThemeColor(R.attr.velvetTextPrimary));
            title.setTextSize(16f);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView description = new TextView(requireContext());
            if (history) {
                description.setText(R.string.reward_wallet_redeemed);
            } else {
                description.setText(voucher.description);
            }
            description.setTextColor(resolveThemeColor(R.attr.velvetTextMuted));
            description.setTextSize(13f);
            description.setMaxLines(2);
            description.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            descriptionParams.topMargin = dp(6);
            card.addView(description, descriptionParams);

            card.setOnClickListener(v -> {
                if (history) showHistoryDetails(voucher);
                else if (container != null) showVoucher(container, voucher);
            });
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(10);
            list.addView(card, cardParams);
        }

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setClipToPadding(false);
        scroll.addView(list);
        int contentHeight = dp(30 + vouchers.size() * 88);
        int maxHeight = Math.round(getResources().getDisplayMetrics().heightPixels * 0.58f);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.min(contentHeight, maxHeight)));
        return scroll;
    }

    private void showHistoryDetails(RewardVoucher voucher) {
        String details = getString(R.string.reward_wallet_detail_fmt,
                voucher.winnerName, voucher.title, voucher.description);
        TextView detailView = new TextView(requireContext());
        detailView.setText(details);
        detailView.setTextColor(resolveThemeColor(R.attr.velvetTextPrimary));
        detailView.setTextSize(15f);
        detailView.setLineSpacing(0f, 1.2f);
        detailView.setPadding(dp(18), dp(18), dp(18), dp(18));
        detailView.setBackgroundResource(R.drawable.bg_card_velvet);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reward_wallet_history)
                .setView(detailView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showVoucher(AppContainer container, RewardVoucher voucher) {
        if (!isAdded()) return;
        String details = getString(R.string.reward_wallet_detail_fmt,
                voucher.winnerName, voucher.title, voucher.description);
        int padding = dp(18);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, 0);

        TextView detailView = new TextView(requireContext());
        detailView.setText(details);
        detailView.setTextColor(resolveThemeColor(R.attr.velvetTextPrimary));
        detailView.setTextSize(15f);
        detailView.setLineSpacing(0f, 1.2f);
        detailView.setPadding(padding, padding, padding, padding);
        detailView.setBackgroundResource(R.drawable.bg_card_velvet_glow);
        content.addView(detailView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (container.consentFilter.hasSoftLimit(voucher.tags)) {
            TextView softLimitWarning = new TextView(requireContext());
            softLimitWarning.setText(R.string.soft_limit_warning);
            softLimitWarning.setTextColor(resolveThemeColor(R.attr.velvetAccent));
            softLimitWarning.setTextSize(13f);
            softLimitWarning.setPadding(padding, padding / 2, padding, padding / 2);
            softLimitWarning.setBackgroundResource(R.drawable.bg_card_velvet);
            LinearLayout.LayoutParams warningParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            warningParams.topMargin = padding / 2;
            content.addView(softLimitWarning, warningParams);
        }

        CheckBox consentCheck = new MaterialCheckBox(requireContext());
        consentCheck.setText(R.string.soft_limit_ack);
        consentCheck.setTextColor(resolveThemeColor(R.attr.velvetTextPrimary));
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        checkParams.topMargin = padding / 2;
        content.addView(consentCheck, checkParams);
        ScrollView scrollContent = new ScrollView(requireContext());
        scrollContent.addView(content);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reward_wallet_title)
                .setView(scrollContent)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reward_wallet_redeem, (buttonDialog, which) -> {
                    if (!container.consentRepository.isInitialized()
                            || !container.consentFilter.isAllowed(voucher.tags)
                            || voucher.tier > container.getEffectiveMaxTier()) {
                        Toast.makeText(requireContext(), R.string.reward_wallet_blocked,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    boolean redeemed = container.rewardVoucherManager.redeem(voucher.id);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), redeemed
                                        ? R.string.reward_wallet_redeemed
                                        : R.string.reward_wallet_redeem_failed,
                                redeemed ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                    }
                })
                .create();
        dialog.setOnShowListener(ignored -> {
            android.widget.Button redeemButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (redeemButton == null) return;
            redeemButton.setTextColor(resolveThemeColor(R.attr.velvetAccent));
            redeemButton.setEnabled(consentCheck.isChecked());
            consentCheck.setOnCheckedChangeListener((button, checked) ->
                    redeemButton.setEnabled(checked));
        });
        dialog.show();
    }

    private int resolveThemeColor(int attrId) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (requireContext().getTheme().resolveAttribute(attrId, value, true)) {
            if (value.resourceId != 0) {
                return androidx.core.content.ContextCompat.getColor(requireContext(), value.resourceId);
            }
            return value.data;
        }
        return 0xFFFFFFFF;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
