package com.velvet.couplesgame.core.engine;

import android.os.CountDownTimer;

/**
 * Built-in countdown timer helper for timed dares.
 */
public class DareTimerHelper {
    public interface Listener {
        void onTick(int secondsRemaining);
        void onFinish();
    }

    private CountDownTimer timer;

    public void start(int durationSeconds, Listener listener) {
        cancel();
        if (durationSeconds <= 0) return;

        timer = new CountDownTimer(durationSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (listener != null) {
                    listener.onTick((int) Math.ceil(millisUntilFinished / 1000.0));
                }
            }

            @Override
            public void onFinish() {
                if (listener != null) {
                    listener.onFinish();
                }
            }
        };
        timer.start();
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
