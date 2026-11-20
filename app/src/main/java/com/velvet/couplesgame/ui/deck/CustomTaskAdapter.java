package com.velvet.couplesgame.ui.deck;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.data.local.entity.CustomTaskEntity;
import com.velvet.couplesgame.databinding.ItemCustomTaskBinding;
import java.util.ArrayList;
import java.util.List;

public class CustomTaskAdapter extends RecyclerView.Adapter<CustomTaskAdapter.Holder> {
    public interface OnDeleteListener {
        void onDelete(String taskId);
    }

    private final List<CustomTaskEntity> items = new ArrayList<>();
    private final OnDeleteListener deleteListener;

    public CustomTaskAdapter(OnDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void submitList(List<CustomTaskEntity> newItems) {
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
        CustomTaskEntity item = items.get(position);
        holder.binding.tvCustomMeta.setText(TagDisplayMapper.formatCardMeta(
                holder.itemView.getContext(), item.tier, item.type, false, true
        ));
        holder.binding.tvCustomText.setText(item.text);
        holder.binding.btnDeleteCustom.setOnClickListener(v -> deleteListener.onDelete(item.id));
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
