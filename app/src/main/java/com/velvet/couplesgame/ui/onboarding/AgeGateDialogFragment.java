package com.velvet.couplesgame.ui.onboarding;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.profile.AgeGateManager;
import com.velvet.couplesgame.databinding.DialogAgeGateBinding;

/**
 * B 项 · 首启 18+ 年龄确认弹窗（架架规格 §2.1~§2.4 / [API-009]）。
 *
 * <p><b>必须早于任何成人向内容呈现</b>：MainActivity 在导航宿主可见之前弹出本弹窗，
 * 确认或拒绝之后才恢复主页渲染。
 *
 * <p><b>软门禁</b>（主理人裁决）：点「我未满 18 岁」→ 就地切换为
 * 「只保留基础玩法」的说明 + 隐私承诺，<b>不锁死 App</b>。
 *
 * <p>文案不含任何身体 / 动作 / 性暗示词（安安 RED-A2 / A7），
 * 隐私说明一律使用否定式（RED-P §3.3 允许词表）。
 */
public class AgeGateDialogFragment extends DialogFragment {

    public static final String TAG = "velvet_age_gate";
    public static final String RESULT_KEY = "age_gate_completed";

    private DialogAgeGateBinding binding;
    private AlertDialog dialog;
    /** 用户是否已选「未满 18 岁」；决定正按钮是「确认成年」还是「关闭软门禁说明」。 */
    private boolean declined;

    public static AgeGateDialogFragment newInstance() {
        return new AgeGateDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAgeGateBinding.inflate(LayoutInflater.from(requireContext()));
        // P0 修复：listener 必须在 create() 前绑定。
        // 传 null 会走 AlertController 默认 auto-dismiss；而 onStart 里 super 之后再加
        // setOnShowListener 也不会补发 SHOW 消息，listener 永不执行 → 首启白屏。
        dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .setPositiveButton(R.string.agegate_confirm, null)
                .setNegativeButton(R.string.agegate_decline, null)
                .setCancelable(false)
                .create();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        declined = ageGateManager().isDeclined();
        if (declined) showDeclinedState();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!declined && !ageGateManager().confirm(false)) {
                showSaveError();
                return;
            }
            setNavHostVisible(true);
            dismissAllowingStateLoss();
            getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
        });
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
            if (!ageGateManager().confirm(true)) {
                showSaveError();
                return;
            }
            declined = true;
            showDeclinedState();
        });
    }

    private void showSaveError() {
        Toast.makeText(requireContext(), R.string.agegate_save_failed, Toast.LENGTH_LONG).show();
    }

    /** 拒绝后：就地切换为「仅保留基础玩法」说明 + 否定式隐私承诺。 */
    private void showDeclinedState() {
        if (binding == null || dialog == null) return;
        binding.tvAgegateBody.setVisibility(View.GONE);
        binding.tvAgegateDeclinedNotice.setVisibility(View.VISIBLE);
        binding.tvAgegateDeclineNote.setVisibility(View.VISIBLE);
        View negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (negative != null) negative.setVisibility(View.GONE);
        // 正按钮已被 onCreateDialog 的 listener 绑定；此处只切 declined 标志改变其行为，
        // 不再 setOnClickListener 覆盖，避免重现 P0。
    }

    /** 恢复主页渲染（导航宿主可见）。 */
    private void setNavHostVisible(boolean visible) {
        if (!isAdded()) return;
        View host = requireActivity().findViewById(R.id.nav_host_fragment);
        if (host != null) host.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private AgeGateManager ageGateManager() {
        return ((CouplesGameApp) requireActivity().getApplication())
                .getContainer().ageGateManager;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
