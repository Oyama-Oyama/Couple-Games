package com.velvet.couplesgame.ui.dateplanner;

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
import com.velvet.couplesgame.core.dateplanner.DatePlanManager;
import com.velvet.couplesgame.core.dateplanner.DatePlannerEngine;
import com.velvet.couplesgame.databinding.FragmentDatePlannerBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.common.PassPhoneDialog;
import com.velvet.couplesgame.ui.common.PhysicsAnimator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Date Planner: both partners review the same shortlist of date activities
 * without seeing each other's choices, then the agreed ones become the plan.
 * Finally the couple picks a day, a time slot and writes a note; the result is
 * stored as the upcoming date plan and shown on the home screen.
 */
public class DatePlannerFragment extends Fragment {

    private static final String REQ_HANDOFF = "date_planner_handoff";
    private static final int SHORTLIST_SIZE = DatePlannerEngine.DEFAULT_SHORTLIST_SIZE;
    private static final int QUICK_DATE_OPTIONS = 5;

    private FragmentDatePlannerBinding binding;
    private AppContainer container;
    private final DatePlannerEngine engine = new DatePlannerEngine();
    private final List<DatePlannerEngine.Activity> matches = new ArrayList<>();
    private DatePlannerEngine.Activity selectedActivity;
    private LocalDate selectedDate;
    private int selectedSlot = -1;
    private boolean loaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDatePlannerBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        getChildFragmentManager().setFragmentResultListener(REQ_HANDOFF, getViewLifecycleOwner(),
                (key, bundle) -> {
                    if (bundle.getBoolean(PassPhoneDialog.RESULT_CONFIRMED, false)) {
                        engine.handOffToPartnerB();
                        renderStage();
                    }
                });

        binding.btnDpLike.setOnClickListener(v -> onVote(v, true));
        binding.btnDpPass.setOnClickListener(v -> onVote(v, false));
        binding.btnDpNextStep.setOnClickListener(v -> onConfirmMatch(v));
        binding.btnDpSave.setOnClickListener(v -> onSavePlan(v));
        binding.btnDpRestart.setOnClickListener(v -> {
            HapticHelper.tick(v);
            restart();
        });

        loadContent();
    }

    private void loadContent() {
        int maxTier = container.getEffectiveMaxTier();
        container.contentRepository.loadDatePlannerAsync(activities -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                engine.loadPool(activities);
                engine.buildShortlist(SHORTLIST_SIZE, maxTier);
                loaded = true;
                renderStage();
            });
        });
    }

    // ---------------- voting ----------------

    private void onVote(View v, boolean liked) {
        if (binding == null) return;
        DatePlannerEngine.Phase phase = engine.getPhase();
        boolean recorded = phase == DatePlannerEngine.Phase.PICK_A
                ? engine.recordPickA(liked)
                : engine.recordPickB(liked);
        if (!recorded) return;

        HapticHelper.tick(v);
        if (phase == DatePlannerEngine.Phase.PICK_A && engine.getCurrentActivity() == null) {
            // Partner A finished the shortlist - hand the phone over.
            binding.layoutDpCandidate.setVisibility(View.INVISIBLE);
            binding.layoutDpActions.setVisibility(View.GONE);
            String nameB = container.profileManager.getPlayerName(false);
            PassPhoneDialog.newInstance(REQ_HANDOFF, nameB)
                    .show(getChildFragmentManager(), "date_planner_handoff");
            return;
        }
        if (phase == DatePlannerEngine.Phase.PICK_B && engine.getPhase() == DatePlannerEngine.Phase.RESULT) {
            showResult();
            return;
        }
        renderStage();
    }

    private void showResult() {
        if (binding == null) return;
        matches.clear();
        matches.addAll(engine.getMutualMatches());

        binding.layoutDpCandidate.setVisibility(View.GONE);
        binding.layoutDpActions.setVisibility(View.GONE);
        binding.layoutDpResult.setVisibility(View.VISIBLE);
        binding.tvDpStage.setText(R.string.date_planner_stage_result);

        binding.tvDpResultTitle.setText(getString(R.string.date_planner_result_count, matches.size()));
        boolean hasMatch = !matches.isEmpty();
        binding.tvDpEmpty.setVisibility(hasMatch ? View.GONE : View.VISIBLE);
        binding.btnDpNextStep.setVisibility(hasMatch ? View.VISIBLE : View.GONE);
        binding.rgDpMatches.removeAllViews();

        if (hasMatch) {
            for (int i = 0; i < matches.size(); i++) {
                DatePlannerEngine.Activity activity = matches.get(i);
                RadioButton row = new RadioButton(requireContext());
                row.setId(View.generateViewId());
                row.setText(activity.title);
                row.setTextColor(resolveTextColor());
                row.setTag(activity.id);
                binding.rgDpMatches.addView(row);
                if (i == 0) {
                    // Must be checked after the view joins the group so RadioGroup tracks it.
                    binding.rgDpMatches.check(row.getId());
                    selectedActivity = activity;
                }
            }
        }
        HapticHelper.confirm(binding.getRoot());
    }

    private void onConfirmMatch(View v) {
        if (binding == null) return;
        int checkedId = binding.rgDpMatches.getCheckedRadioButtonId();
        if (checkedId == View.NO_ID) {
            return;
        }
        View checked = binding.rgDpMatches.findViewById(checkedId);
        if (checked == null) {
            return;
        }
        Object tag = checked.getTag();
        selectedActivity = engine.findById(tag == null ? null : tag.toString());
        if (selectedActivity == null) return;

        HapticHelper.confirm(v);
        binding.layoutDpResult.setVisibility(View.GONE);
        binding.layoutDpSchedule.setVisibility(View.VISIBLE);
        binding.tvDpStage.setText(R.string.date_planner_stage_schedule);
        buildQuickDates();
        binding.rgDpSlots.check(R.id.rb_dp_slot_evening);
        selectedSlot = 0;
    }

    // ---------------- scheduling ----------------

    /** Offer today + the next few days as one-tap options. */
    private void buildQuickDates() {
        if (binding == null) return;
        binding.layoutDpDates.removeAllViews();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < QUICK_DATE_OPTIONS; i++) {
            LocalDate date = today.plusDays(i);
            final View chip = createDateChip(formatQuickDate(date, i), null);
            chip.setOnClickListener(v -> {
                HapticHelper.tick(v);
                selectedDate = date;
                binding.inputDpCustomDate.setVisibility(View.GONE);
                binding.inputDpCustomDate.setError(null);
                highlightQuickDates(chip);
            });
            binding.layoutDpDates.addView(chip);
        }
        final View custom = createDateChip(getString(R.string.date_planner_date_custom), null);
        custom.setOnClickListener(v -> {
            HapticHelper.tick(v);
            binding.inputDpCustomDate.setVisibility(View.VISIBLE);
            highlightQuickDates(custom);
        });
        binding.layoutDpDates.addView(custom);

        selectedDate = today;
        highlightQuickDates(binding.layoutDpDates.getChildAt(0));
    }

    private View createDateChip(String label, View.OnClickListener listener) {        com.google.android.material.button.MaterialButton chip =
                new com.google.android.material.button.MaterialButton(
                        requireContext(), null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
        chip.setText(label);
        chip.setTextSize(11f);
        chip.setPadding(4, 0, 4, 0);
        chip.setInsetTop(0);
        chip.setInsetBottom(0);
        chip.setAllCaps(false);
        chip.setMinWidth(0);
        chip.setMinimumWidth(0);
        chip.setOnClickListener(listener);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginEnd(6);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void highlightQuickDates(View active) {
        if (binding == null) return;
        for (int i = 0; i < binding.layoutDpDates.getChildCount(); i++) {
            View child = binding.layoutDpDates.getChildAt(i);
            if (child instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) child)
                        .setStrokeWidth(child == active ? 3 : 1);
                ((com.google.android.material.button.MaterialButton) child)
                        .setAlpha(child == active ? 1.0f : 0.6f);
            }
        }
    }

    private String formatQuickDate(LocalDate date, int offset) {
        if (offset == 0) return getString(R.string.date_planner_date_today);
        if (offset == 1) return getString(R.string.date_planner_date_tomorrow);
        String day = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        return date.getMonthValue() + "/" + date.getDayOfMonth() + " " + day;
    }

    private void onSavePlan(View v) {
        if (binding == null || selectedActivity == null) return;

        LocalDate date = selectedDate;
        if (binding.inputDpCustomDate.getVisibility() == View.VISIBLE) {
            String raw = binding.etDpCustomDate.getText() != null
                    ? binding.etDpCustomDate.getText().toString().trim() : "";
            LocalDate parsed = parseDate(raw);
            if (parsed == null) {
                binding.inputDpCustomDate.setError(getString(R.string.date_planner_invalid_date));
                return;
            }
            binding.inputDpCustomDate.setError(null);
            date = parsed;
        }
        if (date == null) {
            date = LocalDate.now();
        }

        String note = binding.etDpNote.getText() != null
                ? binding.etDpNote.getText().toString().trim() : "";
        container.datePlanManager.savePlan(date, selectedActivity.id, selectedActivity.title, note);

        HapticHelper.confirm(v);
        binding.layoutDpSchedule.setVisibility(View.GONE);
        binding.layoutDpSaved.setVisibility(View.VISIBLE);
        binding.btnDpRestart.setVisibility(View.VISIBLE);
        binding.tvDpStage.setText(R.string.date_planner_stage_saved);

        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
        String when = formatDate(date);
        binding.tvDpSavedTitle.setText(selectedActivity.title);
        binding.tvDpSavedDetail.setText(getString(R.string.date_planner_saved_detail,
                when, slotLabel(), note.isEmpty() ? getString(R.string.date_planner_no_note) : note));
        if (days == 0) {
            binding.tvDpSavedDetail.setText(getString(R.string.date_planner_saved_today,
                    slotLabel(), note.isEmpty() ? getString(R.string.date_planner_no_note) : note));
        }

        // Record the planning session in the Date Night History.
        container.contentRepository.recordSessionStat("DATE_PLANNER", 1, 1, 0);
    }

    private String slotLabel() {
        int checkedId = binding.rgDpSlots.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_dp_slot_night) return getString(R.string.date_planner_slot_night);
        if (checkedId == R.id.rb_dp_slot_day) return getString(R.string.date_planner_slot_day);
        return getString(R.string.date_planner_slot_evening);
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] patterns = {"yyyy-MM-dd", "yyyy/M/d", "yyyy.M.d"};
        for (String p : patterns) {
            try {
                return LocalDate.parse(raw, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private int resolveTextColor() {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (!requireContext().getTheme().resolveAttribute(R.attr.velvetTextPrimary, tv, true)) {
            return requireContext().getResources().getColor(android.R.color.primary_text_dark);
        }
        if (tv.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(requireContext(), tv.resourceId);
        }
        return tv.data;
    }

    // ---------------- rendering ----------------

    private void renderStage() {
        if (binding == null || !loaded) return;
        DatePlannerEngine.Phase phase = engine.getPhase();
        if (phase == DatePlannerEngine.Phase.EMPTY) {
            binding.layoutDpCandidate.setVisibility(View.GONE);
            binding.layoutDpActions.setVisibility(View.GONE);
            binding.tvDpStage.setText(R.string.date_planner_empty);
            return;
        }

        binding.layoutDpResult.setVisibility(View.GONE);
        binding.layoutDpSchedule.setVisibility(View.GONE);
        binding.layoutDpSaved.setVisibility(View.GONE);
        binding.btnDpRestart.setVisibility(View.GONE);
        binding.layoutDpCandidate.setVisibility(View.VISIBLE);
        binding.layoutDpActions.setVisibility(View.VISIBLE);

        DatePlannerEngine.Activity current = engine.getCurrentActivity();
        if (current == null) return;

        boolean isA = phase == DatePlannerEngine.Phase.PICK_A;
        String playerName = container.profileManager.getPlayerName(isA);
        binding.tvDpStage.setText(getString(R.string.date_planner_stage_pick,
                playerName, engine.getCursor() + 1, engine.getShortlistSize()));

        binding.progressDp.setMax(engine.getShortlistSize());
        binding.progressDp.setProgress(engine.getCursor());

        binding.tvDpCategory.setText(categoryLabel(current.category));
        binding.tvDpTitle.setText(current.title);
        binding.tvDpDetail.setText(current.detail);
        String tierLabel = getString(com.velvet.couplesgame.core.i18n.TagDisplayMapper
                .resolveTierNameRes(current.tier));
        binding.tvDpMeta.setText(getString(R.string.date_planner_meta,
                tierLabel + " · " + getString(R.string.date_planner_duration_fmt, current.durationMin)));
        PhysicsAnimator.flipCard3D(binding.layoutDpCandidate, null);
    }

    private void restart() {
        if (binding == null) return;
        matches.clear();
        selectedActivity = null;
        selectedDate = null;
        selectedSlot = -1;
        binding.etDpNote.setText("");
        binding.etDpCustomDate.setText("");
        binding.inputDpCustomDate.setVisibility(View.GONE);
        engine.resetSession();
        int maxTier = container.getEffectiveMaxTier();
        engine.buildShortlist(SHORTLIST_SIZE, maxTier);
        renderStage();
    }

    private String categoryLabel(String category) {
        if (category == null) return "";
        switch (category) {
            case "dining": return getString(R.string.date_planner_cat_dining);
            case "outdoor": return getString(R.string.date_planner_cat_outdoor);
            case "indoor": return getString(R.string.date_planner_cat_indoor);
            case "culture": return getString(R.string.date_planner_cat_culture);
            case "getaway": return getString(R.string.date_planner_cat_getaway);
            case "ritual": return getString(R.string.date_planner_cat_ritual);
            default: return category;
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
