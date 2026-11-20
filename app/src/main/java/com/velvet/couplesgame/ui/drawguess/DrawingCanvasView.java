package com.velvet.couplesgame.ui.drawguess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.velvet.couplesgame.core.theme.AppTheme;
import java.util.ArrayList;
import java.util.List;

/**
 * Smooth quadratic-Bezier finger drawing canvas with color selection, stroke width, undo, and clear.
 */
public class DrawingCanvasView extends View {
    private static class Stroke {
        final Path path;
        final int color;
        final float widthPx;

        Stroke(Path path, int color, float widthPx) {
            this.path = path;
            this.color = color;
            this.widthPx = widthPx;
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private final Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Path activePath;
    private float lastX;
    private float lastY;

    private int canvasBgColor = 0xFF1D1820;
    private int borderColor = 0x55D4AF37;
    private int currentBrushColor = 0xFFF5F0EB;
    private float currentStrokeWidthDp = 5f;
    private boolean drawingEnabled = true;

    public DrawingCanvasView(Context context) {
        super(context);
        init();
    }

    public DrawingCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingCanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);

        borderPaint.setStyle(Paint.Style.STROKE);
        applyTheme(AppTheme.VELVET_NOIR);
    }

    public void applyTheme(AppTheme theme) {
        AppTheme t = theme != null ? theme : AppTheme.VELVET_NOIR;
        Context ctx = getContext();
        canvasBgColor = ContextCompat.getColor(ctx, t.surfaceColorRes);
        int accent = ContextCompat.getColor(ctx, t.accentColorRes);
        borderColor = (accent & 0x00FFFFFF) | 0x66000000;
        currentBrushColor = ContextCompat.getColor(ctx, t.textPrimaryColorRes);
        invalidate();
    }

    public void setBrushColor(int color) {
        this.currentBrushColor = color;
    }

    public void setStrokeWidthDp(float dp) {
        this.currentStrokeWidthDp = dp;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.drawingEnabled = enabled;
    }

    public boolean undo() {
        if (strokes.isEmpty()) return false;
        strokes.remove(strokes.size() - 1);
        invalidate();
        return true;
    }

    public void clear() {
        strokes.clear();
        activePath = null;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!drawingEnabled) return false;
        float x = event.getX();
        float y = event.getY();
        float density = getResources().getDisplayMetrics().density;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                activePath = new Path();
                activePath.moveTo(x, y);
                strokes.add(new Stroke(activePath, currentBrushColor, currentStrokeWidthDp * density));
                lastX = x;
                lastY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (activePath == null) return false;
                float midX = (x + lastX) / 2f;
                float midY = (y + lastY) / 2f;
                activePath.quadTo(lastX, lastY, midX, midY);
                lastX = x;
                lastY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activePath != null) {
                    activePath.lineTo(x, y);
                    activePath = null;
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        canvas.drawColor(canvasBgColor);

        for (Stroke stroke : strokes) {
            drawPaint.setColor(stroke.color);
            drawPaint.setStrokeWidth(stroke.widthPx);
            canvas.drawPath(stroke.path, drawPaint);
        }

        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(1.5f * density);
        canvas.drawRect(1f, 1f, getWidth() - 1f, getHeight() - 1f, borderPaint);
    }
}
