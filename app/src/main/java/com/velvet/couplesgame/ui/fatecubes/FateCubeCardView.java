package com.velvet.couplesgame.ui.fatecubes;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeFaceDto;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import java.util.List;

/**
 * Tactile 3D X-axis slot-drum cube card for Fate Cubes.
 * Rapidly tumbles through candidate faces with haptic ticks before settling with a spring bounce.
 */
public class FateCubeCardView extends FrameLayout {
    private TextView tvHeader;
    private TextView tvIcon;
    private TextView tvResult;
    private TextView tvTapHint;
    private ValueAnimator tumbleAnimator;
    private boolean isSpinning = false;

    public FateCubeCardView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public FateCubeCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FateCubeCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_fate_cube_card, this, true);
        tvHeader = findViewById(R.id.tv_cube_header);
        tvIcon = findViewById(R.id.tv_cube_icon);
        tvResult = findViewById(R.id.tv_cube_result);
        tvTapHint = findViewById(R.id.tv_cube_hint);
        setBackgroundResource(R.drawable.bg_card_velvet_glow);
        float density = getResources().getDisplayMetrics().density;
        setCameraDistance(9000f * density);
    }

    public void setHeaderText(String header) {
        tvHeader.setText(header);
    }

    public void showIdle(String icon, String prompt, String hint) {
        cancelSpin();
        setBackgroundResource(R.drawable.bg_card_velvet_glow);
        tvIcon.setText(icon);
        tvResult.setText(prompt);
        tvTapHint.setText(hint);
    }

    public boolean isSpinning() {
        return isSpinning;
    }

    /**
     * Spins the cube along a 3D X-axis drum tumble for ~680ms across candidate faces, then locks on targetFace.
     */
    public void spinToFace(List<CubeFaceDto> pool, CubeFaceDto targetFace, String lockedHint, Runnable onSettled) {
        if (targetFace == null) return;
        cancelSpin();
        isSpinning = true;
        setBackgroundResource(R.drawable.bg_card_velvet);
        tvTapHint.setText(R.string.cubes_spinning_hint);

        final int totalTicks = 12;
        final int[] lastTick = {-1};

        tumbleAnimator = ValueAnimator.ofInt(0, totalTicks);
        tumbleAnimator.setDuration(680L);
        tumbleAnimator.setInterpolator(new DecelerateInterpolator(1.6f));
        tumbleAnimator.addUpdateListener(anim -> {
            int step = (int) anim.getAnimatedValue();
            if (step != lastTick[0]) {
                lastTick[0] = step;
                CubeFaceDto preview;
                if (step >= totalTicks || pool == null || pool.isEmpty()) {
                    preview = targetFace;
                } else {
                    preview = pool.get(step % pool.size());
                }
                tvIcon.setText(preview.icon != null ? preview.icon : "♥");
                tvResult.setText(preview.label);
                // Subtle 3D drum rock per tick
                setRotationX((step % 2 == 0) ? -14f : 14f);
                HapticHelper.tick(this);
            }
        });
        final boolean[] canceled = {false};
        tumbleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                canceled[0] = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isSpinning = false;
                setRotationX(0f);
                if (canceled[0] || !isAttachedToWindow()) return;
                tvIcon.setText(targetFace.icon != null ? targetFace.icon : "♥");
                tvResult.setText(targetFace.label);
                tvTapHint.setText(lockedHint);
                setBackgroundResource(R.drawable.bg_card_velvet_selected);
                HapticHelper.confirm(FateCubeCardView.this);
                PhysicsAnimator.popIn(FateCubeCardView.this);
                if (onSettled != null) onSettled.run();
            }
        });
        tumbleAnimator.start();
    }

    public void cancelSpin() {
        if (tumbleAnimator != null) {
            tumbleAnimator.cancel();
            tumbleAnimator = null;
        }
        isSpinning = false;
        setRotationX(0f);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelSpin();
    }
}
