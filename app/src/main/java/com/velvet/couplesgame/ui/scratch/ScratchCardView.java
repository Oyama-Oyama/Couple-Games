package com.velvet.couplesgame.ui.scratch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.ui.common.HapticHelper;

/**
 * Hardware-friendly tactile metallic foil scratch-off overlay view.
 * Uses an offscreen ARGB_8888 bitmap erased by PorterDuff.Mode.CLEAR finger strokes
 * and a fast 16x10 cell tracker to fire auto-reveal at >= 42% scratched.
 */
public class ScratchCardView extends View {
    public interface OnScratchListener {
        void onProgress(int percent);
        void onRevealed();
    }

    private static final int GRID_COLS = 16;
    private static final int GRID_ROWS = 10;
    private static final int AUTO_REVEAL_THRESHOLD = 42;

    private Bitmap foilBitmap;
    private Canvas foilCanvas;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint erasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint foilTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path touchPath = new Path();

    private final boolean[][] scratchedGrid = new boolean[GRID_ROWS][GRID_COLS];
    private int scratchedCells = 0;
    private boolean revealed = false;
    private long lastHapticMs = 0L;
    private float lastX;
    private float lastY;

    private AppTheme activeTheme = AppTheme.VELVET_NOIR;
    private String foilPromptText = "";
    private OnScratchListener listener;

    public ScratchCardView(Context context) {
        super(context);
        init();
    }

    public ScratchCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScratchCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeWidth(34f * density);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        foilTextPaint.setTextAlign(Paint.Align.CENTER);
        foilTextPaint.setFakeBoldText(true);
        foilPromptText = getContext().getString(R.string.scratch_foil_overlay_hint);
    }

    public void setOnScratchListener(OnScratchListener listener) {
        this.listener = listener;
    }

    public void applyTheme(AppTheme theme) {
        this.activeTheme = theme != null ? theme : AppTheme.VELVET_NOIR;
        if (getWidth() > 0 && getHeight() > 0 && !revealed) {
            paintFreshFoil();
            invalidate();
        }
    }

    public void resetFoil() {
        revealed = false;
        scratchedCells = 0;
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                scratchedGrid[r][c] = false;
            }
        }
        animate().cancel();
        setAlpha(1f);
        setVisibility(VISIBLE);
        if (getWidth() > 0 && getHeight() > 0) {
            paintFreshFoil();
        }
        invalidate();
    }

    public void revealInstantly() {
        if (revealed) return;
        revealed = true;
        HapticHelper.confirm(this);
        animate().alpha(0f).setDuration(260L).withEndAction(() -> setVisibility(GONE)).start();
        if (listener != null) {
            listener.onProgress(100);
            listener.onRevealed();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        if (foilBitmap != null) {
            foilBitmap.recycle();
        }
        foilBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        foilCanvas = new Canvas(foilBitmap);
        if (!revealed) {
            paintFreshFoil();
        }
    }

    private void paintFreshFoil() {
        if (foilCanvas == null || foilBitmap == null) return;
        int w = foilBitmap.getWidth();
        int h = foilBitmap.getHeight();
        float density = getResources().getDisplayMetrics().density;

        Context ctx = getContext();
        int primary = ContextCompat.getColor(ctx, activeTheme.primaryColorRes);
        int accent = ContextCompat.getColor(ctx, activeTheme.accentColorRes);
        int surface = ContextCompat.getColor(ctx, activeTheme.surfaceColorRes);

        foilCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(
                0f, 0f, w, h,
                new int[]{surface, primary, accent, primary},
                new float[]{0f, 0.35f, 0.68f, 1f},
                Shader.TileMode.CLAMP
        ));
        RectF rect = new RectF(0f, 0f, w, h);
        foilCanvas.drawRoundRect(rect, 18f * density, 18f * density, bgPaint);

        // Subtle diagonal shimmer lines
        Paint shimmer = new Paint(Paint.ANTI_ALIAS_FLAG);
        shimmer.setColor(0x22FFFFFF);
        shimmer.setStrokeWidth(2f * density);
        for (int x = -h; x < w + h; x += Math.round(24f * density)) {
            foilCanvas.drawLine(x, 0, x + h * 0.4f, h, shimmer);
        }

        // Centered metallic label
        foilTextPaint.setColor(0xEEFFFFFF);
        foilTextPaint.setTextSize(16f * density);
        foilCanvas.drawText("✦  " + foilPromptText + "  ✦", w / 2f, h / 2f + 6f * density, foilTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (revealed || foilCanvas == null) return false;
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                touchPath.reset();
                touchPath.moveTo(x, y);
                lastX = x;
                lastY = y;
                scratchAt(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                touchPath.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f);
                foilCanvas.drawPath(touchPath, erasePaint);
                lastX = x;
                lastY = y;
                scratchAt(x, y);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                touchPath.reset();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void scratchAt(float x, float y) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Draw dot for single taps too
        foilCanvas.drawPoint(x, y, erasePaint);

        int col = Math.max(0, Math.min(GRID_COLS - 1, (int) (x / w * GRID_COLS)));
        int row = Math.max(0, Math.min(GRID_ROWS - 1, (int) (y / h * GRID_ROWS)));

        // Mark touched cell + immediate neighbors matching wide brush radius
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = row + dr;
                int nc = col + dc;
                if (nr >= 0 && nr < GRID_ROWS && nc >= 0 && nc < GRID_COLS && !scratchedGrid[nr][nc]) {
                    scratchedGrid[nr][nc] = true;
                    scratchedCells++;
                }
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastHapticMs > 75L) {
            lastHapticMs = now;
            HapticHelper.tick(this);
        }

        int pct = Math.min(100, (scratchedCells * 100) / (GRID_ROWS * GRID_COLS));
        if (listener != null) listener.onProgress(pct);
        invalidate();

        if (pct >= AUTO_REVEAL_THRESHOLD) {
            revealInstantly();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (foilBitmap != null && !foilBitmap.isRecycled()) {
            canvas.drawBitmap(foilBitmap, 0f, 0f, bitmapPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (foilBitmap != null && !foilBitmap.isRecycled()) {
            foilBitmap.recycle();
            foilBitmap = null;
            foilCanvas = null;
        }
    }
}
