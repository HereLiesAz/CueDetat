# Design: Subscription → One-Time Purchase

Date: 2026-05-31

## Context

Expert Mode was sold as an auto-renewing subscription (`expert_mode`, monthly/yearly
base plans, 3-day free trial). We are switching to a single one-time purchase. No one ever
bought the subscription, so there are no existing entitlements to migrate or honor.

Google Play does not allow changing an existing product's type, and reusing a former
subscription ID as an in-app product is unreliable. We therefore use a **new** non-consumable
in-app product ID: **`expert_mode_unlock`**. Price is configured in Play Console only (never
hardcoded), as before.

## Product model

- One **non-consumable** in-app product, ID `expert_mode_unlock`.
- No base plans, no free trial, no auto-renew.
- Once `PURCHASED` and acknowledged, Expert Mode is owned **permanently** — never revoked,
  even fully offline. (The old 14-day offline cap, sized for subscriptions, is removed.)

## Changes by layer

### Billing wrapper (`app/src/play/.../BillingClientWrapper.kt`)
- `queryActiveSubscriptions()` → `queryOwnedPurchases()` using `ProductType.INAPP`.
- `queryExpertProductDetails()` switches to `ProductType.INAPP`.
- `launchBillingFlow(activity, productDetails)` drops the `offerToken` parameter — one-time
  products build `ProductDetailsParams` without an offer token.
- `enablePendingPurchases(... enableOneTimeProducts())` already present; unchanged.

### Repository (`app/src/play/.../PlayBillingEntitlementRepository.kt`)
- `launchPurchase(activity)` (no `BasePlanId`): reads `oneTimePurchaseOfferDetails`, launches
  the flow without an offer token.
- `refresh()` calls `queryOwnedPurchases()`.
- `productDetails()` emits a single formatted price from `oneTimePurchaseOfferDetails`
  (`findFormattedPrice`/`findTrialDays` over `subscriptionOfferDetails` are removed).
- `applyOfflineCap` no longer revokes; owned entitlement stays active indefinitely. The
  14-day constant and `OfflineCapTest` are removed.
- `PurchaseToEntitlementMapper` is kept as-is (still maps `PURCHASED` → active,
  `expiresAtMillis = null`); `isAutoRenewing` is now irrelevant but harmless.

### Interface + FOSS (`app/src/main`, `app/src/foss`)
- `EntitlementRepository.launchPurchase(activity)` loses the `BasePlanId` param.
- `ProductDetailsState.Loaded(monthly, yearly, trialDays)` → `Loaded(formattedPrice: String)`.
- `BasePlanId.kt` deleted.
- `FossEntitlementRepository.launchPurchase(activity)` updated; `productDetails()` stays
  `NotApplicable`.

### Paywall UI (`app/src/main/.../paywall/`)
- `PaywallViewModel.purchase(activity)` (no plan param).
- `PaywallSheet` replaces the two `PlanCard`s + trial copy with a single price and an
  **"Unlock Expert Mode — <price>"** button; footer copy becomes "One-time purchase. No
  subscription." Tester-license section, Restore Purchases, and diagnostics unchanged.

### Product ID constant
- `BillingProductIds.PRODUCT_ID_EXPERT = "expert_mode_unlock"`; comment updated
  (subscription → in-app product).

## Tests
- Remove `OfflineCapTest` (no cap).
- Update paywall tests and any test referencing `BasePlanId` / `Loaded(monthly,…)`.
- `PurchaseToEntitlementMapperTest` largely unchanged (PURCHASED → active).

## Out of scope
- No data migration (no existing subscribers).
- Play Console product creation and pricing are operational steps done outside the code.

## Verification
- `bash ./gradlew :app:assembleFossDebug :app:compilePlayDebugKotlin`.
- `bash ./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest`.
- Manual: paywall shows a single price + one-time button; purchase unlocks Expert and it
  persists across relaunch/offline.
