package com.velvet.couplesgame.ui.settings;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import com.velvet.couplesgame.databinding.ItemSessionStatBinding;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionStatAdapter extends RecyclerView.Adapter<SessionStatAdapter.Holder> {
    private final List<SessionStatEntity> items = new ArrayList<>();
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private String nameA = "A";
    private String nameB = "B";

    public void setPartnerNames(String a, String b) {
        this.nameA = a != null ? a : "A";
        this.nameB = b != null ? b : "B";
        notifyDataSetChanged();
    }

    public void submitList(List<SessionStatEntity> stats) {
        items.clear();
        if (stats != null) items.addAll(stats);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionStatBinding binding = ItemSessionStatBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SessionStatEntity stat = items.get(position);
        holder.binding.tvStatMode.setText(TagDisplayMapper.resolveGameModeRes(stat.gameMode));
        holder.binding.tvStatDate.setText(dateFormat.format(new Date(stat.playedAt)));
        holder.binding.tvStatDetail.setText(nameA + ": " + stat.playerACompleted + "  ·  " + nameB + ": " + stat.playerBCompleted);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemSessionStatBinding binding;

        Holder(ItemSessionStatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
