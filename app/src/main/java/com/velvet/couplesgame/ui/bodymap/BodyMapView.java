package com.velvet.couplesgame.ui.bodymap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BodyMapZoneDto;
import com.velvet.couplesgame.core.theme.AppTheme;
import java.util.List;

/**
 * Tasteful, minimalist rose-gold/velvet artistic silhouette with tappable sensory hotspot pins.
 */
public class BodyMapView extends View {
    public interface OnZoneTapListener {
        void onZoneTap(BodyMapZoneDto zone);
    }

    private BodyMapEngine engine;
    private AppTheme activeTheme = AppTheme.VELVET_NOIR;
    private OnZoneTapListener listener;

    private int colorSurface = 0xFF1D1820;
    private int colorPrimary = 0xFF8C1D40;
    private int colorAccent = 0xFFD4AF37;
    private int colorText = 0xFFF5F0EB;
    private int colorMuted = 0xFFA69BA8;

    private final Paint silhouettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path silhouettePath = new Path();
    private final RectF frameRect = new RectF();
    private ExploreByTouchHelper accessibilityHelper;

    public BodyMapView(Context context) {
        super(context);
        init();
    }

    public BodyMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BodyMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        accessibilityHelper = new ExploreByTouchHelper(this) {
            @Override
            protected int getVirtualViewAt(float x, float y) {
                if (engine == null) return INVALID_ID;
                float hitRadius = 32f * getResources().getDisplayMetrics().density;
                float bestDistanceSquared = hitRadius * hitRadius;
                int nearestIndex = INVALID_ID;
                List<BodyMapZoneDto> zones = engine.getZones();
                for (int i = 0; i < zones.size(); i++) {
                    BodyMapZoneDto zone = zones.get(i);
                    float dx = x - zone.nx * getWidth();
                    float dy = y - zone.ny * getHeight();
                    float distanceSquared = dx * dx + dy * dy;
                    if (distanceSquared <= bestDistanceSquared) {
                        bestDistanceSquared = distanceSquared;
                        nearestIndex = i;
                    }
                }
                return nearestIndex;
            }

            @Override
            protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
                if (engine == null) return;
                for (int i = 0; i < engine.getZones().size(); i++) {
                    virtualViewIds.add(i);
                }
            }

            @Override
            protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
                BodyMapZoneDto zone = getZone(virtualViewId);
                if (zone != null) event.setContentDescription(zone.label);
            }

            @Override
            protected void onPopulateNodeForVirtualView(
                    int virtualViewId, AccessibilityNodeInfoCompat node) {
                BodyMapZoneDto zone = getZone(virtualViewId);
                if (zone == null) {
                    node.setContentDescription("");
                    return;
                }
                node.setClassName("android.widget.Button");
                node.setContentDescription(zone.label);
                node.setClickable(true);
                node.setFocusable(true);
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                node.setSelected(engine.getPhase() == BodyMapEngine.Phase.SET_TRAPS
                        ? engine.isSecretHotSpot(zone.id)
                        : engine.isExplored(zone.id));

                float density = getResources().getDisplayMetrics().density;
                float centerX = zone.nx * getWidth();
                float centerY = zone.ny * getHeight();
                int halfTarget = Math.round(24f * density);
                node.setBoundsInParent(new Rect(
                        Math.round(centerX) - halfTarget,
                        Math.round(centerY) - halfTarget,
                        Math.round(centerX) + halfTarget,
                        Math.round(centerY) + halfTarget
                ));
            }

            @Override
            protected boolean onPerformActionForVirtualView(
                    int virtualViewId, int action, Bundle arguments) {
                BodyMapZoneDto zone = getZone(virtualViewId);
                if (action != AccessibilityNodeInfoCompat.ACTION_CLICK || zone == null || listener == null) {
                    return false;
                }
                listener.onZoneTap(zone);
                invalidate();
                invalidateRoot();
                return true;
            }

            private BodyMapZoneDto getZone(int virtualViewId) {
                if (engine == null || virtualViewId < 0 || virtualViewId >= engine.getZones().size()) {
                    return null;
                }
                return engine.getZones().get(virtualViewId);
            }
        };
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper);
        silhouettePaint.setStyle(Paint.Style.FILL);
        outlinePaint.setStyle(Paint.Style.STROKE);
        pinFillPaint.setStyle(Paint.Style.FILL);
        pinRingPaint.setStyle(Paint.Style.STROKE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        applyTheme(AppTheme.VELVET_NOIR);
    }

    public void bindEngine(BodyMapEngine engine) {
        this.engine = engine;
        invalidate();
        accessibilityHelper.invalidateRoot();
    }

    public void setOnZoneTapListener(OnZoneTapListener listener) {
        this.listener = listener;
    }

    public void applyTheme(AppTheme theme) {
        this.activeTheme = theme != null ? theme : AppTheme.VELVET_NOIR;
        Context ctx = getContext();
        colorSurface = ContextCompat.getColor(ctx, activeTheme.surfaceColorRes);
        colorPrimary = ContextCompat.getColor(ctx, activeTheme.primaryColorRes);
        colorAccent = ContextCompat.getColor(ctx, activeTheme.accentColorRes);
        colorText = ContextCompat.getColor(ctx, activeTheme.textPrimaryColorRes);
        colorMuted = ContextCompat.getColor(ctx, activeTheme.textMutedColorRes);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float pad = 8f * density;

        // Card container
        frameRect.set(pad, pad, w - pad, h - pad);
        silhouettePaint.setColor(colorSurface);
        canvas.drawRoundRect(frameRect, 18f * density, 18f * density, silhouettePaint);
        outlinePaint.setStrokeWidth(1.2f * density);
        outlinePaint.setColor(withAlpha(colorAccent, 80));
        canvas.drawRoundRect(frameRect, 18f * density, 18f * density, outlinePaint);

        // Elegant abstract silhouette (head + neck + graceful torso + hips)
        drawArtisticSilhouette(canvas, w, h, density);

        if (engine == null) return;
        List<BodyMapZoneDto> zones = engine.getZones();
        boolean settingPhase = (engine.getPhase() == BodyMapEngine.Phase.SET_TRAPS);
        float pinR = 16f * density;
        labelPaint.setTextSize(10.5f * density);

        for (BodyMapZoneDto z : zones) {
            float cx = z.nx * w;
            float cy = z.ny * h;

            boolean isSelectedHot = settingPhase && engine.isSecretHotSpot(z.id);
            boolean isExplored = !settingPhase && engine.isExplored(z.id);
            boolean isFoundHot = !settingPhase && engine.isFoundHotSpot(z.id);

            int fill;
            int stroke;
            String glyph;
            if (isSelectedHot || isFoundHot) {
                fill = colorPrimary;
                stroke = colorAccent;
                glyph = "🔥";
            } else if (isExplored) {
                fill = withAlpha(colorAccent, 70);
                stroke = colorAccent;
                glyph = "✓";
            } else {
                fill = withAlpha(colorPrimary, 110);
                stroke = withAlpha(colorAccent, 160);
                glyph = "♥";
            }

            // Outer glow
            pinFillPaint.setColor(withAlpha(fill, 70));
            canvas.drawCircle(cx, cy, pinR * 1.32f, pinFillPaint);

            pinFillPaint.setColor(fill);
            canvas.drawCircle(cx, cy, pinR, pinFillPaint);

            pinRingPaint.setStrokeWidth((isSelectedHot || isFoundHot ? 2.2f : 1.3f) * density);
            pinRingPaint.setColor(stroke);
            canvas.drawCircle(cx, cy, pinR, pinRingPaint);

            labelPaint.setColor(colorText);
            Paint.FontMetrics fm = labelPaint.getFontMetrics();
            canvas.drawText(glyph, cx, cy - (fm.ascent + fm.descent) / 2f, labelPaint);

            // Zone caption pill directly below pin
            labelPaint.setColor(isSelectedHot || isFoundHot ? colorAccent : colorText);
            float labelY = cy + pinR + 12f * density;
            if (labelY > h - 4f * density) {
                labelY = cy - pinR - 6f * density;
            }
            float labelWidth = labelPaint.measureText(z.label);
            float labelX = cx;
            if (z.nx < 0.46f) {
                labelPaint.setTextAlign(Paint.Align.RIGHT);
                labelX = Math.max(pad + labelWidth, cx - pinR - 4f * density);
            } else if (z.nx > 0.54f) {
                labelPaint.setTextAlign(Paint.Align.LEFT);
                labelX = Math.min(w - pad - labelWidth, cx + pinR + 4f * density);
            }
            canvas.drawText(z.label, labelX, labelY, labelPaint);
            labelPaint.setTextAlign(Paint.Align.CENTER);
        }
    }

    private void drawArtisticSilhouette(Canvas canvas, float w, float h, float density) {
        float cx = w * 0.50f;
        silhouettePaint.setColor(withAlpha(colorPrimary, 36));
        outlinePaint.setStrokeWidth(1.5f * density);
        outlinePaint.setColor(withAlpha(colorAccent, 65));

        // Head oval
        RectF head = new RectF(cx - w * 0.11f, h * 0.05f, cx + w * 0.11f, h * 0.21f);
        canvas.drawOval(head, silhouettePaint);
        canvas.drawOval(head, outlinePaint);

        // Neck + shoulders + hourglass waist + hips
        silhouettePath.reset();
        silhouettePath.moveTo(cx - w * 0.045f, h * 0.21f);
        silhouettePath.lineTo(cx - w * 0.055f, h * 0.28f);
        silhouettePath.quadTo(cx - w * 0.24f, h * 0.30f, cx - w * 0.26f, h * 0.42f);
        silhouettePath.quadTo(cx - w * 0.17f, h * 0.54f, cx - w * 0.18f, h * 0.60f);
        silhouettePath.quadTo(cx - w * 0.24f, h * 0.72f, cx - w * 0.18f, h * 0.92f);
        silhouettePath.lineTo(cx + w * 0.18f, h * 0.92f);
        silhouettePath.quadTo(cx + w * 0.24f, h * 0.72f, cx + w * 0.18f, h * 0.60f);
        silhouettePath.quadTo(cx + w * 0.17f, h * 0.54f, cx + w * 0.26f, h * 0.42f);
        silhouettePath.quadTo(cx + w * 0.24f, h * 0.30f, cx + w * 0.055f, h * 0.28f);
        silhouettePath.lineTo(cx + w * 0.045f, h * 0.21f);
        silhouettePath.close();

        canvas.drawPath(silhouettePath, silhouettePaint);
        canvas.drawPath(silhouettePath, outlinePaint);

        outlinePaint.setStrokeWidth(10f * density);
        outlinePaint.setStrokeCap(Paint.Cap.ROUND);
        outlinePaint.setColor(withAlpha(colorPrimary, 70));
        canvas.drawLine(cx - w * 0.21f, h * 0.34f, cx - w * 0.34f, h * 0.52f, outlinePaint);
        canvas.drawLine(cx + w * 0.21f, h * 0.34f, cx + w * 0.34f, h * 0.52f, outlinePaint);
        canvas.drawLine(cx - w * 0.09f, h * 0.78f, cx - w * 0.15f, h * 0.98f, outlinePaint);
        canvas.drawLine(cx + w * 0.09f, h * 0.78f, cx + w * 0.15f, h * 0.98f, outlinePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || engine == null || listener == null) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        float x = event.getX();
        float y = event.getY();
        float w = getWidth();
        float h = getHeight();
        float hitRadius = 32f * getResources().getDisplayMetrics().density;

        BodyMapZoneDto nearest = null;
        float bestDistSq = hitRadius * hitRadius;
        for (BodyMapZoneDto z : engine.getZones()) {
            float dx = x - z.nx * w;
            float dy = y - z.ny * h;
            float distSq = dx * dx + dy * dy;
            if (distSq <= bestDistSq) {
                bestDistSq = distSq;
                nearest = z;
            }
        }
        if (nearest != null) {
            performClick();
            listener.onZoneTap(nearest);
            invalidate();
            accessibilityHelper.invalidateRoot();
        }
        return true;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
