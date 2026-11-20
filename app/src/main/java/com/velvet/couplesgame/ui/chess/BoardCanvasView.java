package com.velvet.couplesgame.ui.chess;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.ui.common.HapticHelper;
import java.util.Collections;
import java.util.List;

/**
 * Custom 36-tile serpentine tabletop board dynamically rendered with any of the 5 sensual AppThemes.
 * Supports smooth tile-by-tile parabolic hopping animations with haptic ticks.
 */
public class BoardCanvasView extends View {
    private List<BoardTile> tiles = Collections.emptyList();
    private AppTheme activeTheme = AppTheme.VELVET_NOIR;

    private int colorBg;
    private int colorSurface;
    private int colorPrimary;
    private int colorAccent;
    private int colorTextPrimary;
    private int colorTextMuted;

    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF boardRect = new RectF();
    private final RectF tileRect = new RectF();
    private final Path serpentinePath = new Path();

    private float animIndexA = 0f;
    private float animIndexB = 0f;
    private float hopLiftA = 0f;
    private float hopLiftB = 0f;
    private int highlightedTile = 0;
    private String tokenLabelA = "A";
    private String tokenLabelB = "B";

    private ValueAnimator activeAnimator;

    public BoardCanvasView(Context context) {
        super(context);
        init();
    }

    public BoardCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoardCanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        framePaint.setStyle(Paint.Style.FILL);

        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);

        tileFillPaint.setStyle(Paint.Style.FILL);
        tileStrokePaint.setStyle(Paint.Style.STROKE);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        badgePaint.setTextAlign(Paint.Align.CENTER);
        badgePaint.setFakeBoldText(true);

        tokenPaint.setStyle(Paint.Style.FILL);
        tokenRingPaint.setStyle(Paint.Style.STROKE);

        tokenLabelPaint.setTextAlign(Paint.Align.CENTER);
        tokenLabelPaint.setFakeBoldText(true);

        applyTheme(AppTheme.VELVET_NOIR);
    }

    public void setTokenLabels(String labelA, String labelB) {
        this.tokenLabelA = (labelA != null && !labelA.isEmpty()) ? labelA : "A";
        this.tokenLabelB = (labelB != null && !labelB.isEmpty()) ? labelB : "B";
        invalidate();
    }

    public void setBoardTiles(List<BoardTile> boardTiles) {
        this.tiles = boardTiles != null ? boardTiles : Collections.emptyList();
        invalidate();
    }

    /** Dynamically switches the entire board canvas palette to any of the 5 AppThemes. */
    public void applyTheme(AppTheme theme) {
        this.activeTheme = theme != null ? theme : AppTheme.VELVET_NOIR;
        Context ctx = getContext();
        colorBg = ContextCompat.getColor(ctx, activeTheme.bgColorRes);
        colorSurface = ContextCompat.getColor(ctx, activeTheme.surfaceColorRes);
        colorPrimary = ContextCompat.getColor(ctx, activeTheme.primaryColorRes);
        colorAccent = ContextCompat.getColor(ctx, activeTheme.accentColorRes);
        colorTextPrimary = ContextCompat.getColor(ctx, activeTheme.textPrimaryColorRes);
        colorTextMuted = ContextCompat.getColor(ctx, activeTheme.textMutedColorRes);
        invalidate();
    }

    public void syncPositionsInstant(int posA, int posB) {
        cancelAnimation();
        this.animIndexA = posA;
        this.animIndexB = posB;
        this.hopLiftA = 0f;
        this.hopLiftB = 0f;
        invalidate();
    }

    public void animateTokenMove(boolean isPlayerA, int fromIdx, int toIdx, Runnable onEnd) {
        cancelAnimation();
        highlightedTile = toIdx;

        int steps = Math.max(1, Math.abs(toIdx - fromIdx));
        long durationMs = Math.min(1150L, 190L * steps);
        final int[] lastStepTick = {fromIdx};

        activeAnimator = ValueAnimator.ofFloat(fromIdx, toIdx);
        activeAnimator.setDuration(durationMs);
        activeAnimator.setInterpolator(new OvershootInterpolator(0.45f));
        activeAnimator.addUpdateListener(anim -> {
            float val = (float) anim.getAnimatedValue();
            float clamped = Math.max(0f, Math.min(CoupleBoardEngine.FINISH_INDEX, val));
            float frac = clamped - (float) Math.floor(clamped);
            float hopArc = (float) Math.sin(frac * Math.PI);

            int currentWhole = Math.round(clamped);
            if (currentWhole != lastStepTick[0]) {
                lastStepTick[0] = currentWhole;
                HapticHelper.tick(this);
            }

            if (isPlayerA) {
                animIndexA = clamped;
                hopLiftA = hopArc;
            } else {
                animIndexB = clamped;
                hopLiftB = hopArc;
            }
            invalidate();
        });
        activeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isPlayerA) {
                    animIndexA = toIdx;
                    hopLiftA = 0f;
                } else {
                    animIndexB = toIdx;
                    hopLiftB = 0f;
                }
                invalidate();
                if (onEnd != null) onEnd.run();
            }
        });
        activeAnimator.start();
    }

    public void cancelAnimation() {
        if (activeAnimator != null) {
            activeAnimator.cancel();
            activeAnimator = null;
        }
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
        if (tiles.isEmpty()) return;

        float density = getResources().getDisplayMetrics().density;
        float pad = 10f * density;
        float w = getWidth();
        float h = getHeight();

        // 1. Tabletop board frame
        boardRect.set(pad / 2f, pad / 2f, w - pad / 2f, h - pad / 2f);
        framePaint.setColor(blendColors(colorBg, colorSurface, 0.55f));
        canvas.drawRoundRect(boardRect, 18f * density, 18f * density, framePaint);

        tileStrokePaint.setStrokeWidth(1.4f * density);
        tileStrokePaint.setColor(withAlpha(colorAccent, 95));
        canvas.drawRoundRect(boardRect, 18f * density, 18f * density, tileStrokePaint);

        float innerLeft = pad * 1.3f;
        float innerTop = pad * 1.3f;
        float innerSize = Math.min(w, h) - pad * 2.6f;
        float cellSize = innerSize / CoupleBoardEngine.GRID_SIZE;
        float gap = 4.5f * density;

        // 2. Serpentine connecting guide path
        serpentinePath.reset();
        for (int i = 0; i < tiles.size(); i++) {
            BoardTile t = tiles.get(i);
            float cx = innerLeft + (t.col + 0.5f) * cellSize;
            float cy = innerTop + (t.row + 0.5f) * cellSize;
            if (i == 0) serpentinePath.moveTo(cx, cy);
            else serpentinePath.lineTo(cx, cy);
        }
        pathPaint.setStrokeWidth(3f * density);
        pathPaint.setColor(withAlpha(colorAccent, 55));
        canvas.drawPath(serpentinePath, pathPaint);

        // 3. Draw 36 tiles with escalating theme tints
        textPaint.setTextSize(10f * density);
        badgePaint.setTextSize(13f * density);

        for (BoardTile t : tiles) {
            float left = innerLeft + t.col * cellSize + gap;
            float top = innerTop + t.row * cellSize + gap;
            float right = innerLeft + (t.col + 1) * cellSize - gap;
            float bottom = innerTop + (t.row + 1) * cellSize - gap;
            tileRect.set(left, top, right, bottom);

            int fillColor = resolveTileFillColor(t);
            tileFillPaint.setColor(fillColor);
            canvas.drawRoundRect(tileRect, 9f * density, 9f * density, tileFillPaint);

            boolean isHighlighted = (t.index == highlightedTile);
            tileStrokePaint.setStrokeWidth((isHighlighted ? 2.2f : 1f) * density);
            tileStrokePaint.setColor(isHighlighted ? colorAccent : withAlpha(colorAccent, 70));
            canvas.drawRoundRect(tileRect, 9f * density, 9f * density, tileStrokePaint);

            float cx = tileRect.centerX();
            float cy = tileRect.centerY();

            // Tile number in top-left/top-center
            textPaint.setColor(withAlpha(colorTextMuted, 210));
            canvas.drawText(String.valueOf(t.index + 1), cx, top + 12f * density, textPaint);

            // Tile event icon/symbol
            String symbol = resolveTileSymbol(t.type);
            if (!symbol.isEmpty()) {
                badgePaint.setColor(t.type == BoardTileType.FINISH || t.type == BoardTileType.DESIRE_WILDCARD
                        ? colorAccent : colorTextPrimary);
                canvas.drawText(symbol, cx, cy + 9f * density, badgePaint);
            }
        }

        // 4. Draw Player A & Player B tokens with parabolic jump offset
        float[] posA = interpolateTileCenter(animIndexA, innerLeft, innerTop, cellSize);
        float[] posB = interpolateTileCenter(animIndexB, innerLeft, innerTop, cellSize);

        boolean overlapping = Math.abs(animIndexA - animIndexB) < 0.35f;
        float sep = overlapping ? cellSize * 0.19f : 0f;
        float hopPx = cellSize * 0.32f;

        drawToken(canvas, posA[0] - sep, posA[1] - hopLiftA * hopPx, cellSize * 0.23f,
                colorPrimary, colorTextPrimary, tokenLabelA, density);
        drawToken(canvas, posB[0] + sep, posB[1] - hopLiftB * hopPx, cellSize * 0.23f,
                colorAccent, colorBg, tokenLabelB, density);
    }

    private void drawToken(Canvas canvas, float cx, float cy, float radius,
                           int fill, int labelColor, String label, float density) {
        // Soft drop shadow
        tokenPaint.setColor(0x55000000);
        canvas.drawCircle(cx, cy + 2.5f * density, radius, tokenPaint);

        tokenPaint.setColor(fill);
        canvas.drawCircle(cx, cy, radius, tokenPaint);

        tokenRingPaint.setStrokeWidth(1.8f * density);
        tokenRingPaint.setColor(colorTextPrimary);
        canvas.drawCircle(cx, cy, radius, tokenRingPaint);

        tokenLabelPaint.setColor(labelColor);
        tokenLabelPaint.setTextSize(radius * 1.15f);
        Paint.FontMetrics fm = tokenLabelPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, cx, textY, tokenLabelPaint);
    }

    private float[] interpolateTileCenter(float fractionalIndex, float innerLeft, float innerTop, float cellSize) {
        int low = Math.max(0, Math.min(tiles.size() - 1, (int) Math.floor(fractionalIndex)));
        int high = Math.max(0, Math.min(tiles.size() - 1, (int) Math.ceil(fractionalIndex)));
        float t = fractionalIndex - low;

        BoardTile tileLow = tiles.get(low);
        BoardTile tileHigh = tiles.get(high);

        float x1 = innerLeft + (tileLow.col + 0.5f) * cellSize;
        float y1 = innerTop + (tileLow.row + 0.5f) * cellSize;
        float x2 = innerLeft + (tileHigh.col + 0.5f) * cellSize;
        float y2 = innerTop + (tileHigh.row + 0.5f) * cellSize;

        return new float[]{x1 + (x2 - x1) * t, y1 + (y2 - y1) * t};
    }

    private int resolveTileFillColor(BoardTile tile) {
        if (tile.type == BoardTileType.START) {
            return blendColors(colorSurface, colorAccent, 0.25f);
        }
        if (tile.type == BoardTileType.FINISH) {
            return blendColors(colorPrimary, colorAccent, 0.45f);
        }
        if (tile.type == BoardTileType.DESIRE_WILDCARD) {
            return blendColors(colorPrimary, colorAccent, 0.22f);
        }
        // Subtle heat escalation from Tier 1 (cool surface) to Tier 4 (warm primary glow)
        float heatRatio = (tile.tier - 1) * 0.18f;
        return blendColors(colorSurface, colorPrimary, heatRatio);
    }

    private String resolveTileSymbol(BoardTileType type) {
        switch (type) {
            case START: return "★";
            case FINISH: return "♛";
            case BOOST_FORWARD: return "+2";
            case STEP_BACK: return "-2";
            case SWAP_POSITIONS: return "⇄";
            case EXTRA_ROLL: return "🎲";
            case DESIRE_WILDCARD: return "♥";
            case TASK:
            default:
                return "";
        }
    }

    private int blendColors(int from, int to, float ratio) {
        float inverse = 1f - ratio;
        int r = Math.round(Color.red(from) * inverse + Color.red(to) * ratio);
        int g = Math.round(Color.green(from) * inverse + Color.green(to) * ratio);
        int b = Math.round(Color.blue(from) * inverse + Color.blue(to) * ratio);
        return Color.rgb(r, g, b);
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAnimation();
    }
}
