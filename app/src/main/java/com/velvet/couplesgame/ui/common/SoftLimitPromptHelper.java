package com.velvet.couplesgame.ui.common;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import com.velvet.couplesgame.core.engine.ConsentFilter;
import java.util.Collection;

public final class SoftLimitPromptHelper {
    private SoftLimitPromptHelper() {}

    public static boolean bind(ConsentFilter consentFilter,
                               Collection<String> tags,
                               TextView warning,
                               CheckBox acknowledgment,
                               View... gatedActions) {
        boolean requiresAcknowledgment = consentFilter.hasSoftLimit(tags);
        warning.setVisibility(requiresAcknowledgment ? View.VISIBLE : View.GONE);
        acknowledgment.setVisibility(requiresAcknowledgment ? View.VISIBLE : View.GONE);
        acknowledgment.setOnCheckedChangeListener(null);
        acknowledgment.setChecked(false);
        setActionsEnabled(gatedActions, !requiresAcknowledgment);
        acknowledgment.setOnCheckedChangeListener((button, checked) ->
                setActionsEnabled(gatedActions, checked));
        return requiresAcknowledgment;
    }

    private static void setActionsEnabled(View[] actions, boolean enabled) {
        for (View action : actions) {
            action.setEnabled(enabled);
        }
    }
}
