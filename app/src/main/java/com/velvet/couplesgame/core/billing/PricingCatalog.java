package com.velvet.couplesgame.core.billing;

/**
 * Fallback display pricing & anchor metadata (overridden live by Google Play Billing ProductDetails).
 * Ensures logical anchor ladder: Weekly ($4.99) < Annual ($19.99) < Lifetime ($29.99).
 */
public final class PricingCatalog {
    public static final double FALLBACK_WEEKLY_USD = 4.99;
    public static final double FALLBACK_ANNUAL_USD = 19.99;
    public static final double FALLBACK_LIFETIME_USD = 29.99;

    public static final String LABEL_WEEKLY = "$4.99 / wk";
    public static final String LABEL_ANNUAL = "$19.99 / yr";
    public static final String LABEL_LIFETIME = "$29.99";

    private PricingCatalog() {}
}
