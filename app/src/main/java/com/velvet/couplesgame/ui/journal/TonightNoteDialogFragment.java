package com.velvet.couplesgame.ui.journal;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.journal.TonightNoteManager;
import com.velvet.couplesgame.databinding.DialogTonightNoteBinding;

/**
 * C 项 · 「今晚一句话」编辑弹窗（架架规格 §3.5 第 2 层）。
 *
 * <p>双方各一句 + 5 档心情标记；同一天**覆盖写**，不提供导出 / 分享
 * （安安 RED-A6 / RED-P）。提示语使用邀约式，不使用命令式（架架 §4 N3）。
 */
public class TonightNoteDialogFragment extends DialogFragment {

    public static final String TAG = "velvet_tonight_note";

    private DialogTonightNoteBinding binding;
    private AlertDialog dialog;

    public static TonightNoteDialogFragment newInstance() {
        return new TonightNoteDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogTonightNoteBinding.inflate(LayoutInflater.from(requireContext()));
        // P0 修复：listener 在 create() 前直接绑定，不传 null、不依赖 setOnShowListener。
        // 保存失败时 save() 不 dismiss，用户可修正后重试。
        dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .setPositiveButton(R.string.tonight_note_save, (d, which) -> save())
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismissAllowingStateLoss())
                .create();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        prefill();
    }

    private void prefill() {
        if (binding == null) return;
        TonightNoteManager manager = manager();
        String today = manager.getTodayIsoDate();
        if (binding.etTnNoteA != null) {
            binding.etTnNoteA.setText(manager.getNote(today) != null
                    ? manager.getNote(today).noteA : "");
        }
        if (binding.etTnNoteB != null) {
            binding.etTnNoteB.setText(manager.getNote(today) != null
                    ? manager.getNote(today).noteB : "");
        }
        if (binding.rbTnMood3 != null) binding.rbTnMood3.setChecked(true);
    }

    private void save() {
        if (binding == null) return;
        String noteA = binding.etTnNoteA != null
                ? binding.etTnNoteA.getText().toString() : "";
        String noteB = binding.etTnNoteB != null
                ? binding.etTnNoteB.getText().toString() : "";
        int mood = selectedMood();
        boolean ok = manager().saveNote(manager().getTodayIsoDate(), noteA, noteB, mood);
        if (ok) {
            Toast.makeText(requireContext(), R.string.tonight_note_saved_toast,
                    Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
        }
    }

    private int selectedMood() {
        if (binding == null) return -1;
        if (binding.rbTnMood1.isChecked()) return 1;
        if (binding.rbTnMood2.isChecked()) return 2;
        if (binding.rbTnMood3.isChecked()) return 3;
        if (binding.rbTnMood4.isChecked()) return 4;
        if (binding.rbTnMood5.isChecked()) return 5;
        return -1;
    }

    private TonightNoteManager manager() {
        return ((CouplesGameApp) requireActivity().getApplication())
                .getContainer().tonightNoteManager;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
