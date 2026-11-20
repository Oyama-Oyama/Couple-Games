package com.velvet.couplesgame.ui.minesweeper;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.databinding.ItemMineCellBinding;
import java.util.ArrayList;
import java.util.List;

public class MinesweeperGridAdapter extends RecyclerView.Adapter<MinesweeperGridAdapter.Holder> {
    public interface CellListener {
        void onReveal(MineCell cell);
        void onToggleFlag(MineCell cell);
    }

    private final List<MineCell> cells = new ArrayList<>();
    private final AppTheme theme;
    private final CellListener listener;

    public MinesweeperGridAdapter(AppTheme theme, CellListener listener) {
        this.theme = theme != null ? theme : AppTheme.VELVET_NOIR;
        this.listener = listener;
    }

    public void submitCells(List<MineCell> list) {
        cells.clear();
        if (list != null) cells.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMineCellBinding binding = ItemMineCellBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MineCell cell = cells.get(position);
        Context ctx = holder.itemView.getContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        int bgColor = ContextCompat.getColor(ctx, theme.bgColorRes);
        int surfaceColor = ContextCompat.getColor(ctx, theme.surfaceColorRes);
        int primaryColor = ContextCompat.getColor(ctx, theme.primaryColorRes);
        int accentColor = ContextCompat.getColor(ctx, theme.accentColorRes);
        int textPrimary = ContextCompat.getColor(ctx, theme.textPrimaryColorRes);

        GradientDrawable tileBg = new GradientDrawable();
        tileBg.setShape(GradientDrawable.RECTANGLE);
        tileBg.setCornerRadius(10f * density);

        if (!cell.revealed) {
            tileBg.setColor(surfaceColor);
            tileBg.setStroke(Math.round(1.2f * density), (accentColor & 0x00FFFFFF) | 0x55000000);
            holder.binding.tvMineCell.setText(cell.flagged ? "⚑" : "");
            holder.binding.tvMineCell.setTextColor(accentColor);
        } else if (cell.hasMine) {
            tileBg.setColor(primaryColor);
            tileBg.setStroke(Math.round(1.6f * density), accentColor);
            holder.binding.tvMineCell.setText("♥");
            holder.binding.tvMineCell.setTextColor(accentColor);
        } else {
            tileBg.setColor(bgColor);
            tileBg.setStroke(Math.round(1f * density), (accentColor & 0x00FFFFFF) | 0x26000000);
            if (cell.adjacentMines > 0) {
                holder.binding.tvMineCell.setText(String.valueOf(cell.adjacentMines));
                holder.binding.tvMineCell.setTextColor(cell.adjacentMines >= 2 ? accentColor : textPrimary);
            } else {
                holder.binding.tvMineCell.setText("");
            }
        }

        holder.binding.tvMineCell.setBackground(tileBg);
        holder.itemView.setOnClickListener(v -> listener.onReveal(cell));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onToggleFlag(cell);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMineCellBinding binding;

        Holder(ItemMineCellBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
