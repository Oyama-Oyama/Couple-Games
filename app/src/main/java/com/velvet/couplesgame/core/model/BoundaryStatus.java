package com.velvet.couplesgame.core.model;

public enum BoundaryStatus {
    YES,
    SOFT_LIMIT,
    HARD_NO;

    public BoundaryStatus next() {
        switch (this) {
            case YES:
                return SOFT_LIMIT;
            case SOFT_LIMIT:
                return HARD_NO;
            case HARD_NO:
            default:
                return YES;
        }
    }

    public static BoundaryStatus fromString(String value) {
        if (value == null) return YES;
        try {
            return BoundaryStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return YES;
        }
    }
}
