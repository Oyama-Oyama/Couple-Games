package com.velvet.couplesgame.ui.common;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.engine.DareTimerHelper;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;

public final class TaskPromptDialogHelper {

    public interface Callback {
        void onApproved();
        void onSecondaryAction();
        default void onSkipped() {}
    }

    private TaskPromptDialogHelper() {}

    public static void show(Fragment fragment,
                            AppContainer container,
                            DareTimerHelper dareTimer,
                            boolean actingPlayerA,
                            TaskCard initialCard,
                            String customBadge,
                            String secondaryButtonLabel,
                            Callback callback) {
        if (!fragment.isAdded() || initialCard == null) return;

        View view = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_board_task, null);
        TextView tvMeta = view.findViewById(R.id.tv_dialog_task_meta);
        TextView tvBody = view.findViewById(R.id.tv_dialog_task_text);
        TextView tvTimer = view.findViewById(R.id.tv_dialog_timer);
        MaterialButton btnTimer = view.findViewById(R.id.btn_dialog_timer);
        MaterialButton btnSkip = view.findViewById(R.id.btn_dialog_skip);
        MaterialButton btnFav = view.findViewById(R.id.btn_dialog_fav);
        MaterialButton btnApprove = view.findViewById(R.id.btn_dialog_approve);
        MaterialButton btnSecondary = view.findViewById(R.id.btn_dialog_redo);

        if (secondaryButtonLabel != null && !secondaryButtonLabel.isEmpty()) {
            btnSecondary.setText(secondaryButtonLabel);
        }

        final TaskCard[] activeCard = {initialCard};
        String actorName = container.profileManager.getPlayerName(actingPlayerA);

        Runnable bindCardUi = () -> {
            dareTimer.cancel();
            TaskCard c = activeCard[0];
            String baseMeta = TagDisplayMapper.formatCardMeta(
                    fragment.requireContext(), c.tier, c.type, c.isProcedural, c.isCustom
            );
            String badge = (customBadge != null && !customBadge.isEmpty()) ? " · " + customBadge : "";
            tvMeta.setText(actorName + " · " + baseMeta + badge);
            tvBody.setText(c.text);
            tvTimer.setVisibility(View.GONE);
            btnFav.setText(R.string.btn_favorite);

            container.contentRepository.isFavoriteAsync(c.id, isSaved -> {
                if (!fragment.isAdded()) return;
                fragment.requireActivity().runOnUiThread(() -> {
                    if (activeCard[0].id.equals(c.id)) {
                        btnFav.setText(isSaved ? R.string.btn_favorited : R.string.btn_favorite);
                    }
                });
            });

            if (c.durationSec > 0) {
                btnTimer.setVisibility(View.VISIBLE);
                btnTimer.setText(fragment.getString(R.string.btn_start_timer, c.durationSec));
            } else {
                btnTimer.setVisibility(View.GONE);
            }
            btnSkip.setText(fragment.getString(
                    R.string.btn_skip_card,
                    container.taskEngine.getSkipsLeft(actingPlayerA)
            ));
        };

        bindCardUi.run();

        Dialog dialog = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setView(view)
                .setCancelable(false)
                .create();

        btnTimer.setOnClickListener(v -> {
            TaskCard c = activeCard[0];
            if (c.durationSec <= 0) return;
            tvTimer.setVisibility(View.VISIBLE);
            dareTimer.start(c.durationSec, new DareTimerHelper.Listener() {
                @Override
                public void onTick(int secondsRemaining) {
                    if (fragment.isAdded()) {
                        tvTimer.setText(fragment.getString(R.string.timer_seconds_fmt, secondsRemaining));
                    }
                }

                @Override
                public void onFinish() {
                    if (fragment.isAdded()) {
                        HapticHelper.confirm(view);
                        tvTimer.setText(R.string.timer_done);
                    }
                }
            });
        });

        btnFav.setOnClickListener(v -> container.contentRepository.toggleFavorite(activeCard[0], saved -> {
            if (!fragment.isAdded()) return;
            fragment.requireActivity().runOnUiThread(() ->
                    btnFav.setText(saved ? R.string.btn_favorited : R.string.btn_favorite)
            );
        }));

        btnSkip.setOnClickListener(v -> {
            if (!container.taskEngine.consumeSkip(actingPlayerA)) {
                Toast.makeText(fragment.requireContext(), R.string.msg_no_skips, Toast.LENGTH_SHORT).show();
                return;
            }
            HapticHelper.tick(v);
            if (callback != null) callback.onSkipped();
            container.contentRepository.loadMergedPoolAsync((allCards, mutualMatches) -> {
                int cap = Math.min(activeCard[0].tier, container.entitlementManager.getSelectedMaxTier());
                TaskCard replacement = container.taskEngine.drawNext(allCards, mutualMatches, cap, null, true);
                if (!fragment.isAdded()) return;
                fragment.requireActivity().runOnUiThread(() -> {
                    if (replacement != null) {
                        PhysicsAnimator.flipCard3D(view, () -> {
                            activeCard[0] = replacement;
                            bindCardUi.run();
                        });
                    }
                });
            });
        });

        btnApprove.setOnClickListener(v -> {
            dareTimer.cancel();
            HapticHelper.confirm(v);
            dialog.dismiss();
            if (callback != null) callback.onApproved();
        });

        btnSecondary.setOnClickListener(v -> {
            dareTimer.cancel();
            HapticHelper.tick(v);
            dialog.dismiss();
            if (callback != null) callback.onSecondaryAction();
        });

        dialog.show();
    }
}
