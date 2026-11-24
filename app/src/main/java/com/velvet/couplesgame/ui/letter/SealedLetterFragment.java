package com.velvet.couplesgame.ui.letter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.i18n.AssetContentLoader;
import com.velvet.couplesgame.core.letter.SealedLetter;
import com.velvet.couplesgame.core.letter.SealedLetterManager;
import com.velvet.couplesgame.databinding.FragmentSealedLetterBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Sealed Letter: one partner writes a letter, seals it with a wax stamp and
 * picks the day it may be opened. The body is encrypted on the device and
 * stays unreadable until that date.
 */
public class SealedLetterFragment extends Fragment {

    private FragmentSealedLetterBinding binding;
    private AppContainer container;

    private final List<String> promptTexts = new ArrayList<>();
    private final List<AssetContentLoader.SealDto> seals = new ArrayList<>();
    private final List<AssetContentLoader.LetterDelayDto> delays = new ArrayList<>();

    private int promptCursor = 0;
    private String selectedSealId = "seal_crimson";
    private int selectedDelayDays = 7;
    private boolean writingAsA = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSealedLetterBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        binding.btnLetterWrite.setOnClickListener(v -> openCompose());
        binding.btnLetterCancel.setOnClickListener(v -> {
            HapticHelper.tick(v);
            showInbox();
        });
        binding.btnLetterSeal.setOnClickListener(v -> sealLetter(v));
        binding.btnLetterPromptNext.setOnClickListener(v -> {
            HapticHelper.tick(v);
            promptCursor++;
            renderPrompt();
        });

        // "Who is writing?" lists the couple's own nicknames, not "Partner A/B".
        container.profileManager.observeState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null) return;
            binding.rbLetterAuthorA.setText(state.partnerA.name);
            binding.rbLetterAuthorB.setText(state.partnerB.name);
            renderInbox();
        });

        loadContent();
        renderInbox();
    }

    private void loadContent() {
        AssetContentLoader.SealedLetterBundleDto bundle =
                container.assetLoader.loadSealedLetter();
        if (bundle != null) {
            if (bundle.prompts != null) {
                for (AssetContentLoader.LetterPromptDto p : bundle.prompts) {
                    if (p != null && p.text != null) promptTexts.add(p.text);
                }
            }
            if (bundle.seals != null) seals.addAll(bundle.seals);
            if (bundle.delays != null) delays.addAll(bundle.delays);
        }
        if (seals.isEmpty()) selectedSealId = "seal_crimson";
        renderPrompt();
    }

    // ---------------- inbox ----------------

    private void showInbox() {
        binding.layoutLetterCompose.setVisibility(View.GONE);
        binding.layoutLetterInbox.setVisibility(View.VISIBLE);
        renderInbox();
    }

    private void renderInbox() {
        if (binding == null) return;
        SealedLetterManager manager = container.letterManager;

        List<SealedLetter> sealed = manager.getSealedLetters();
        List<SealedLetter> ready = manager.getReadyLetters();
        List<SealedLetter> opened = manager.getOpenedLetters();

        binding.tvLetterPendingCount.setText(getString(R.string.letter_pending_count,
                sealed.size() + ready.size()));
        binding.tvLetterReadyCount.setText(getString(R.string.letter_ready_count, ready.size()));
        binding.tvLetterOpenedCount.setText(getString(R.string.letter_opened_count, opened.size()));

        binding.layoutLetterSealedBox.setVisibility(sealed.isEmpty() ? View.GONE : View.VISIBLE);
        binding.layoutLetterReadyBox.setVisibility(ready.isEmpty() ? View.GONE : View.VISIBLE);
        binding.layoutLetterOpenedBox.setVisibility(opened.isEmpty() ? View.GONE : View.VISIBLE);

        fillSealedList(sealed);
        fillReadyList(ready);
        fillOpenedList(opened);
    }

    private void fillSealedList(List<SealedLetter> letters) {
        binding.layoutLetterSealedList.removeAllViews();
        for (SealedLetter letter : letters) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_sealed_letter, binding.layoutLetterSealedList, false);
            bindRow(row, letter, false);
            binding.layoutLetterSealedList.addView(row);
        }
    }

    private void fillReadyList(List<SealedLetter> letters) {
        binding.layoutLetterReadyList.removeAllViews();
        for (SealedLetter letter : letters) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_sealed_letter, binding.layoutLetterReadyList, false);
            bindRow(row, letter, true);
            binding.layoutLetterReadyList.addView(row);
        }
    }

    private void fillOpenedList(List<SealedLetter> letters) {
        binding.layoutLetterOpenedList.removeAllViews();
        for (SealedLetter letter : letters) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_sealed_letter, binding.layoutLetterOpenedList, false);
            bindRow(row, letter, true);
            binding.layoutLetterOpenedList.addView(row);
        }
    }

    private void bindRow(View row, SealedLetter letter, boolean canOpen) {
        android.widget.TextView title = row.findViewById(R.id.tv_letter_row_title);
        android.widget.TextView meta = row.findViewById(R.id.tv_letter_row_meta);
        android.widget.TextView status = row.findViewById(R.id.tv_letter_row_status);
        com.google.android.material.button.MaterialButton action =
                row.findViewById(R.id.btn_letter_row_action);

        title.setText(letter.title.isEmpty() ? getString(R.string.letter_untitled) : letter.title);

        String from = container.profileManager.getPlayerName(letter.fromPlayerA);
        String sealLabel = sealLabelOf(letter.sealId);
        int days = LetterRules.daysUntilReveal(letter, LocalDate.now());
        if (days > 0) {
            meta.setText(getString(R.string.letter_row_meta, from, sealLabel));
            status.setText(getString(R.string.letter_status_locked_days, days));
            status.setTextColor(mutedColor());
            action.setVisibility(View.GONE);
        } else if (canOpen) {
            meta.setText(getString(R.string.letter_row_meta, from, sealLabel));
            status.setText(getString(R.string.letter_status_ready));
            status.setTextColor(accentColor());
            action.setVisibility(View.VISIBLE);
            action.setText(letter.openedAt > 0
                    ? R.string.letter_action_reopen : R.string.letter_action_open);
            action.setOnClickListener(v -> openLetter(letter));
        } else {
            meta.setText(getString(R.string.letter_row_meta, from, sealLabel));
            status.setText(getString(R.string.letter_status_locked));
            status.setTextColor(mutedColor());
            action.setVisibility(View.GONE);
        }
    }

    private void openLetter(SealedLetter letter) {
        SealedLetter opened = container.letterManager.open(letter.id);
        if (opened == null) {
            android.widget.Toast.makeText(requireContext(),
                    R.string.letter_open_failed, android.widget.Toast.LENGTH_SHORT).show();
            renderInbox();
            return;
        }
        HapticHelper.confirm(binding.getRoot());
        PhysicsAnimator.flipCard3D(binding.layoutLetterCompose, null);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(opened.title.isEmpty() ? getString(R.string.letter_untitled) : opened.title)
                .setMessage(opened.body)
                .setPositiveButton(R.string.letter_action_close, null)
                .show();
        renderInbox();
    }

    // ---------------- compose ----------------

    private void openCompose() {
        HapticHelper.tick(binding.getRoot());
        binding.layoutLetterInbox.setVisibility(View.GONE);
        binding.layoutLetterCompose.setVisibility(View.VISIBLE);
        binding.etLetterTitle.setText("");
        binding.etLetterBody.setText("");
        binding.rbLetterAuthorA.setChecked(true);
        writingAsA = true;
        renderPrompt();
        renderSealOptions();
        renderDelayOptions();
    }

    private void renderPrompt() {
        if (binding == null || promptTexts.isEmpty()) return;
        binding.tvLetterPrompt.setText(LetterRules.nextPrompt(promptTexts, promptCursor));
    }

    private void renderSealOptions() {
        binding.rgLetterSeals.removeAllViews();
        for (AssetContentLoader.SealDto seal : seals) {
            RadioButton row = new RadioButton(requireContext());
            row.setId(View.generateViewId());
            row.setText(seal.label);
            row.setTextColor(primaryColor());
            row.setTag(seal.id);
            binding.rgLetterSeals.addView(row);
            if (seal.id.equals(selectedSealId)) {
                binding.rgLetterSeals.check(row.getId());
            } else if (binding.rgLetterSeals.getCheckedRadioButtonId() == View.NO_ID
                    && seals.size() > 0 && seals.indexOf(seal) == 0) {
                binding.rgLetterSeals.check(row.getId());
            }
        }
    }

    private void renderDelayOptions() {
        binding.rgLetterDelays.removeAllViews();
        boolean anyChecked = false;
        for (AssetContentLoader.LetterDelayDto delay : delays) {
            RadioButton row = new RadioButton(requireContext());
            row.setId(View.generateViewId());
            row.setText(delay.label);
            row.setTextColor(primaryColor());
            row.setTag(delay.days);
            binding.rgLetterDelays.addView(row);
            if (delay.days == selectedDelayDays) {
                binding.rgLetterDelays.check(row.getId());
                anyChecked = true;
            }
        }
        if (!anyChecked && binding.rgLetterDelays.getChildCount() > 0) {
            binding.rgLetterDelays.check(binding.rgLetterDelays.getChildAt(0).getId());
        }
    }

    private void sealLetter(View v) {
        if (binding == null) return;
        String body = binding.etLetterBody.getText() != null
                ? binding.etLetterBody.getText().toString().trim() : "";
        if (body.isEmpty()) {
            HapticHelper.tick(v);
            binding.inputLetterBody.setError(getString(R.string.letter_body_required));
            return;
        }
        binding.inputLetterBody.setError(null);

        String title = binding.etLetterTitle.getText() != null
                ? binding.etLetterTitle.getText().toString() : "";

        writingAsA = binding.rbLetterAuthorA.isChecked();

        int checkedSeal = binding.rgLetterSeals.getCheckedRadioButtonId();
        Object sealTag = checkedSeal != View.NO_ID
                ? binding.rgLetterSeals.findViewById(checkedSeal).getTag() : null;
        if (sealTag instanceof String) selectedSealId = (String) sealTag;

        int checkedDelay = binding.rgLetterDelays.getCheckedRadioButtonId();
        Object delayTag = checkedDelay != View.NO_ID
                ? binding.rgLetterDelays.findViewById(checkedDelay).getTag() : null;
        if (delayTag instanceof Integer) selectedDelayDays = (Integer) delayTag;

        long now = System.currentTimeMillis();
        LocalDate revealDate = LetterRules.revealDateFor(now, selectedDelayDays);

        SealedLetter sealed = container.letterManager.seal(
                writingAsA,
                LetterRules.sanitize(title, LetterRules.MAX_TITLE),
                LetterRules.sanitize(body, LetterRules.MAX_BODY),
                revealDate,
                selectedSealId);

        if (sealed == null) {
            HapticHelper.tick(v);
            android.widget.Toast.makeText(requireContext(),
                    R.string.letter_seal_failed, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        HapticHelper.confirm(binding.getRoot());
        container.contentRepository.recordSessionStat("SEALED_LETTER", 1, 1, 0);
        android.widget.Toast.makeText(requireContext(),
                getString(R.string.letter_sealed_toast, LetterRules.formatDate(revealDate)),
                android.widget.Toast.LENGTH_LONG).show();
        showInbox();
    }

    // ---------------- helpers ----------------

    private String sealLabelOf(String sealId) {
        for (AssetContentLoader.SealDto seal : seals) {
            if (seal.id.equals(sealId)) return seal.label;
        }
        return getString(R.string.letter_seal_default);
    }

    private int accentColor() {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.velvetAccent, tv, true);
        return tv.data;
    }

    private int mutedColor() {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.velvetTextMuted, tv, true);
        return tv.data;
    }

    private int primaryColor() {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.velvetTextPrimary, tv, true);
        return tv.data;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
