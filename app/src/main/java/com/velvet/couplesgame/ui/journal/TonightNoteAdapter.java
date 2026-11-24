package com.velvet.couplesgame.ui.journal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.journal.TonightNote;
import com.velvet.couplesgame.core.profile.TogetherDateFormat;
import com.velvet.couplesgame.databinding.ItemTonightNoteBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * C 项 · 「今晚一句话」按日列表适配器。
 *
 * <p><b>隐私姿态</b>：列表默认**折叠**（只显示摘要），需点击才展开全文 ——
 * 与 FLAG_SECURE 防截屏叙事一致（架架 §3.5）。
 */
public class TonightNoteAdapter extends RecyclerView.Adapter<TonightNoteAdapter.NoteViewHolder> {

    private final List<TonightNote> items = new ArrayList<>();
    private Locale locale = Locale.getDefault();

    public void submit(List<TonightNote> notes) {
        items.clear();
        if (notes != null) items.addAll(notes);
        notifyDataSetChanged();
    }

    public void setLocale(Locale locale) {
        if (locale != null) this.locale = locale;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTonightNoteBinding binding = ItemTonightNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        TonightNote note = items.get(position);
        holder.bind(note, locale);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final ItemTonightNoteBinding binding;

        NoteViewHolder(ItemTonightNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TonightNote note, Locale locale) {
            String date = TogetherDateFormat.display(note.dateIso, locale);
            binding.tvTnItemDate.setText(date != null ? date : note.dateIso);
            binding.tvTnItemMood.setText(moodLabel(note.moodScore));
            binding.tvTnItemSummary.setText(note.summary());
            binding.tvTnItemFull.setVisibility(View.GONE);
            binding.tvTnItemFull.setText(fullText(note));

            binding.rootTnItem.setOnClickListener(v -> {
                boolean expanded = binding.tvTnItemFull.getVisibility() == View.VISIBLE;
                binding.tvTnItemFull.setVisibility(expanded ? View.GONE : View.VISIBLE);
                binding.tvTnItemSummary.setMaxLines(expanded ? 2 : 6);
            });
        }

        private String moodLabel(int score) {
            if (score < 1 || score > 5) return "";
            return binding.getRoot().getContext().getString(
                    score == 1 ? R.string.mood_1
                            : score == 2 ? R.string.mood_2
                            : score == 3 ? R.string.mood_3
                            : score == 4 ? R.string.mood_4
                            : R.string.mood_5);
        }

        private String fullText(TonightNote note) {
            StringBuilder sb = new StringBuilder();
            if (!note.noteA.trim().isEmpty()) {
                sb.append(note.noteA.trim());
            }
            if (!note.noteB.trim().isEmpty()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(note.noteB.trim());
            }
            return sb.toString();
        }
    }
}
