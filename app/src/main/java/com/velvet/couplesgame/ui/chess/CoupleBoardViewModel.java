package com.velvet.couplesgame.ui.chess;

import androidx.lifecycle.ViewModel;

/**
 * Survives configuration changes & theme updates so an active Couple Board match never resets.
 */
public class CoupleBoardViewModel extends ViewModel {
    private final CoupleBoardEngine engine = new CoupleBoardEngine();

    public CoupleBoardEngine getEngine() {
        return engine;
    }
}
