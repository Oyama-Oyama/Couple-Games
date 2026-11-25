# Velvet Date Night — Google Play Launch, ASO & Compliance Checklist (v1.1.0 Polished)

## 1. Billing SKU Configuration (Play Console -> Monetize)
Configure these exact Product IDs matching `PlayBillingBridge.java` & `PricingCatalog.java` (proper price ladder):
- **Subscription**: `velvet_weekly_pass` (Base plan: 1 Week, Suggested: **$4.99 / wk**)
- **Subscription**: `velvet_annual_pass` (Base plan: 1 Year, Suggested: **$19.99 / yr** — Save 92%)
- **One-Time Product (In-app product)**: `velvet_lifetime_unlock` (Suggested: **$29.99 USD** — Pay Once, Own Forever)
- Remember to enroll in Google Play's **15% Service Fee** program before first payout.

## 2. Store Listing ASO Copy (Compliant — Zero Banned Emojis / Explicit Terms)

### English (Primary — `en-US`)
- **App Name (30 chars max)**: `Velvet: Couples Board Game`
- **Short Description (80 chars max)**: `Offline 2-player date night board game, desire match & spin wheel. No ads.`

### 简体中文 (`zh-CN`)
- **应用名称**: `丝绒约会夜：情侣桌游与飞行棋`
- **简短说明**: `纯离线无广告双人情侣桌游：飞行棋、盲选配对、转盘、扫雷与你画我猜。`

### 日本語 (`ja-JP`)
- **アプリ名**: `ベルベット：カップルすごろく＆恋愛ゲーム`
- **簡単な説明**: `完全オフライン・広告なしの2人用テーブルゲーム。すごろく、盲選マッチ、ルーレット収録。`

## 3. Verified Production Readiness (Phase 5 Polish)
- **Android 16 (targetSdk 36) Edge-to-Edge**: Implemented with `EdgeToEdge.enable()` and `WindowInsetsCompat.Type.systemBars()` listener.
- **Adaptive Launcher Icon**: Velvet crimson gradient + champagne gold heart emblem bundled in `mipmap-anydpi-v26`.
- **Consent-First & Inclusive UX**: First-launch couple profile onboarding (`He / She / They` pronouns + custom Safe Word), Private Pass boundary mode (hides individual vetoes), immersive sub-game Pause/Safe Word bar.
- **Data Safety Form**: Zero ads, zero analytics SDKs, zero backend cloud sync; optional `FLAG_SECURE` screenshot shield.
