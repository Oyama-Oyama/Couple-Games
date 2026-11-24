package com.velvet.couplesgame.ui.common;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class HapticHelper {
    private static final int TONE_DURATION_MS = 130;
    private static final int TONE_VOLUME = 60;
    private static final int VIBRATION_DURATION_MS = 70;

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

    /**
     * 卡片倒计时结束反馈：短促提示音 + 轻震动。
     * 使用 STREAM_NOTIFICATION，遵循系统通知音量与静音设置；
     * 所有失败路径静默降级，绝不影响游戏流程。
     */
    public static void countdownFinished(View view) {
        if (view == null) return;
        Context context = view.getContext();
        if (context == null) return;

        if (!vibrateLightly(context)) {
            // 无震动硬件时回退到系统触感反馈
            confirm(view);
        }
        playCountdownTone(context);
    }

    private static boolean vibrateLightly(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return false;
            // minSdk 26 起 VibrationEffect 全版本可用
            vibrator.vibrate(VibrationEffect.createOneShot(
                    VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void playCountdownTone(Context context) {
        try {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null && audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                return;
            }
            final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS);
            // startTone 是异步的，播放完毕后再释放，避免声音被截断
            new Handler(Looper.getMainLooper()).postDelayed(tone::release, TONE_DURATION_MS + 120L);
        } catch (Exception ignored) {
            // 音频不可用时静默降级
        }
    }
}
