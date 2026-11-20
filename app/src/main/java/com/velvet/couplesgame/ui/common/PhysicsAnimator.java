package com.velvet.couplesgame.ui.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * Native physical & tactile animation toolkit:
 * - Spring snap-back & pop-in
 * - 3D Y-axis tabletop card flip with perspective camera distance
 * - Suspenseful dice tumble with haptic ticks
 * - Inertial FlingAnimation for Lucky Wheel
 */
public final class PhysicsAnimator {
    private PhysicsAnimator() {}

    public static void popIn(View target) {
        target.setScaleX(0.92f);
        target.setScaleY(0.92f);
        SpringAnimation scaleX = new SpringAnimation(target, DynamicAnimation.SCALE_X, 1.0f);
        SpringAnimation scaleY = new SpringAnimation(target, DynamicAnimation.SCALE_Y, 1.0f);
        scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        scaleY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        scaleX.start();
        scaleY.start();
    }

    /**
     * Tactile 3D tabletop card flip: rotates 0 -> 90 deg, swaps card content at midpoint, then lands 270 -> 360 deg.
     */
    public static void flipCard3D(View cardView, Runnable onMidpointSwap) {
        if (cardView == null) return;
        float density = cardView.getResources().getDisplayMetrics().density;
        cardView.setCameraDistance(8000f * density);

        ObjectAnimator firstHalf = ObjectAnimator.ofFloat(cardView, View.ROTATION_Y, 0f, 90f);
        firstHalf.setDuration(150L);
        firstHalf.setInterpolator(new AccelerateInterpolator());
        firstHalf.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onMidpointSwap != null) onMidpointSwap.run();
                cardView.setRotationY(-90f);
                ObjectAnimator secondHalf = ObjectAnimator.ofFloat(cardView, View.ROTATION_Y, -90f, 0f);
                secondHalf.setDuration(180L);
                secondHalf.setInterpolator(new DecelerateInterpolator());
                secondHalf.start();
            }
        });
        firstHalf.start();
    }

    /**
     * Rapid dice tumble suspense animation (420ms) before revealing the final roll value.
     */
    public static ValueAnimator rollDiceSuspense(TextView diceBadge, int finalRoll, Runnable onComplete) {
        ValueAnimator animator = ValueAnimator.ofInt(0, 7);
        animator.setDuration(420L);
        animator.setInterpolator(new DecelerateInterpolator());
        final int[] lastTick = {-1};

        animator.addUpdateListener(anim -> {
            int step = (int) anim.getAnimatedValue();
            if (step != lastTick[0]) {
                lastTick[0] = step;
                int previewFace = (step >= 7) ? finalRoll : ((step * 2 + finalRoll) % 6) + 1;
                diceBadge.setText("🎲 " + previewFace);
                diceBadge.setRotation((step % 2 == 0) ? -7f : 7f);
                HapticHelper.tick(diceBadge);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                diceBadge.setRotation(0f);
                diceBadge.setText("🎲 " + finalRoll);
                popIn(diceBadge);
                if (onComplete != null) onComplete.run();
            }
        });
        animator.start();
        return animator;
    }

    public static void springToPosition(View target, float finalX, float finalY, float finalRotation) {
        SpringAnimation animX = new SpringAnimation(target, DynamicAnimation.TRANSLATION_X, finalX);
        SpringAnimation animY = new SpringAnimation(target, DynamicAnimation.TRANSLATION_Y, finalY);
        SpringAnimation animR = new SpringAnimation(target, DynamicAnimation.ROTATION, finalRotation);

        animX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        animY.getSpring().setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        animR.getSpring().setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

        animX.start();
        animY.start();
        animR.start();
    }

    public static FlingAnimation flingWheel(View wheelView,
                                            float startVelocityDegPerSec,
                                            float sectorAngleDeg,
                                            Runnable onEnd) {
        FlingAnimation fling = new FlingAnimation(wheelView, DynamicAnimation.ROTATION);
        final int[] lastSector = {(int) (wheelView.getRotation() / sectorAngleDeg)};

        fling.setStartVelocity(startVelocityDegPerSec)
                .setFriction(0.65f)
                .addUpdateListener((animation, value, velocity) -> {
                    int currentSector = (int) (value / sectorAngleDeg);
                    if (currentSector != lastSector[0]) {
                        lastSector[0] = currentSector;
                        wheelView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                })
                .addEndListener((animation, canceled, value, velocity) -> {
                    if (!canceled && onEnd != null) onEnd.run();
                });
        fling.start();
        return fling;
    }
}
