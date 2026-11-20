package com.velvet.couplesgame.ui.onboarding;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.profile.CoupleProfileManager;
import com.velvet.couplesgame.databinding.DialogOnboardingBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;

/**
 * 2-step atmospheric onboarding & couple profile editor with localized pronoun options
 * persisted as canonical keys ("He", "She", "They").
 */
public class OnboardingDialogFragment extends DialogFragment {
    private static final String ARG_FIRST_LAUNCH = "first_launch";
    private static final String[] CANONICAL_PRONOUNS = new String[]{"He", "She", "They"};

    private DialogOnboardingBinding binding;
    private AppContainer container;
    private int currentStep = 1;

    public static OnboardingDialogFragment newInstance(boolean isFirstLaunch) {
        OnboardingDialogFragment dialog = new OnboardingDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FIRST_LAUNCH, isFirstLaunch);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();
        binding = DialogOnboardingBinding.inflate(LayoutInflater.from(requireContext()));
        boolean isFirstLaunch = getArguments() != null && getArguments().getBoolean(ARG_FIRST_LAUNCH, false);
        setCancelable(!isFirstLaunch);

        String[] displayPronouns = new String[]{
                getString(R.string.pronoun_he),
                getString(R.string.pronoun_she),
                getString(R.string.pronoun_they)
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                displayPronouns
        );
        binding.spinnerPronounA.setAdapter(spinnerAdapter);
        binding.spinnerPronounB.setAdapter(spinnerAdapter);

        CoupleProfileManager.CoupleState state = container.profileManager.getState();
        binding.etNameA.setText(state.partnerA.name);
        binding.etNameB.setText(state.partnerB.name);
        binding.etSafeWord.setText(state.safeWord);

        selectPronoun(binding.spinnerPronounA, state.partnerA.pronoun);
        selectPronoun(binding.spinnerPronounB, state.partnerB.pronoun);

        binding.btnOnboardingNext.setOnClickListener(v -> {
            HapticHelper.tick(v);
            if (currentStep == 1) {
                currentStep = 2;
                renderStep();
            } else {
                saveAndClose();
            }
        });

        binding.btnOnboardingBack.setOnClickListener(v -> {
            HapticHelper.tick(v);
            if (currentStep == 2) {
                currentStep = 1;
                renderStep();
            } else if (!isFirstLaunch) {
                dismiss();
            }
        });

        renderStep();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    private void renderStep() {
        boolean stepOne = (currentStep == 1);
        binding.layoutStep1.setVisibility(stepOne ? View.VISIBLE : View.GONE);
        binding.layoutStep2.setVisibility(stepOne ? View.GONE : View.VISIBLE);
        binding.tvStepCounter.setText(stepOne ? "1 / 2" : "2 / 2");
        binding.btnOnboardingNext.setText(stepOne ? R.string.onboarding_continue : R.string.onboarding_begin);
        binding.btnOnboardingBack.setVisibility(stepOne ? View.INVISIBLE : View.VISIBLE);
    }

    private void saveAndClose() {
        HapticHelper.confirm(binding.getRoot());
        String nameA = binding.etNameA.getText().toString();
        String nameB = binding.etNameB.getText().toString();
        int idxA = Math.max(0, Math.min(CANONICAL_PRONOUNS.length - 1, binding.spinnerPronounA.getSelectedItemPosition()));
        int idxB = Math.max(0, Math.min(CANONICAL_PRONOUNS.length - 1, binding.spinnerPronounB.getSelectedItemPosition()));
        String safeWord = binding.etSafeWord.getText().toString();

        container.profileManager.saveProfiles(
                nameA, CANONICAL_PRONOUNS[idxA],
                nameB, CANONICAL_PRONOUNS[idxB],
                safeWord,
                true
        );
        dismiss();
    }

    private void selectPronoun(Spinner spinner, String saved) {
        if (saved == null) return;
        String lower = saved.toLowerCase();
        if (lower.startsWith("he")) spinner.setSelection(0);
        else if (lower.startsWith("sh")) spinner.setSelection(1);
        else spinner.setSelection(2);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
