package com.velvet.couplesgame.ui.deck;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.data.local.entity.FavoriteTaskEntity;
import com.velvet.couplesgame.databinding.ItemCustomTaskBinding;
import java.util.ArrayList;
import java.util.List;

public class FavoriteTaskAdapter extends RecyclerView.Adapter<FavoriteTaskAdapter.Holder> {
    public interface OnRemoveListener {
        void onRemove(String cardId);
    }

    private final List<FavoriteTaskEntity> items = new ArrayList<>();
    private final OnRemoveListener removeListener;

    public FavoriteTaskAdapter(OnRemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    public void submitList(List<FavoriteTaskEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCustomTaskBinding binding = ItemCustomTaskBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        FavoriteTaskEntity item = items.get(position);
        Context ctx = holder.itemView.getContext();
        String tierLabel = ctx.getString(TagDisplayMapper.resolveTierNameRes(item.tier));
        holder.binding.tvCustomMeta.setText("♥ " + tierLabel + " · " + ctx.getString(R.string.badge_favorite_card));
        holder.binding.tvCustomText.setText(item.snapshotText);
        holder.binding.btnDeleteCustom.setOnClickListener(v -> removeListener.onRemove(item.cardId));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemCustomTaskBinding binding;

        Holder(ItemCustomTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
