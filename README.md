# Velvet Date Night — Android Native (Java 17) v2.1.0 Release

100% private, ad-free, consent-first 20-mode couples tabletop suite for Google Play (Android 8.0 – Android 15+).

> **Current release:** v2.1.0 (`versionCode 210` / `versionName "2.1.0"` in `app/build.gradle`). The section below is a historical record of the earlier v1.6.1 QA package and is retained for reference.

## v1.6.1 Final QA Audit & Release Package (historical)
- **Complete Gradle & ProGuard Build Configuration**: Verified `settings.gradle`, root `build.gradle`, `app/build.gradle` (at the time: `versionCode 10` / `versionName "1.6.1"`, 8 `resourceConfigurations`), `gradle.properties`, and `app/proguard-rules.pro`.
- **Lifecycle & Edge-Case Hardening**:
  - `TaskPromptDialogHelper`: Added `isAdded()` + `dialog.isShowing()` guards inside async favorite/skip callbacks and flip animations, plus empty-pool toast handling.
  - `VelvetBombFragment`: Pauses hidden fuse ticking cleanly in `onPause()` and resumes in `onResume()` to avoid background detonation.
  - `DrawingCanvasView` & `ColorPickerView`: Added single-tap dot rendering and null-safe `getParent()` checks.
  - `DeckFragment`: Custom cards save with the active heat tier so they immediately appear in tonight's deck.
  - `ScratchAdventureFragment`, `PropLabFragment`, `CoupleBoardFragment`, `HeartbeatHoldFragment`, `DesireLadderFragment`, `DesireAuctionFragment`, `VelvetBlackjackFragment`, `DrawGuessFragment`: Graceful empty-pool feedback across all 20 modes.
  - `FateCubesFragment`: Synchronizes saved favorite heart state asynchronously whenever a combo lands.
- **Consumer-Grade Copy Polish Across All 8 Languages**: Removed remaining technical/mechanical wording across `en`, `zh-rCN`, `zh-rTW`, `ja`, `ko`, `es`, `fr`, `pt-rBR`.
