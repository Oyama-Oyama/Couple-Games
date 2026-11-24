package com.velvet.couplesgame.ui.blindmatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.databinding.ItemBlindMatchResultBinding;
import java.util.ArrayList;
import java.util.List;

public class BlindMatchResultAdapter extends RecyclerView.Adapter<BlindMatchResultAdapter.Holder> {
    private final List<TaskCard> items = new ArrayList<>();

    public void submitList(List<TaskCard> matches) {
        items.clear();
        if (matches != null) items.addAll(matches);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBlindMatchResultBinding binding = ItemBlindMatchResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TaskCard card = items.get(position);
        Context ctx = holder.itemView.getContext();
        String tierLabel = ctx.getString(TagDisplayMapper.resolveTierNameRes(card.tier));
        holder.binding.tvMatchTier.setText("♥ " + tierLabel + " · " + ctx.getString(R.string.badge_mutual_desire));
        holder.binding.tvMatchText.setText(card.text);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemBlindMatchResultBinding binding;

        Holder(ItemBlindMatchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
