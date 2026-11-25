package com.velvet.couplesgame.ui.common;

import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.ui.journal.TonightNoteDialogFragment;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Optional, gentle session wrap-up shared by games with a clear completion point. */
public final class AftercareDialogHelper {
    private static final Set<Fragment> SHOWN_FRAGMENTS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private AftercareDialogHelper() {}

    public static void show(Fragment fragment) {
        if (fragment == null || !fragment.isAdded() || !SHOWN_FRAGMENTS.add(fragment)) return;
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.datenight_aftercare_title)
                .setMessage(R.string.datenight_aftercare_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.datenight_aftercare_button, null)
                .setNeutralButton(R.string.datenight_aftercare_note, (dialog, which) ->
                        TonightNoteDialogFragment.newInstance()
                                .show(fragment.getChildFragmentManager(), TonightNoteDialogFragment.TAG))
                .show();
    }

    public static void showBeforeLeaving(Fragment fragment, Runnable onLeave) {
        if (fragment == null || !fragment.isAdded()) {
            if (onLeave != null) onLeave.run();
            return;
        }
        if (!SHOWN_FRAGMENTS.add(fragment)) {
            if (onLeave != null) onLeave.run();
            return;
        }
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.datenight_aftercare_title)
                .setMessage(R.string.datenight_aftercare_body)
                .setNegativeButton(R.string.datenight_aftercare_stay, null)
                .setPositiveButton(R.string.datenight_aftercare_leave, (dialog, which) -> {
                    if (onLeave != null) onLeave.run();
                })
                .setNeutralButton(R.string.datenight_aftercare_note, (dialog, which) ->
                        TonightNoteDialogFragment.newInstance()
                                .show(fragment.getChildFragmentManager(), TonightNoteDialogFragment.TAG))
                .show();
    }
}
