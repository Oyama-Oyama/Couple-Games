package com.velvet.couplesgame.ui.consent;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.databinding.ItemBoundaryRuleBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports both Together Mode (both partners visible) and Private Pass Mode
 * (only the active partner's column is shown so neither sees the other's vetoes).
 */
public class BoundaryAdapter extends RecyclerView.Adapter<BoundaryAdapter.Holder> {
    public enum DisplayMode {
        TOGETHER,
        PRIVATE_A_ONLY,
        PRIVATE_B_ONLY
    }

    public interface Listener {
        void onToggle(BoundaryRuleEntity rule, boolean isPlayerA);
    }

    private final List<BoundaryRuleEntity> items = new ArrayList<>();
    private final Listener listener;
    private String nameA = "Alex";
    private String nameB = "Maya";
    private DisplayMode displayMode = DisplayMode.TOGETHER;

    public BoundaryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPartnerNames(String nameA, String nameB) {
        this.nameA = nameA;
        this.nameB = nameB;
        notifyDataSetChanged();
    }

    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode != null ? mode : DisplayMode.TOGETHER;
        notifyDataSetChanged();
    }

    public void submitList(List<BoundaryRuleEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBoundaryRuleBinding binding = ItemBoundaryRuleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BoundaryRuleEntity rule = items.get(position);
        Context ctx = holder.itemView.getContext();
        holder.binding.tvTagName.setText(TagDisplayMapper.resolveTagTitleRes(rule.tagKey));

        String shortA = nameA.length() > 6 ? nameA.substring(0, 6) : nameA;
        String shortB = nameB.length() > 6 ? nameB.substring(0, 6) : nameB;

        bindStatusButton(ctx, holder.binding.btnPlayerA, shortA + ": ", rule.playerAStatus);
        bindStatusButton(ctx, holder.binding.btnPlayerB, shortB + ": ", rule.playerBStatus);

        holder.binding.btnPlayerA.setVisibility(
                displayMode == DisplayMode.PRIVATE_B_ONLY ? View.GONE : View.VISIBLE
        );
        holder.binding.btnPlayerB.setVisibility(
                displayMode == DisplayMode.PRIVATE_A_ONLY ? View.GONE : View.VISIBLE
        );

        holder.binding.btnPlayerA.setOnClickListener(v -> listener.onToggle(rule, true));
        holder.binding.btnPlayerB.setOnClickListener(v -> listener.onToggle(rule, false));
    }

    private void bindStatusButton(Context ctx, MaterialButton button, String prefix, String status) {
        int colorRes;
        int labelRes;
        if ("HARD_NO".equals(status)) {
            colorRes = R.color.status_hard_no;
            labelRes = R.string.status_hard_no;
        } else if ("SOFT_LIMIT".equals(status)) {
            colorRes = R.color.status_soft;
            labelRes = R.string.status_soft;
        } else {
            colorRes = R.color.status_yes;
            labelRes = R.string.status_yes;
        }
        button.setText(prefix + ctx.getString(labelRes));
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, colorRes)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemBoundaryRuleBinding binding;

        Holder(ItemBoundaryRuleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
