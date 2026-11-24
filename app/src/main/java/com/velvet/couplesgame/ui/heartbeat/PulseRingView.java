package com.velvet.couplesgame.ui.heartbeat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.velvet.couplesgame.core.theme.AppTheme;

/**
 * Animated concentric heartbeat pulse ring synced to active AppTheme.
 */
public class PulseRingView extends View {
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private int colorPrimary = 0xFF8C1D40;
    private int colorAccent = 0xFFD4AF37;
    private int colorText = 0xFFF5F0EB;

    private float progress = 0f; // 0..1
    private float beatScale = 1f;
    private boolean bothLocked = false;

    public PulseRingView(Context context) {
        super(context);
        init();
    }

    public PulseRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PulseRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        corePaint.setStyle(Paint.Style.FILL);
        iconPaint.setTextAlign(Paint.Align.CENTER);
        iconPaint.setFakeBoldText(true);
        applyTheme(AppTheme.VELVET_NOIR);
    }

    public void applyTheme(AppTheme theme) {
        AppTheme t = theme != null ? theme : AppTheme.VELVET_NOIR;
        Context ctx = getContext();
        colorPrimary = ContextCompat.getColor(ctx, t.primaryColorRes);
        colorAccent = ContextCompat.getColor(ctx, t.accentColorRes);
        colorText = ContextCompat.getColor(ctx, t.textPrimaryColorRes);
        invalidate();
    }

    public void setPulseState(float progressRatio, float beatScale, boolean bothLocked) {
        this.progress = Math.max(0f, Math.min(1f, progressRatio));
        this.beatScale = beatScale;
        this.bothLocked = bothLocked;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxR = Math.min(cx, cy) - 10f * density;
        if (maxR <= 0f) return;

        // Outer ambient ripple
        ringPaint.setStrokeWidth(2f * density);
        ringPaint.setColor(withAlpha(colorAccent, bothLocked ? 140 : 55));
        canvas.drawCircle(cx, cy, maxR * (0.86f + 0.12f * beatScale), ringPaint);

        // Background track
        float trackR = maxR * 0.76f;
        ringPaint.setStrokeWidth(7f * density);
        ringPaint.setColor(withAlpha(colorPrimary, 80));
        canvas.drawCircle(cx, cy, trackR, ringPaint);

        // Progress arc
        arcBounds.set(cx - trackR, cy - trackR, cx + trackR, cy + trackR);
        progressPaint.setStrokeWidth(7f * density);
        progressPaint.setColor(bothLocked ? colorAccent : colorPrimary);
        canvas.drawArc(arcBounds, -90f, 360f * progress, false, progressPaint);

        // Pulsing velvet core
        float coreR = trackR * (0.64f + 0.08f * beatScale);
        corePaint.setColor(bothLocked ? colorPrimary : withAlpha(colorPrimary, 160));
        canvas.drawCircle(cx, cy, coreR, corePaint);

        iconPaint.setColor(bothLocked ? colorAccent : colorText);
        iconPaint.setTextSize(coreR * 0.72f);
        Paint.FontMetrics fm = iconPaint.getFontMetrics();
        canvas.drawText(bothLocked ? "♥" : "♡", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint);
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
