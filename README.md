# Velvet Date Night — Android Native (Java 17) v1.1.1 Final Release

100% offline-first, ad-free, consent-first couples tabletop suite crafted for Google Play (Android 8.0 – Android 15+).

## v1.1.1 Final QA & Release Fixes
- **Billing Fix**: Subscription purchases (`velvet_weekly_pass` / `velvet_annual_pass`) now activate VIP entitlements immediately on `onPurchasesUpdated()` without requiring an app restart.
- **Zero-Loss Couple Board**: Backed `CoupleBoardFragment` with `CoupleBoardViewModel` and non-destructive live theme cycling so switching themes mid-match never resets board progress or custom prizes.
- **Android 15 Gesture Bar Insets**: Dynamically shifts bottom system-bar insets onto `nav_host_fragment` whenever sub-games hide `BottomNavigationView`.
- **Curated Deck Recycling & Full VIP Tier Enforcement**: Fixed `TaskEngine` to recycle handcrafted cards before procedural fallback; unified VIP `maxTier` gating across Desire Match, Lucky Wheel (`✦ Spark`), and Draw & Guess penalties.
- **Lifecycle-Safe Dialogs & Polish**: Migrated `PassPhoneDialog` and `PaywallDialogFragment` to AndroidX Fragment Result API (zero `Runnable` fields), fixed Favorites empty state & skip favorite reset, localized all game mode stats / pronouns / countdowns / default prizes, added active toggle highlights across all modes, and isolated `[DEBUG]` preview strings strictly into `src/debug/res/`.
