package com.velvet.couplesgame.ui.common;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class HapticHelper {
    private HapticHelper() {}

    public static void tick(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    public static void confirm(View view) {
        if (view == null) return;
        int constant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.CONFIRM
                : HapticFeedbackConstants.LONG_PRESS;
        view.performHapticFeedback(constant);
    }
}
