package com.velvet.couplesgame.ui.connect4;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.databinding.ItemConnect4CellBinding;
import java.util.ArrayList;
import java.util.List;

public class Connect4GridAdapter extends RecyclerView.Adapter<Connect4GridAdapter.Holder> {
    public interface OnColumnTapListener {
        void onColumnTap(int col);
    }

    private final List<Connect4Engine.Cell> cells = new ArrayList<>();
    private final AppTheme theme;
    private final OnColumnTapListener listener;

    public Connect4GridAdapter(AppTheme theme, OnColumnTapListener listener) {
        this.theme = theme != null ? theme : AppTheme.VELVET_NOIR;
        this.listener = listener;
    }

    public void submitCells(List<Connect4Engine.Cell> next) {
        cells.clear();
        if (next != null) cells.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConnect4CellBinding binding = ItemConnect4CellBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Connect4Engine.Cell cell = cells.get(position);
        Context ctx = holder.itemView.getContext();
        float d = ctx.getResources().getDisplayMetrics().density;

        int surface = ContextCompat.getColor(ctx, theme.surfaceColorRes);
        int primary = ContextCompat.getColor(ctx, theme.primaryColorRes);
        int accent = ContextCompat.getColor(ctx, theme.accentColorRes);
        int bg = ContextCompat.getColor(ctx, theme.bgColorRes);
        int textPrimary = ContextCompat.getColor(ctx, theme.textPrimaryColorRes);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(10f * d);

        if (cell.owner == Connect4Engine.EMPTY) {
            shape.setColor(surface);
            shape.setStroke(Math.round(1f * d), (accent & 0x00FFFFFF) | 0x44000000);
            holder.binding.tvC4Cell.setText(cell.action != null ? cell.action.shortLabel : "♥");
            holder.binding.tvC4Cell.setTextColor((textPrimary & 0x00FFFFFF) | 0xBB000000);
        } else if (cell.owner == Connect4Engine.PLAYER_A) {
            shape.setColor(primary);
            shape.setStroke(Math.round((cell.winningCell ? 2.5f : 1.2f) * d), accent);
            holder.binding.tvC4Cell.setText(cell.winningCell ? "♛" : "♥");
            holder.binding.tvC4Cell.setTextColor(textPrimary);
        } else {
            shape.setColor(accent);
            shape.setStroke(Math.round((cell.winningCell ? 2.5f : 1.2f) * d), textPrimary);
            holder.binding.tvC4Cell.setText(cell.winningCell ? "♛" : "✦");
            holder.binding.tvC4Cell.setTextColor(bg);
        }

        holder.binding.tvC4Cell.setBackground(shape);
        holder.itemView.setOnClickListener(v -> listener.onColumnTap(cell.col));
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemConnect4CellBinding binding;

        Holder(ItemConnect4CellBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
