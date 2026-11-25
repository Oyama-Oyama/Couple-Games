package com.velvet.couplesgame.ui.drawguess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

/**
 * Pure-Android custom HSV freeform color picker view:
 * - Upper 2D Saturation/Value gradient square
 * - Lower 360-degree Hue spectrum bar
 * Zero third-party dependencies, full touch drag support.
 */
public class ColorPickerView extends View {
    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private static final int[] HUE_COLORS = new int[]{
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
            0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
    };

    private float hue = 340f;
    private float sat = 0.78f;
    private float val = 0.96f;

    private final RectF svRect = new RectF();
    private final RectF hueRect = new RectF();

    private final Paint svPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean draggingSv = false;
    private boolean draggingHue = false;
    private OnColorChangedListener listener;

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        // Software layer ensures ComposeShader (MULTIPLY) renders identically across all GPUs/emulators
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        float d = getResources().getDisplayMetrics().density;

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.2f * d);
        borderPaint.setColor(0x55FFFFFF);

        selectorOuterPaint.setStyle(Paint.Style.STROKE);
        selectorOuterPaint.setStrokeWidth(2.6f * d);
        selectorOuterPaint.setColor(0xFFFFFFFF);

        selectorInnerPaint.setStyle(Paint.Style.STROKE);
        selectorInnerPaint.setStrokeWidth(1.2f * d);
        selectorInnerPaint.setColor(0xAA000000);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
        rebuildSvShader();
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(new float[]{hue, sat, val});
    }

    /** Pure-Java HSV -> ARGB conversion for unit testing without Android Color runtime. */
    public static int hsvToRgbPure(float hueDeg, float saturation, float value) {
        float h = ((hueDeg % 360f) + 360f) % 360f;
        float s = Math.max(0f, Math.min(1f, saturation));
        float v = Math.max(0f, Math.min(1f, value));

        float c = v * s;
        float x = c * (1f - Math.abs(((h / 60f) % 2f) - 1f));
        float m = v - c;

        float r1, g1, b1;
        if (h < 60f) { r1 = c; g1 = x; b1 = 0f; }
        else if (h < 120f) { r1 = x; g1 = c; b1 = 0f; }
        else if (h < 180f) { r1 = 0f; g1 = c; b1 = x; }
        else if (h < 240f) { r1 = 0f; g1 = x; b1 = c; }
        else if (h < 300f) { r1 = x; g1 = 0f; b1 = c; }
        else { r1 = c; g1 = 0f; b1 = x; }

        int r = Math.round((r1 + m) * 255f);
        int g = Math.round((g1 + m) * 255f);
        int b = Math.round((b1 + m) * 255f);
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static String formatHex(int argbColor) {
        return String.format(Locale.US, "#%06X", (0xFFFFFF & argbColor));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        float d = getResources().getDisplayMetrics().density;
        float pad = 6f * d;
        float hueBarH = 26f * d;
        float gap = 14f * d;

        svRect.set(pad, pad, w - pad, Math.max(pad + 40f * d, h - pad - hueBarH - gap));
        hueRect.set(pad, svRect.bottom + gap, w - pad, h - pad);

        huePaint.setShader(new LinearGradient(
                hueRect.left, hueRect.top, hueRect.right, hueRect.top,
                HUE_COLORS, null, Shader.TileMode.CLAMP
        ));
        rebuildSvShader();
    }

    private void rebuildSvShader() {
        if (svRect.width() <= 0f || svRect.height() <= 0f) return;
        int pureHueColor = Color.HSVToColor(new float[]{hue, 1f, 1f});
        Shader satShader = new LinearGradient(
                svRect.left, svRect.top, svRect.right, svRect.top,
                0xFFFFFFFF, pureHueColor, Shader.TileMode.CLAMP
        );
        Shader valShader = new LinearGradient(
                svRect.left, svRect.top, svRect.left, svRect.bottom,
                0xFFFFFFFF, 0xFF000000, Shader.TileMode.CLAMP
        );
        svPaint.setShader(new ComposeShader(satShader, valShader, PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (svRect.width() <= 0f) return;
        float d = getResources().getDisplayMetrics().density;
        float r = 10f * d;

        // 2D Saturation-Value square
        canvas.drawRoundRect(svRect, r, r, svPaint);
        canvas.drawRoundRect(svRect, r, r, borderPaint);

        // SV target ring
        float sx = svRect.left + sat * svRect.width();
        float sy = svRect.top + (1f - val) * svRect.height();
        canvas.drawCircle(sx, sy, 9f * d, selectorOuterPaint);
        canvas.drawCircle(sx, sy, 10.5f * d, selectorInnerPaint);

        // Hue spectrum bar
        float hr = hueRect.height() / 2f;
        canvas.drawRoundRect(hueRect, hr, hr, huePaint);
        canvas.drawRoundRect(hueRect, hr, hr, borderPaint);

        // Hue thumb
        float hx = hueRect.left + (hue / 360f) * hueRect.width();
        float hy = hueRect.centerY();
        canvas.drawCircle(hx, hy, 11f * d, selectorOuterPaint);
        canvas.drawCircle(hx, hy, 12.5f * d, selectorInnerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                if (y <= svRect.bottom + (hueRect.top - svRect.bottom) / 2f) {
                    draggingSv = true;
                    draggingHue = false;
                    updateSvFromTouch(x, y);
                } else {
                    draggingHue = true;
                    draggingSv = false;
                    updateHueFromTouch(x);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (draggingSv) {
                    updateSvFromTouch(x, y);
                } else if (draggingHue) {
                    updateHueFromTouch(x);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingSv = false;
                draggingHue = false;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    performClick();
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.SeekBar.class.getName());
        info.setScrollable(true);
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0f, 360f, hue
        ));
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            hue = (hue + (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 15f : -15f) + 360f) % 360f;
            rebuildSvShader();
            invalidate();
            if (listener != null) listener.onColorChanged(getColor());
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    private void updateSvFromTouch(float x, float y) {
        if (svRect.width() <= 0f || svRect.height() <= 0f) return;
        sat = clamp((x - svRect.left) / svRect.width());
        val = 1f - clamp((y - svRect.top) / svRect.height());
        invalidate();
        if (listener != null) listener.onColorChanged(getColor());
    }

    private void updateHueFromTouch(float x) {
        if (hueRect.width() <= 0f) return;
        hue = clamp((x - hueRect.left) / hueRect.width()) * 360f;
        rebuildSvShader();
        invalidate();
        if (listener != null) listener.onColorChanged(getColor());
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
