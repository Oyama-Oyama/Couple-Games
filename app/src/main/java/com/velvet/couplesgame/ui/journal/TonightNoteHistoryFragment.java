package com.velvet.couplesgame.ui.journal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.databinding.FragmentTonightNoteHistoryBinding;

import java.util.Locale;

/**
 * C 项 · 「今晚一句话」按日回看页（架架规格 §3.5 第 3 层）。
 *
 * <p>默认折叠摘要，点击展开全文；**无导出 / 分享 / 批量操作**。
 */
public class TonightNoteHistoryFragment extends Fragment {

    private FragmentTonightNoteHistoryBinding binding;
    private TonightNoteAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTonightNoteHistoryBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new TonightNoteAdapter();
        adapter.setLocale(Locale.getDefault());
        binding.rvTnhNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTnhNotes.setAdapter(adapter);

        ((CouplesGameApp) requireActivity().getApplication())
                .getContainer().tonightNoteManager.observeNotes()
                .observe(getViewLifecycleOwner(), notes -> {
                    if (binding == null) return;
                    adapter.submit(notes);
                    boolean empty = notes == null || notes.isEmpty();
                    binding.tvTnhEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    binding.rvTnhNotes.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
