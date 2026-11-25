package com.velvet.couplesgame.ui.drawguess;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.ui.common.HapticHelper;

/**
 * Tactile freeform HSV Color Picker dialog with live swatch preview + 12 curated quick swatches.
 */
public final class ColorPickerDialogHelper {
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private static final int[] QUICK_ROW_1 = new int[]{
            0xFFFF3B6B, 0xFFFF6B8B, 0xFFFF9F68, 0xFFFFD166, 0xFF06D6A0, 0xFF4CC9F0
    };
    private static final int[] QUICK_ROW_2 = new int[]{
            0xFF9D4EDD, 0xFFC77DFF, 0xFFF72585, 0xFFF5F0EB, 0xFF8D99AE, 0xFF1A1A22
    };

    private ColorPickerDialogHelper() {}

    public static void show(Context context, int initialColor, OnColorSelectedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        View preview = view.findViewById(R.id.view_picker_preview);
        TextView tvHex = view.findViewById(R.id.tv_picker_hex);
        ColorPickerView pickerView = view.findViewById(R.id.color_picker_view);
        LinearLayout row1 = view.findViewById(R.id.layout_quick_swatches_1);
        LinearLayout row2 = view.findViewById(R.id.layout_quick_swatches_2);
        MaterialButton btnCancel = view.findViewById(R.id.btn_picker_cancel);
        MaterialButton btnApply = view.findViewById(R.id.btn_picker_apply);

        float density = context.getResources().getDisplayMetrics().density;
        final int[] chosenColor = {initialColor};

        Runnable updatePreview = () -> {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(chosenColor[0]);
            circle.setStroke(Math.round(1.6f * density), 0xDDFFFFFF);
            preview.setBackground(circle);
            tvHex.setText(ColorPickerView.formatHex(chosenColor[0]));
        };

        pickerView.setColor(initialColor);
        pickerView.setContentDescription(context.getString(R.string.color_picker_accessibility));
        updatePreview.run();

        pickerView.setOnColorChangedListener(color -> {
            chosenColor[0] = color;
            updatePreview.run();
        });

        populateSwatchRow(context, row1, QUICK_ROW_1, density, c -> {
            chosenColor[0] = c;
            pickerView.setColor(c);
            updatePreview.run();
        });
        populateSwatchRow(context, row2, QUICK_ROW_2, density, c -> {
            chosenColor[0] = c;
            pickerView.setColor(c);
            updatePreview.run();
        });

        Dialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnApply.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            dialog.dismiss();
            if (listener != null) listener.onColorSelected(chosenColor[0]);
        });

        dialog.show();
    }

    private static void populateSwatchRow(Context ctx, LinearLayout row, int[] colors, float density,
                                          OnColorSelectedListener onTap) {
        int size = Math.round(32f * density);
        int margin = Math.round(6f * density);
        for (int color : colors) {
            View dot = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            bg.setStroke(Math.round(1.4f * density), 0x88FFFFFF);
            dot.setBackground(bg);
            dot.setContentDescription(ColorPickerView.formatHex(color));
            dot.setClickable(true);
            dot.setFocusable(true);
            dot.setOnClickListener(v -> {
                HapticHelper.tick(v);
                onTap.onColorSelected(color);
            });
            row.addView(dot);
        }
    }
}
