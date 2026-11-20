package com.velvet.couplesgame.core.billing;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Full Google Play Billing Library 7.x bridge supporting:
 * - Weekly Subscription (velvet_weekly_pass)
 * - Annual Subscription (velvet_annual_pass)
 * - One-Time Lifetime Unlock (velvet_lifetime_unlock)
 * Syncs owned entitlements immediately into offline EntitlementManager.
 */
public class PlayBillingBridge implements PurchasesUpdatedListener {
    public static final String SKU_LIFETIME = "velvet_lifetime_unlock";
    public static final String SKU_WEEKLY_PASS = "velvet_weekly_pass";
    public static final String SKU_ANNUAL_PASS = "velvet_annual_pass";

    public interface BillingUiListener {
        void onCatalogUpdated();
        void onPurchaseSuccess();
        void onBillingMessage(String message);
    }

    private final BillingClient billingClient;
    private final EntitlementManager entitlementManager;
    private final Map<String, ProductDetails> productCatalog = new ConcurrentHashMap<>();
    private BillingUiListener uiListener;

    public PlayBillingBridge(Context context, EntitlementManager entitlementManager) {
        this.entitlementManager = entitlementManager;
        this.billingClient = BillingClient.newBuilder(context.getApplicationContext())
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();
    }

    public void setUiListener(@Nullable BillingUiListener listener) {
        this.uiListener = listener;
    }

    public void startConnectionAndSync() {
        if (billingClient.isReady()) {
            queryCatalog();
            queryActiveEntitlements();
            return;
        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryCatalog();
                    queryActiveEntitlements();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Offline or Play Store unavailable: cached offline EntitlementManager remains valid
            }
        });
    }

    public void queryCatalog() {
        if (!billingClient.isReady()) return;

        QueryProductDetailsParams inAppParams = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(SKU_LIFETIME)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                ))
                .build();

        billingClient.queryProductDetailsAsync(inAppParams, (result, detailsList) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && detailsList != null) {
                for (ProductDetails d : detailsList) {
                    productCatalog.put(d.getProductId(), d);
                }
                if (uiListener != null) uiListener.onCatalogUpdated();
            }
        });

        List<QueryProductDetailsParams.Product> subProducts = new ArrayList<>();
        subProducts.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_WEEKLY_PASS)
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        subProducts.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_ANNUAL_PASS)
                .setProductType(BillingClient.ProductType.SUBS)
                .build());

        QueryProductDetailsParams subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subProducts)
                .build();

        billingClient.queryProductDetailsAsync(subsParams, (result, detailsList) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && detailsList != null) {
                for (ProductDetails d : detailsList) {
                    productCatalog.put(d.getProductId(), d);
                }
                if (uiListener != null) uiListener.onCatalogUpdated();
            }
        });
    }

    public String getFormattedPrice(String sku, String fallbackPrice) {
        ProductDetails details = productCatalog.get(sku);
        if (details == null) return fallbackPrice;

        if (BillingClient.ProductType.INAPP.equals(details.getProductType())) {
            ProductDetails.OneTimePurchaseOfferDetails offer = details.getOneTimePurchaseOfferDetails();
            return offer != null ? offer.getFormattedPrice() : fallbackPrice;
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
        if (offers != null && !offers.isEmpty()) {
            List<ProductDetails.PricingPhase> phases = offers.get(0).getPricingPhases().getPricingPhaseList();
            if (!phases.isEmpty()) {
                return phases.get(phases.size() - 1).getFormattedPrice();
            }
        }
        return fallbackPrice;
    }

    public boolean launchPurchaseFlow(Activity activity, String sku) {
        if (!billingClient.isReady()) {
            startConnectionAndSync();
            return false;
        }
        ProductDetails details = productCatalog.get(sku);
        if (details == null) {
            queryCatalog();
            return false;
        }

        BillingFlowParams.ProductDetailsParams.Builder paramsBuilder =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details);

        if (BillingClient.ProductType.SUBS.equals(details.getProductType())) {
            List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
            if (offers == null || offers.isEmpty()) return false;
            paramsBuilder.setOfferToken(offers.get(0).getOfferToken());
        }

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(paramsBuilder.build()))
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        return result.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    public void queryActiveEntitlements() {
        if (!billingClient.isReady()) return;

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (result, purchases) -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        handlePurchases(purchases, false);
                    }
                }
        );

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                (result, purchases) -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        handlePurchases(purchases, true);
                    }
                }
        );
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        int code = billingResult.getResponseCode();
        if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
            boolean unlockedNow = handlePurchases(purchases, false);
            if (unlockedNow && uiListener != null) {
                uiListener.onPurchaseSuccess();
            }
        } else if (code != BillingClient.BillingResponseCode.USER_CANCELED && uiListener != null) {
            uiListener.onBillingMessage(billingResult.getDebugMessage());
        }
    }

    /**
     * Pure-Java decision helper for subscription state reconciliation:
     * - If an active subscription is present in any callback/query, activate immediately.
     * - Only deactivate an expired subscription during a full SUBS query.
     */
    public static boolean resolveNextSubscriptionState(boolean currentSubActive,
                                                       boolean foundActiveSubInBatch,
                                                       boolean isFullSubscriptionQuery) {
        if (foundActiveSubInBatch) return true;
        if (isFullSubscriptionQuery) return false;
        return currentSubActive;
    }

    private boolean handlePurchases(List<Purchase> purchases, boolean isSubscriptionQuery) {
        boolean foundActiveSub = false;
        boolean anyPurchased = false;

        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    anyPurchased = true;
                    acknowledgeIfNeeded(purchase);
                    if (purchase.getProducts().contains(SKU_LIFETIME)) {
                        entitlementManager.setLifetimeUnlocked(true);
                    }
                    if (purchase.getProducts().contains(SKU_WEEKLY_PASS)
                            || purchase.getProducts().contains(SKU_ANNUAL_PASS)) {
                        foundActiveSub = true;
                    }
                }
            }
        }

        boolean nextSub = resolveNextSubscriptionState(
                entitlementManager.isSubscriptionActive(),
                foundActiveSub,
                isSubscriptionQuery
        );
        if (nextSub != entitlementManager.isSubscriptionActive()) {
            entitlementManager.setSubscriptionActive(nextSub);
        }
        return anyPurchased;
    }

    private void acknowledgeIfNeeded(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.acknowledgePurchase(params, result -> {});
        }
    }

    public void endConnection() {
        if (billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}
