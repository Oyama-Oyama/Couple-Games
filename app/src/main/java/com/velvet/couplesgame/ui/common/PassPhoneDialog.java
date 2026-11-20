package com.velvet.couplesgame.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.R;

/**
 * Configuration-change safe privacy shield dialog using AndroidX Fragment Result API.
 */
public class PassPhoneDialog extends DialogFragment {
    private static final String ARG_REQUEST_KEY = "request_key";
    private static final String ARG_PLAYER_NAME = "player_name";
    public static final String RESULT_CONFIRMED = "confirmed";

    public static PassPhoneDialog newInstance(String requestKey, String nextPlayerName) {
        PassPhoneDialog dialog = new PassPhoneDialog();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putString(ARG_PLAYER_NAME, nextPlayerName);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setCancelable(false);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pass_phone, null);
        String name = getArguments() != null ? getArguments().getString(ARG_PLAYER_NAME, "Partner") : "Partner";
        String requestKey = getArguments() != null ? getArguments().getString(ARG_REQUEST_KEY) : null;

        TextView tvMsg = view.findViewById(R.id.tv_pass_message);
        MaterialButton btnReveal = view.findViewById(R.id.btn_reveal);

        tvMsg.setText(getString(R.string.pass_phone_message, name));
        btnReveal.setText(getString(R.string.btn_ready, name));
        btnReveal.setOnClickListener(v -> {
            HapticHelper.tick(v);
            if (requestKey != null) {
                Bundle result = new Bundle();
                result.putBoolean(RESULT_CONFIRMED, true);
                getParentFragmentManager().setFragmentResult(requestKey, result);
            }
            dismiss();
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }
}
