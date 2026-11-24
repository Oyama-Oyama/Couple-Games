package com.velvet.couplesgame.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.databinding.ItemGameModeCardBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders Velvet game modes ordered by Stimulation Level (Heat Rating 5 -> 1)
 * with flame indicators and VIP subscription lock badges.
 */
public class GameModeAdapter extends RecyclerView.Adapter<GameModeAdapter.Holder> {
    public interface OnModeSelectListener {
        void onModeSelected(GameModeCatalog mode);
    }

    private final List<GameModeCatalog> items = new ArrayList<>();
    private final OnModeSelectListener listener;
    private boolean premiumUnlocked = false;

    public GameModeAdapter(OnModeSelectListener listener) {
        this.listener = listener;
    }

    public void submitModes(List<GameModeCatalog> modes) {
        items.clear();
        if (modes != null) items.addAll(modes);
        notifyDataSetChanged();
    }

    public void setPremiumUnlocked(boolean unlocked) {
        this.premiumUnlocked = unlocked;
        notifyDataSetChanged();
    }

    public static void bindCard(ItemGameModeCardBinding binding,
                                GameModeCatalog mode,
                                boolean premiumUnlocked,
                                OnModeSelectListener listener) {
        Context ctx = binding.getRoot().getContext();

        binding.tvModeFlames.setText(mode.flameIndicator());
        binding.tvModeHeatTag.setText(mode.heatTagRes);
        binding.tvModeTitle.setText(mode.titleRes);
        binding.tvModeDesc.setText(mode.descRes);

        boolean locked = mode.vipRequired && !premiumUnlocked;
        if (mode.vipRequired) {
            binding.tvModeVipBadge.setVisibility(View.VISIBLE);
            binding.tvModeVipBadge.setText(locked
                    ? ctx.getString(R.string.mode_badge_vip_locked)
                    : ctx.getString(R.string.mode_badge_vip_unlocked));
            binding.cardGameModeRoot.setBackgroundResource(
                    mode.heatRating >= 5 ? R.drawable.bg_card_velvet_selected : R.drawable.bg_card_velvet_glow
            );
        } else {
            binding.tvModeVipBadge.setVisibility(View.VISIBLE);
            binding.tvModeVipBadge.setText(R.string.mode_badge_free);
            binding.cardGameModeRoot.setBackgroundResource(R.drawable.bg_card_velvet);
        }

        binding.cardGameModeRoot.setAlpha(locked ? 0.90f : 1.0f);
        binding.cardGameModeRoot.setOnClickListener(v -> {
            if (listener != null) listener.onModeSelected(mode);
        });
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGameModeCardBinding binding = ItemGameModeCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        bindCard(holder.binding, items.get(position), premiumUnlocked, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemGameModeCardBinding binding;

        Holder(ItemGameModeCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
