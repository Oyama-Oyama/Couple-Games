package com.velvet.couplesgame.ui.blindmatch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;

/**
 * Interactive drag-and-swipe card with SpringAnimation snap-back and fly-out physics.
 */
public class SwipeableCardView extends FrameLayout {
    public interface OnSwipeListener {
        void onSwipeProgress(float progress); // -1.0 (PASS) .. 0 .. +1.0 (YES)
        void onSwiped(boolean liked);
    }

    private float downRawX;
    private float downRawY;
    private boolean isDragging;
    private boolean isAnimatingOut;
    private OnSwipeListener swipeListener;

    public SwipeableCardView(@NonNull Context context) {
        super(context);
    }

    public SwipeableCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeableCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    public void resetInstant() {
        animate().cancel();
        isAnimatingOut = false;
        setTranslationX(0f);
        setTranslationY(0f);
        setRotation(0f);
        setAlpha(1f);
        if (swipeListener != null) swipeListener.onSwipeProgress(0f);
        PhysicsAnimator.popIn(this);
    }

    public void triggerSwipe(boolean liked) {
        if (isAnimatingOut) return;
        flyOut(liked);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isAnimatingOut || !isEnabled()) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                isDragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) return false;
                float dx = event.getRawX() - downRawX;
                float dy = (event.getRawY() - downRawY) * 0.35f;
                setTranslationX(dx);
                setTranslationY(dy);
                setRotation(dx / 24f);

                float threshold = Math.max(1f, getWidth() * 0.30f);
                float progress = Math.max(-1f, Math.min(1f, dx / threshold));
                if (swipeListener != null) swipeListener.onSwipeProgress(progress);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isDragging) return false;
                isDragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);

                float finalDx = getTranslationX();
                float swipeThreshold = Math.max(120f, getWidth() * 0.28f);
                if (Math.abs(finalDx) >= swipeThreshold) {
                    flyOut(finalDx > 0);
                } else {
                    PhysicsAnimator.springToPosition(this, 0f, 0f, 0f);
                    if (swipeListener != null) swipeListener.onSwipeProgress(0f);
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void flyOut(boolean liked) {
        isAnimatingOut = true;
        HapticHelper.tick(this);
        float targetX = (liked ? 1f : -1f) * (getWidth() * 1.35f + 220f);
        float targetRot = liked ? 22f : -22f;

        if (swipeListener != null) swipeListener.onSwipeProgress(liked ? 1f : -1f);

        animate()
                .translationX(targetX)
                .rotation(targetRot)
                .alpha(0f)
                .setDuration(240)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animate().setListener(null);
                        isAnimatingOut = false;
                        if (swipeListener != null) {
                            swipeListener.onSwiped(liked);
                        }
                    }
                })
                .start();
    }
}
