package com.velvet.couplesgame.ui.wheel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.velvet.couplesgame.core.theme.AppTheme;
import java.util.Collections;
import java.util.List;

/**
 * Custom Canvas Lucky Wheel synchronized with all 5 sensual AppThemes.
 * Sector 0 starts at -90 deg (12 o'clock) and sweeps clockwise so rotation math matches LuckyWheelEngine.
 */
public class WheelView extends View {
    private List<WheelSector> sectors = Collections.emptyList();
    private AppTheme activeTheme = AppTheme.VELVET_NOIR;

    private int colorBg;
    private int colorSurface;
    private int colorPrimary;
    private int colorAccent;
    private int colorTextPrimary;

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public WheelView(Context context) {
        super(context);
        init();
    }

    public WheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        slicePaint.setStyle(Paint.Style.FILL);
        dividerPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStyle(Paint.Style.STROKE);

        labelPaint.setTextAlign(Paint.Align.RIGHT);
        labelPaint.setFakeBoldText(true);

        hubFillPaint.setStyle(Paint.Style.FILL);
        hubIconPaint.setTextAlign(Paint.Align.CENTER);
        hubIconPaint.setFakeBoldText(true);

        applyTheme(AppTheme.VELVET_NOIR);
    }

    public void setSectors(List<WheelSector> newSectors) {
        this.sectors = newSectors != null ? newSectors : Collections.emptyList();
        invalidate();
    }

    public int getSectorCount() {
        return sectors.size();
    }

    public void applyTheme(AppTheme theme) {
        this.activeTheme = theme != null ? theme : AppTheme.VELVET_NOIR;
        Context ctx = getContext();
        colorBg = ContextCompat.getColor(ctx, activeTheme.bgColorRes);
        colorSurface = ContextCompat.getColor(ctx, activeTheme.surfaceColorRes);
        colorPrimary = ContextCompat.getColor(ctx, activeTheme.primaryColorRes);
        colorAccent = ContextCompat.getColor(ctx, activeTheme.accentColorRes);
        colorTextPrimary = ContextCompat.getColor(ctx, activeTheme.textPrimaryColorRes);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int size = width;
        if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            size = Math.min(width, heightSize);
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (sectors.isEmpty()) return;

        float density = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 10f * density;
        if (radius <= 0f) return;

        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);
        float sweepAngle = 360f / sectors.size();

        dividerPaint.setStrokeWidth(1.5f * density);
        dividerPaint.setColor(withAlpha(colorAccent, 130));
        labelPaint.setTextSize(12.5f * density);

        for (int i = 0; i < sectors.size(); i++) {
            WheelSector sector = sectors.get(i);
            float startAngle = -90f + i * sweepAngle;

            slicePaint.setColor(resolveSliceColor(i, sector));
            canvas.drawArc(oval, startAngle, sweepAngle, true, slicePaint);
            canvas.drawArc(oval, startAngle, sweepAngle, true, dividerPaint);

            // Draw crisp radial text along sector bisector
            float midAngle = startAngle + sweepAngle / 2f;
            canvas.save();
            canvas.rotate(midAngle, cx, cy);
            labelPaint.setColor(sector.desireWildcard ? colorAccent : colorTextPrimary);
            Paint.FontMetrics fm = labelPaint.getFontMetrics();
            float textY = cy - (fm.ascent + fm.descent) / 2f;
            String displayLabel = fitLabel(sector.label, radius * 0.58f);
            canvas.drawText(displayLabel, cx + radius - 16f * density, textY, labelPaint);
            canvas.restore();
        }

        // Outer metallic accent rim
        ringPaint.setStrokeWidth(4f * density);
        ringPaint.setColor(colorAccent);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        // Decorative studs around outer rim
        hubFillPaint.setColor(colorTextPrimary);
        for (int i = 0; i < sectors.size(); i++) {
            double rad = Math.toRadians(-90f + i * sweepAngle);
            float sx = cx + (float) (Math.cos(rad) * radius);
            float sy = cy + (float) (Math.sin(rad) * radius);
            canvas.drawCircle(sx, sy, 3.2f * density, hubFillPaint);
        }

        // Center velvet hub
        float hubRadius = radius * 0.19f;
        hubFillPaint.setColor(colorBg);
        canvas.drawCircle(cx, cy, hubRadius, hubFillPaint);

        ringPaint.setStrokeWidth(2.4f * density);
        ringPaint.setColor(colorAccent);
        canvas.drawCircle(cx, cy, hubRadius, ringPaint);

        hubIconPaint.setColor(colorAccent);
        hubIconPaint.setTextSize(hubRadius * 0.85f);
        Paint.FontMetrics hfm = hubIconPaint.getFontMetrics();
        canvas.drawText("♥", cx, cy - (hfm.ascent + hfm.descent) / 2f, hubIconPaint);
    }

    private String fitLabel(String label, float maxWidth) {
        if (label == null || labelPaint.measureText(label) <= maxWidth) return label == null ? "" : label;
        String ellipsis = "…";
        int end = label.length();
        while (end > 0) {
            end -= Character.charCount(label.codePointBefore(end));
            String candidate = label.substring(0, end) + ellipsis;
            if (labelPaint.measureText(candidate) <= maxWidth) return candidate;
        }
        return ellipsis;
    }

    private int resolveSliceColor(int idx, WheelSector sector) {
        if (sector.desireWildcard) {
            return blend(colorPrimary, colorAccent, 0.25f);
        }
        int mod = idx % 3;
        if (mod == 0) return colorPrimary;
        if (mod == 1) return colorSurface;
        return blend(colorSurface, colorPrimary, 0.45f);
    }

    private int blend(int from, int to, float ratio) {
        float inv = 1f - ratio;
        return Color.rgb(
                Math.round(Color.red(from) * inv + Color.red(to) * ratio),
                Math.round(Color.green(from) * inv + Color.green(to) * ratio),
                Math.round(Color.blue(from) * inv + Color.blue(to) * ratio)
        );
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
