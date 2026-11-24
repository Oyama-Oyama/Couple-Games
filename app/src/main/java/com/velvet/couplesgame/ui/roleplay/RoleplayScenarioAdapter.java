package com.velvet.couplesgame.ui.roleplay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayScenarioDto;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.databinding.ItemRoleplayScenarioBinding;
import java.util.ArrayList;
import java.util.List;

public class RoleplayScenarioAdapter extends RecyclerView.Adapter<RoleplayScenarioAdapter.Holder> {
    public interface OnPickListener {
        void onPick(RoleplayScenarioDto scenario);
    }

    private final List<RoleplayScenarioDto> items = new ArrayList<>();
    private final OnPickListener listener;

    public RoleplayScenarioAdapter(OnPickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RoleplayScenarioDto> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoleplayScenarioBinding binding = ItemRoleplayScenarioBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RoleplayScenarioDto s = items.get(position);
        Context ctx = holder.itemView.getContext();
        String tierLabel = ctx.getString(TagDisplayMapper.resolveTierNameRes(s.tier));
        holder.binding.tvRpMeta.setText(tierLabel + " · " + ctx.getString(R.string.roleplay_acts_count, s.acts.size()));
        holder.binding.tvRpTitle.setText(s.title);
        holder.binding.tvRpSubtitle.setText(s.subtitle);
        holder.binding.tvRpRoles.setText(s.leadRole + "  ×  " + s.partnerRole);
        holder.itemView.setOnClickListener(v -> listener.onPick(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemRoleplayScenarioBinding binding;

        Holder(ItemRoleplayScenarioBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
