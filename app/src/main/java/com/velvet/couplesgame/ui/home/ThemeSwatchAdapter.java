package com.velvet.couplesgame.ui.home;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.databinding.ItemThemeSwatchBinding;

public class ThemeSwatchAdapter extends RecyclerView.Adapter<ThemeSwatchAdapter.Holder> {
    public interface OnThemeSelectedListener {
        void onSelect(AppTheme theme);
    }

    private final AppTheme[] themes = AppTheme.values();
    private final AppTheme activeTheme;
    private boolean premiumUnlocked;
    private final OnThemeSelectedListener listener;

    public ThemeSwatchAdapter(AppTheme activeTheme, boolean premiumUnlocked, OnThemeSelectedListener listener) {
        this.activeTheme = activeTheme;
        this.premiumUnlocked = premiumUnlocked;
        this.listener = listener;
    }

    public void setPremiumUnlocked(boolean unlocked) {
        this.premiumUnlocked = unlocked;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemThemeSwatchBinding binding = ItemThemeSwatchBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppTheme theme = themes[position];
        Context ctx = holder.itemView.getContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        int bgColor = ContextCompat.getColor(ctx, theme.bgColorRes);
        int surfaceColor = ContextCompat.getColor(ctx, theme.surfaceColorRes);
        int primaryColor = ContextCompat.getColor(ctx, theme.primaryColorRes);
        int accentColor = ContextCompat.getColor(ctx, theme.accentColorRes);
        int textPrimary = ContextCompat.getColor(ctx, theme.textPrimaryColorRes);
        int textMuted = ContextCompat.getColor(ctx, theme.textMutedColorRes);

        boolean isActive = (theme == activeTheme);
        boolean isLocked = theme.premium && !premiumUnlocked;

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(14f * density);
        cardBg.setColor(surfaceColor);
        cardBg.setStroke(
                Math.round((isActive ? 2f : 1f) * density),
                isActive ? accentColor : (accentColor & 0x00FFFFFF) | 0x44000000
        );
        holder.binding.swatchCardRoot.setBackground(cardBg);

        GradientDrawable strip = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{bgColor, primaryColor, accentColor}
        );
        strip.setCornerRadius(10f * density);
        holder.binding.viewThemeGradient.setBackground(strip);

        holder.binding.dotPrimary.setBackground(makeCircle(primaryColor));
        holder.binding.dotAccent.setBackground(makeCircle(accentColor));

        holder.binding.tvThemeName.setText(theme.titleRes);
        holder.binding.tvThemeName.setTextColor(textPrimary);

        holder.binding.tvThemeMood.setText(theme.moodRes);
        holder.binding.tvThemeMood.setTextColor(textMuted);

        if (isActive) {
            holder.binding.tvThemeBadge.setText(R.string.theme_badge_active);
            holder.binding.tvThemeBadge.setTextColor(accentColor);
        } else if (isLocked) {
            holder.binding.tvThemeBadge.setText("🔒 " + ctx.getString(R.string.theme_badge_vip));
            holder.binding.tvThemeBadge.setTextColor(accentColor);
        } else if (theme.premium) {
            holder.binding.tvThemeBadge.setText("✓ " + ctx.getString(R.string.theme_badge_vip));
            holder.binding.tvThemeBadge.setTextColor(textMuted);
        } else {
            holder.binding.tvThemeBadge.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onSelect(theme));
    }

    private GradientDrawable makeCircle(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        return dot;
    }

    @Override
    public int getItemCount() {
        return themes.length;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemThemeSwatchBinding binding;

        Holder(ItemThemeSwatchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
