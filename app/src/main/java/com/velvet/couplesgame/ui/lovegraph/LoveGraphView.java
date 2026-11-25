package com.velvet.couplesgame.ui.lovegraph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.velvet.couplesgame.core.lovegraph.LoveGraphEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the abstract Love Map: an abstract constellation of the couple's
 * shared rituals and milestones.
 *
 * <p>Deliberately not a geographic map: the whole view is drawn on a Canvas
 * from local data, so the app needs no map SDK, no network and no location
 * permission.
 */
public class LoveGraphView extends View {

    /** Callback fired when a node is tapped. */
    public interface OnNodeTapListener {
        void onNodeTapped(LoveGraphEngine.Node node);
    }

    private final Paint linkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<LoveGraphEngine.Node> nodes = new ArrayList<>();
    private final List<LoveGraphEngine.Link> links = new ArrayList<>();
    private final List<float[]> hitPoints = new ArrayList<>();

    /** Label baselines and value flags, recomputed on every draw. */
    private float[] labelBaselines = new float[0];
    private boolean[] labelValues = new boolean[0];

    private float centerX;
    private float centerY;
    private float innerRadius;
    private float outerRadius;

    private int accentColor = 0xFFD9A441;
    private int mutedColor = 0xFF9A8F86;
    private int primaryColor = 0xFFF5EFE6;
    private String selectedId = "";
    private OnNodeTapListener tapListener;
    private ExploreByTouchHelper accessibilityHelper;

    public LoveGraphView(Context context) {
        super(context);
        init();
    }

    public LoveGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoveGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        accessibilityHelper = new ExploreByTouchHelper(this) {
            @Override
            protected int getVirtualViewAt(float x, float y) {
                float hitRadius = 48f * getResources().getDisplayMetrics().density;
                float bestDistanceSquared = hitRadius * hitRadius;
                int nearestIndex = INVALID_ID;
                for (int i = 0; i < hitPoints.size(); i++) {
                    float[] point = hitPoints.get(i);
                    float dx = x - point[0];
                    float dy = y - point[1];
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
                for (int i = 0; i < nodes.size(); i++) virtualViewIds.add(i);
            }

            @Override
            protected void onPopulateEventForVirtualView(
                    int virtualViewId, AccessibilityEvent event) {
                LoveGraphEngine.Node node = getNode(virtualViewId);
                if (node != null) event.setContentDescription(accessibilityLabel(node));
            }

            @Override
            protected void onPopulateNodeForVirtualView(
                    int virtualViewId, AccessibilityNodeInfoCompat nodeInfo) {
                LoveGraphEngine.Node node = getNode(virtualViewId);
                if (node == null) {
                    nodeInfo.setContentDescription("");
                    return;
                }
                nodeInfo.setClassName("android.widget.Button");
                nodeInfo.setContentDescription(accessibilityLabel(node));
                nodeInfo.setClickable(true);
                nodeInfo.setFocusable(true);
                nodeInfo.setSelected(node.id.equals(selectedId));
                nodeInfo.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

                float[] point = virtualViewId < hitPoints.size()
                        ? hitPoints.get(virtualViewId)
                        : new float[]{getWidth() / 2f, getHeight() / 2f};
                int halfTarget = Math.round(24f * getResources().getDisplayMetrics().density);
                nodeInfo.setBoundsInParent(new Rect(
                        Math.round(point[0]) - halfTarget,
                        Math.round(point[1]) - halfTarget,
                        Math.round(point[0]) + halfTarget,
                        Math.round(point[1]) + halfTarget
                ));
            }

            @Override
            protected boolean onPerformActionForVirtualView(
                    int virtualViewId, int action, Bundle arguments) {
                LoveGraphEngine.Node node = getNode(virtualViewId);
                if (action != AccessibilityNodeInfoCompat.ACTION_CLICK || node == null) {
                    return false;
                }
                selectNode(node);
                return true;
            }
        };
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper);
        linkPaint.setStyle(Paint.Style.STROKE);
        linkPaint.setStrokeCap(Paint.Cap.ROUND);

        nodePaint.setStyle(Paint.Style.FILL);

        glowPaint.setStyle(Paint.Style.FILL);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1f);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(spToPx(11f));

        resolveThemeColors();
    }

    /** Resolve the velvet theme colours from the activity theme. */
    public void resolveThemeColors() {
        accentColor = resolveAttr(com.velvet.couplesgame.R.attr.velvetAccent, accentColor);
        mutedColor = resolveAttr(com.velvet.couplesgame.R.attr.velvetTextMuted, mutedColor);
        primaryColor = resolveAttr(com.velvet.couplesgame.R.attr.velvetTextPrimary, primaryColor);
        invalidate();
    }

    private int resolveAttr(int attr, int fallback) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(attr, tv, true) && tv.resourceId != 0) {
            try {
                return getContext().getResources().getColor(tv.resourceId, getContext().getTheme());
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    /** Replace the rendered graph. */
    public void setGraph(@Nullable LoveGraphEngine.Graph graph) {
        nodes.clear();
        links.clear();
        hitPoints.clear();
        if (graph != null) {
            nodes.addAll(graph.nodes);
            links.addAll(graph.links);
        }
        invalidate();
        accessibilityHelper.invalidateRoot();
    }

    /** Highlight a node by id; an empty id clears the selection. */
    public void setSelectedNode(String id) {
        selectedId = id != null ? id : "";
        invalidate();
        accessibilityHelper.invalidateRoot();
    }

    public void setOnNodeTapListener(OnNodeTapListener listener) {
        this.tapListener = listener;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        centerX = w / 2f;
        centerY = h / 2f;
        float base = Math.min(w, h);
        innerRadius = base * 0.22f;
        outerRadius = base * 0.38f;

        // Faint guide rings.
        ringPaint.setColor(withAlpha(mutedColor, 46));
        canvas.drawCircle(centerX, centerY, innerRadius, ringPaint);
        canvas.drawCircle(centerX, centerY, outerRadius, ringPaint);

        // Position the nodes first so the links can reference them.
        hitPoints.clear();
        for (LoveGraphEngine.Node node : nodes) {
            float radius = node.ring == 0 ? innerRadius : outerRadius;
            float px = centerX + (float) Math.cos(node.angle) * radius;
            float py = centerY + (float) Math.sin(node.angle) * radius;
            hitPoints.add(new float[]{px, py});
        }

        drawLinks(canvas);
        drawNodes(canvas);
    }

    private void drawLinks(Canvas canvas) {
        for (LoveGraphEngine.Link link : links) {
            int from = indexOf(link.fromId);
            int to = indexOf(link.toId);
            if (from < 0 || to < 0) continue;
            float[] a = hitPoints.get(from);
            float[] b = hitPoints.get(to);
            linkPaint.setStrokeWidth(1f + 3.2f * link.strength);
            linkPaint.setColor(withAlpha(accentColor, (int) (40 + 110 * link.strength)));
            canvas.drawLine(a[0], a[1], b[0], b[1], linkPaint);
        }
    }

    private void drawNodes(Canvas canvas) {
        // Pass 1: draw the nodes themselves.
        for (int i = 0; i < nodes.size(); i++) {
            LoveGraphEngine.Node node = nodes.get(i);
            float[] point = hitPoints.get(i);
            float px = point[0];
            float py = point[1];
            float radius = radiusOf(node);
            boolean selected = node.id.equals(selectedId);

            // Soft halo behind the node.
            glowPaint.setShader(new RadialGradient(px, py, radius * 2.6f,
                    new int[]{withAlpha(accentColor, 90), withAlpha(accentColor, 0)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(px, py, radius * 2.6f, glowPaint);
            glowPaint.setShader(null);

            nodePaint.setColor(selected ? primaryColor : withAlpha(accentColor, 220));
            canvas.drawCircle(px, py, radius, nodePaint);

            if (selected) {
                linkPaint.setStyle(Paint.Style.STROKE);
                linkPaint.setStrokeWidth(2f);
                linkPaint.setColor(accentColor);
                canvas.drawCircle(px, py, radius + 5f, linkPaint);
                linkPaint.setStyle(Paint.Style.STROKE);
            }
        }

        // Pass 2: lay the labels out, then draw them.
        layoutLabels();
        for (int i = 0; i < nodes.size(); i++) {
            drawLabel(canvas, nodes.get(i), labelBaselines[i], labelValues[i]);
        }
    }

    private float radiusOf(LoveGraphEngine.Node node) {
        float base = node.ring == 0 ? 16f : 12f;
        return base * (0.55f + 0.65f * node.weight);
    }

    /**
     * Computes the baseline of every label, pushing labels apart when two of
     * them would overlap. Each label is placed on the outward side of its
     * node, and a second pass moves any colliding label further out.
     */
    private void layoutLabels() {
        int count = nodes.size();
        labelBaselines = new float[count];
        labelValues = new boolean[count];

        for (int i = 0; i < count; i++) {
            LoveGraphEngine.Node node = nodes.get(i);
            float py = hitPoints.get(i)[1];
            float radius = radiusOf(node);
            float outward = py < centerY ? -1f : 1f;
            labelBaselines[i] = py + outward * labelClearance(node);
            labelValues[i] = node.value > 0;
        }

        // Two relaxation rounds are enough for the small fixed node count.
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < count; i++) {
                for (int j = i + 1; j < count; j++) {
                    float minDistance = spToPx(26f);
                    float gap = labelBaselines[i] - labelBaselines[j];
                    if (Math.abs(gap) >= minDistance) continue;
                    // Push the upper label up and the lower label down.
                    float push = (minDistance - Math.abs(gap)) / 2f + 1f;
                    if (gap >= 0) {
                        labelBaselines[i] += push;
                        labelBaselines[j] -= push;
                    } else {
                        labelBaselines[i] -= push;
                        labelBaselines[j] += push;
                    }
                }
            }
        }

        // The relaxation must never push a label back onto its own node.
        for (int i = 0; i < count; i++) {
            LoveGraphEngine.Node node = nodes.get(i);
            float py = hitPoints.get(i)[1];
            float clearance = labelClearance(node);
            float outward = py < centerY ? -1f : 1f;
            float limit = py + outward * clearance;
            if (outward > 0) {
                labelBaselines[i] = Math.max(labelBaselines[i], limit);
            } else {
                labelBaselines[i] = Math.min(labelBaselines[i], limit);
            }
        }
    }

    /**
     * Distance from the node centre to the label baseline: the node radius plus
     * the gap plus one line of text, so the text always clears the circle.
     */
    private float labelClearance(LoveGraphEngine.Node node) {
        return radiusOf(node) + spToPx(12f) + spToPx(11f);
    }

    /**
     * Draws the node label as two stacked lines (name, then value) placed as a
     * block on the outward side of the node, so the circle never covers the
     * text. Labels on the right half of the map are right-aligned so long
     * localised strings stay inside the canvas.
     */
    private void drawLabel(Canvas canvas, LoveGraphEngine.Node node,
                           float nameBaseline, boolean hasValue) {
        int index = indexOf(node.id);
        float px = hitPoints.get(index)[0];
        boolean selected = node.id.equals(selectedId);

        // Shift the text away from the node circle so it never sits on top of it.
        float anchorX = px + anchorOffsetFor(node, px);

        labelPaint.setColor(selected ? primaryColor : mutedColor);
        labelPaint.setTextSize(spToPx(selected ? 12f : 10.5f));
        labelPaint.setTextAlign(alignFor(px));
        canvas.drawText(getContext().getString(labelResFor(node)), anchorX, nameBaseline, labelPaint);

        if (hasValue) {
            labelPaint.setColor(selected ? primaryColor : withAlpha(mutedColor, 190));
            labelPaint.setTextSize(spToPx(selected ? 11f : 9.5f));
            canvas.drawText(String.valueOf(node.value), anchorX,
                    nameBaseline + spToPx(12f), labelPaint);
        }
    }

    /**
     * Pushes the text anchor outwards by the node radius plus a small gap, so
     * right- or left-aligned labels clear the node circle.
     */
    private float anchorOffsetFor(LoveGraphEngine.Node node, float px) {
        float radius = radiusOf(node);
        float gap = spToPx(4f);
        if (px > centerX + outerRadius * 0.35f) {
            return -(radius + gap);
        }
        if (px < centerX - outerRadius * 0.35f) {
            return radius + gap;
        }
        return 0f;
    }

    private int labelResFor(LoveGraphEngine.Node node) {
        switch (node.id) {
            case "together": return com.velvet.couplesgame.R.string.love_graph_together;
            case "anniversary": return com.velvet.couplesgame.R.string.love_graph_anniversary;
            case "badges": return com.velvet.couplesgame.R.string.love_graph_badges;
            case "play": return com.velvet.couplesgame.R.string.love_graph_play;
            case "mood": return com.velvet.couplesgame.R.string.love_graph_mood;
            case "date": return com.velvet.couplesgame.R.string.love_graph_date;
            case "letter": return com.velvet.couplesgame.R.string.love_graph_letter;
            case "favorite": return com.velvet.couplesgame.R.string.love_graph_favorite;
            default: return com.velvet.couplesgame.R.string.love_graph_together;
        }
    }

    private int indexOf(String id) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX();
        float y = event.getY();
        for (int i = 0; i < hitPoints.size(); i++) {
            float[] p = hitPoints.get(i);
            float dx = x - p[0];
            float dy = y - p[1];
            if (dx * dx + dy * dy <= 36f * 36f) {
                LoveGraphEngine.Node node = nodes.get(i);
                selectNode(node);
                performClick();
                return true;
            }
        }
        return true;
    }

    private void selectNode(LoveGraphEngine.Node node) {
        HapticTap();
        setSelectedNode(node.id);
        if (tapListener != null) tapListener.onNodeTapped(node);
    }

    private LoveGraphEngine.Node getNode(int index) {
        return index >= 0 && index < nodes.size() ? nodes.get(index) : null;
    }

    private String accessibilityLabel(LoveGraphEngine.Node node) {
        return getContext().getString(labelResFor(node)) + ", " + node.value;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Small haptic tick without pulling the UI helper into a custom View. */
    private void HapticTap() {
        try {
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        } catch (Exception ignored) {
            // Haptics are best-effort.
        }
    }

    private static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /** Right-align labels on the right side of the map so they stay in view. */
    private Paint.Align alignFor(float px) {
        if (px > centerX + outerRadius * 0.35f) return Paint.Align.RIGHT;
        if (px < centerX - outerRadius * 0.35f) return Paint.Align.LEFT;
        return Paint.Align.CENTER;
    }
}
