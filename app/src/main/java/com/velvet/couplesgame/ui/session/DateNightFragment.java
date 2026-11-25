package com.velvet.couplesgame.ui.session;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.session.DateNightSessionEngine;
import com.velvet.couplesgame.core.session.DateNightStage;
import com.velvet.couplesgame.databinding.FragmentDateNightBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import com.velvet.couplesgame.ui.common.SoftLimitPromptHelper;
import com.velvet.couplesgame.ui.common.AftercareDialogHelper;

import java.util.List;

/**
 * A 项 · 引导式「约会夜」串场界面（架架规格 §1.7 / §7 [API-008]）。
 *
 * <p>四个阶段：暖场 → 靠近一点 → 心跳 → 慢慢收尾。阶段推进靠情绪密度与节奏，
 * 不靠动作升级（安安 RED-D）。
 *
 * <p><b>PEAK 阶段不设倒计时</b>（主理人裁决）：由双方自行决定何时继续。
 * 因此本页只使用「完成 / 跳过」两个动作，不启动计时器。
 *
 * <p>底部<b>安全暂停条必须保留</b>，与既有 Safe Word / Pause 机制呼应（RED-B3）。
 */
public class DateNightFragment extends Fragment {

    private FragmentDateNightBinding binding;
    private AppContainer container;
    private DateNightSessionEngine engine;

    /** 已完成张数（退出时兜底 flush，沿用 SocialGameFragment 的历史第八轮修法）。 */
    private int completedSinceLastWrite;

    private TaskCard currentCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDateNightBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        engine = new DateNightSessionEngine();

        binding.btnDnPrimary.setOnClickListener(v -> {
            HapticHelper.tick(v);
            if (engine.isFinished()) {
                showAftercareDialog();
            } else {
                onPrimaryClicked();
            }
        });
        binding.btnDnSkip.setOnClickListener(v -> {
            HapticHelper.tick(v);
            engine.markSkipped();
            drawNext();
        });
        binding.tvDnSafe.setOnClickListener(v -> {
            HapticHelper.tick(v);
            showSafePauseDialog();
        });

        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadPool(allCards, container.consentFilter, maxTier);
                engine.startSession(maxTier);
                renderStage();
                drawNext();
            });
        });
    }

    // ------------------------------------------------------------------
    // Stage rendering
    // ------------------------------------------------------------------

    private void renderStage() {
        if (binding == null) return;
        DateNightStage stage = engine.currentStage();
        binding.tvDnStageTitle.setText(stageLabel(stage));
        binding.tvDnStagePrompt.setText(stagePrompt(stage));

        TextView[] segments = {
                binding.tvDnStage1, binding.tvDnStage2,
                binding.tvDnStage3, binding.tvDnStage4
        };
        for (int i = 0; i < segments.length; i++) {
            boolean active = i == stage.index();
            segments[i].setTextColor(active
                    ? resolveAccent()
                    : resolveMuted());
            segments[i].setTypeface(null, active ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }
        // AC-807 #6：阶段推进时收窄段间距（逼近感），不使用红色渐变（避免被读成"强度升级"）。
        applyStageSpacing(stage.index());
    }

    /** 暖场 8dp → 靠近 6dp → 心跳 4dp → 收尾 2dp，420ms 缓出。 */
    private void applyStageSpacing(int stageIndex) {
        TextView[] segments = {
                binding.tvDnStage1, binding.tvDnStage2,
                binding.tvDnStage3, binding.tvDnStage4
        };
        int[] marginsDp = {8, 6, 4, 2};
        int idx = Math.max(0, Math.min(marginsDp.length - 1, stageIndex));
        int target = Math.round(marginsDp[idx] * getResources().getDisplayMetrics().density);
        for (int i = 1; i < segments.length; i++) {
            final TextView segment = segments[i];
            ViewGroup.LayoutParams lp = segment.getLayoutParams();
            if (!(lp instanceof LinearLayout.LayoutParams)) continue;
            final LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
            if (llp.leftMargin == target) continue;
            ValueAnimator anim = ValueAnimator.ofInt(llp.leftMargin, target);
            anim.setDuration(420L);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(a -> {
                llp.leftMargin = (Integer) a.getAnimatedValue();
                segment.requestLayout();
            });
            anim.start();
        }
    }

    private int resolveAccent() {
        return resolveThemeColor(com.velvet.couplesgame.R.attr.velvetAccent);
    }

    private int resolveMuted() {
        return resolveThemeColor(com.velvet.couplesgame.R.attr.velvetTextMuted);
    }

    /** Resolves a velvet theme attribute (12 套主题由 AppTheme 枚举驱动). */
    private int resolveThemeColor(int attr) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (getContext() != null
                && getContext().getTheme().resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return androidx.core.content.ContextCompat.getColor(
                        requireContext(), value.resourceId);
            }
            return value.data;
        }
        return 0xFFFFFFFF;
    }

    private int stageLabel(DateNightStage stage) {
        switch (stage) {
            case DEEPEN:
                return R.string.datenight_stage_deepen;
            case PEAK:
                return R.string.datenight_stage_peak;
            case CLOSING:
                return R.string.datenight_stage_closing;
            case WARM_UP:
            default:
                return R.string.datenight_stage_warmup;
        }
    }

    private int stagePrompt(DateNightStage stage) {
        switch (stage) {
            case DEEPEN:
                return R.string.datenight_deepen_prompt;
            case PEAK:
                return R.string.datenight_peak_prompt;
            case CLOSING:
                return R.string.datenight_closing_prompt;
            case WARM_UP:
            default:
                return R.string.datenight_warmup_prompt;
        }
    }

    // ------------------------------------------------------------------
    // Flow
    // ------------------------------------------------------------------

    private void drawNext() {
        if (binding == null || !isAdded()) return;
        int stageBefore = engine.currentStageIndex();
        TaskCard card = engine.drawNext();
        // 引擎在「某阶段无合规卡」时会在 drawNext() 内部推进阶段（[API-008] 降档逻辑），
        // 此处同步阶段指示，避免 4 段指示与引擎阶段脱节。
        if (engine.currentStageIndex() != stageBefore) {
            renderStage();
        }
        if (card == null) {
            binding.tvDnSoftLimitWarning.setVisibility(View.GONE);
            binding.checkDnSoftLimitAck.setVisibility(View.GONE);
            showFinishedState();
            return;
        }
        currentCard = card;
        SoftLimitPromptHelper.bind(
                container.consentFilter,
                card.tags,
                binding.tvDnSoftLimitWarning,
                binding.checkDnSoftLimitAck,
                binding.btnDnPrimary
        );
        binding.btnDnSkip.setEnabled(true);
        binding.tvDnCardText.setText(substituteNames(card.text));
        // 揭晓仪式：新文案先落位，再以轻柔弹簧入场 + 一次 tick，避免「瞬切」打断私密节奏。
        // 不引入倒计时（PEAK 不计时），haptic 仅用既有 VIBRATE 权限。
        HapticHelper.tick(binding.tvDnCardText);
        PhysicsAnimator.popIn(binding.tvDnCardText);
        binding.tvDnCardMeta.setText(TagDisplayMapper.formatCardMeta(
                requireContext(), card.tier, card.type, card.isProcedural, card.isCustom));
        binding.tvDnProgress.setText(getString(
                R.string.datenight_progress_fmt,
                engine.stageProgress(), engine.stageTotal()));
    }

    private String substituteNames(String text) {
        if (text == null) return "";
        String out = text;
        if (out.contains("{p1}") || out.contains("{p2}")) {
            String p1 = container.profileManager.getPlayerName(true);
            String p2 = container.profileManager.getPlayerName(false);
            out = out.replace("{p1}", p1).replace("{p2}", p2);
        }
        return out;
    }

    private void onPrimaryClicked() {
        if (binding == null) return;
        boolean stageComplete = engine.markDone();
        completedSinceLastWrite++;
        if (stageComplete) {
            HapticHelper.confirm(binding.getRoot());
            if (engine.isFinished()) {
                writeHistory();
                showFinishedState();
                return;
            }
            renderStage();
        }
        drawNext();
    }

    private void showFinishedState() {
        if (binding == null) return;
        binding.tvDnCardText.setText(R.string.datenight_finish);
        binding.tvDnProgress.setText("");
        binding.tvDnSoftLimitWarning.setVisibility(View.GONE);
        binding.checkDnSoftLimitAck.setVisibility(View.GONE);
        binding.btnDnPrimary.setText(R.string.datenight_aftercare_button);
        binding.btnDnPrimary.setEnabled(true);
        binding.btnDnSkip.setEnabled(false);
    }

    /** 持久化到记忆档案（A/B 完成数相同，沿用 DailyReveal / SocialGameFragment 语义）。 */
    private void writeHistory() {
        if (container == null || completedSinceLastWrite <= 0) return;
        container.contentRepository.recordSessionStat(
                DateNightSessionEngine.SESSION_KEY,
                completedSinceLastWrite,
                completedSinceLastWrite,
                0
        );
        completedSinceLastWrite = 0;
    }

    private void showAftercareDialog() {
        AftercareDialogHelper.show(this);
    }

    /** 安全暂停：与既有 Safe Word / Pause 机制保持一致（不得移除）。 */
    private void showSafePauseDialog() {
        if (!isAdded()) return;
        String safeWord = container.profileManager.getSafeWord();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.safe_pause_title)
                .setMessage(getString(R.string.safe_pause_message, safeWord))
                .setPositiveButton(R.string.safe_pause_resume, null)
                .setNeutralButton(R.string.safe_pause_boundaries, (d, w) -> {
                    if (isAdded()) {
                        androidx.navigation.Navigation.findNavController(requireView())
                                .navigate(R.id.nav_consent);
                    }
                })
                .setNegativeButton(R.string.safe_pause_exit, (d, w) -> {
                    if (isAdded() && getActivity() != null) {
                        getActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        // 兜底 flush：提前退出也要记录已完成的张数
        if (container != null) writeHistory();
        binding = null;
        super.onDestroyView();
    }
}
