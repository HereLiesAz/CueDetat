# Design Spec: Expert Mode Google Play Subscription

**Date:** 2026-05-07
**Status:** Approved
**Target version:** TBD (next release after closed testing)

---

## Overview

Gate `ExperienceMode.EXPERT` behind a Google Play subscription on the Play Store distribution. Ship two product flavors from one codebase: a `play` flavor that includes Play Billing Library and enforces the paywall, and a `foss` flavor (built by GitHub Actions) that has no billing dependency and leaves Expert Mode unlocked. Verification is client-only — tampering is not a concern; users who want Expert Mode for free can build the `foss` APK from source.

The entitlement is exposed to the rest of the app as a single `Boolean` (`isExpertEntitled`) on `OverlayState`, sourced from a flavor-specific `EntitlementRepository`. Reducers stay pure and unaware of the flavor split.

---

## Scope

| Area | Change |
|------|--------|
| `app/build.gradle.kts` | **Modified.** Add `play` and `foss` product flavors; add `playImplementation` for Play Billing Library 7.x. |
| `gradle/libs.versions.toml` | **Modified.** Add `androidx-billing-ktx`. |
| `billing/Entitlement.kt` | **New** (`main/`). Shared model. |
| `billing/EntitlementRepository.kt` | **New** (`main/`). Interface only. |
| `billing/BasePlanId.kt` | **New** (`main/`). Enum: `MONTHLY`, `YEARLY`. |
| `billing/PaywallTrigger.kt` | **New** (`main/`). Enum for analytics tagging. |
| `billing/PlayBillingEntitlementRepository.kt` | **New** (`play/`). Wraps `BillingClient`. |
| `billing/BillingClientWrapper.kt` | **New** (`play/`). Connection management. |
| `billing/EntitlementCacheStore.kt` | **New** (`play/`). DataStore cache. |
| `billing/BillingProductIds.kt` | **New** (`play/`). Product/base-plan constants. |
| `billing/FossEntitlementRepository.kt` | **New** (`foss/`). Always-entitled stub. |
| `di/PlayBillingModule.kt` | **New** (`play/`). |
| `di/FossBillingModule.kt` | **New** (`foss/`). |
| `ui/composables/paywall/PaywallSheet.kt` | **New** (`main/`). Compose modal sheet. |
| `ui/composables/paywall/PaywallViewModel.kt` | **New** (`main/`). |
| `domain/UiModel.kt` | **Modified.** Add `isExpertEntitled: Boolean = false` to `OverlayState`. |
| `domain/reducers/EntitlementReducer.kt` | **New.** Forces BEGINNER when entitlement drops. |
| `domain/reducers/ToggleReducer.kt` | **Modified.** Guard `SetExperienceMode(EXPERT)` on `isExpertEntitled`. |
| `ui/MainViewModel.kt` | **Modified.** Collect `EntitlementRepository.entitlement`, dispatch `EntitlementChanged`. Trigger onboarding paywall once. |
| `ui/composables/AzNavRailMenu.kt` | **Modified.** Show "Get Expert Mode" tile when `!isExpertEntitled` (Play flavor only — naturally hidden in FOSS since flag is always true). |
| `MainActivity.kt` | **Modified.** Call `EntitlementRepository.refresh()` from `onResume`. |
| `.github/workflows/*.yml` | **Modified.** CI builds `assembleFossRelease`. |

---

## 1. Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Verification | Client-only (Play Billing Library) | Tampering not a concern; FOSS APK is the explicit "free" path. |
| Build split | Product flavors `play` + `foss` | Standard pattern; IDE-supported; reproducible. |
| Existing users | Hard cutover | App is in closed testing — no live install base to grandfather. |
| SKU | One product `expert_mode`, base plans `monthly` + `yearly`, 7-day intro free trial | Standard structure; trial reduces friction. |
| Lapse | Mirror Play's `queryPurchasesAsync` result | No explicit grace handling code; Play's response is treated as truth. |
| Paywall surfaces | Onboarding (skippable, once) + on-tap gate + persistent nav rail tile | Triple coverage to maximize discovery. |
| Offline cache window | 14 days | Tolerates travel; bounded against indefinite use after refund. |

---

## 2. Architecture

### 2.1 Module Boundaries

```
app/src/main/                ← shared code, no billing imports
  billing/
    Entitlement.kt           ← data class
    EntitlementRepository.kt ← interface
    BasePlanId.kt
    PaywallTrigger.kt
  ui/composables/paywall/
    PaywallSheet.kt
    PaywallViewModel.kt
  domain/reducers/
    EntitlementReducer.kt    ← new
    ToggleReducer.kt         ← modified

app/src/play/                ← only built into the play APK
  billing/
    PlayBillingEntitlementRepository.kt
    BillingClientWrapper.kt
    EntitlementCacheStore.kt
    BillingProductIds.kt
  di/PlayBillingModule.kt

app/src/foss/                ← only built into the foss APK
  billing/FossEntitlementRepository.kt
  di/FossBillingModule.kt
```

The `com.android.billingclient` import only ever appears in `app/src/play/`.

### 2.2 Build Configuration

`app/build.gradle.kts`:

```kotlin
android {
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            // applicationId stays as-is so Play upgrades work
        }
        create("foss") {
            dimension = "distribution"
            applicationIdSuffix = ".foss"
            versionNameSuffix = "-foss"
        }
    }
}
dependencies {
    "playImplementation"(libs.androidx.billing.ktx)
}
```

`gradle/libs.versions.toml` adds:
```toml
androidx-billing = "7.1.1"  # latest 7.x at time of writing
androidx-billing-ktx = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "androidx-billing" }
```

Local default flavor: `play` (set via Android Studio variant selector + a default in the `defaultConfig` block if needed). CI explicitly builds `foss`.

### 2.3 Models

`Entitlement`:

```kotlin
data class Entitlement(
    val active: Boolean,
    val source: EntitlementSource,
    val expiresAt: Instant?,
    val productId: String?,
    val lastVerifiedAt: Instant?
)

enum class EntitlementSource {
    NONE,             // never entitled
    PLAY_LOCAL,       // Play Billing client confirmed active purchase
    OFFLINE_CACHED,   // operating from cache; refresh failed
    FOSS_BUILD        // FOSS flavor; permanently active
}
```

`Entitlement.NONE` shorthand: `Entitlement(active=false, source=NONE, expiresAt=null, productId=null, lastVerifiedAt=null)`.

`EntitlementRepository`:

```kotlin
interface EntitlementRepository {
    val entitlement: StateFlow<Entitlement>
    suspend fun launchPurchase(activity: Activity, basePlan: BasePlanId)
    suspend fun restorePurchases()  // wraps refresh()
    suspend fun refresh()
    fun queryProductDetails(): Flow<ProductDetailsState>  // for paywall UI
}
```

`ProductDetailsState` is a sealed type: `Loading | Loaded(monthly, yearly) | Error(reason)`. The shared model ensures both flavors compile against the same paywall ViewModel — though the FOSS impl is never wired to a sheet because `ShowPaywall` is never emitted.

### 2.4 MVI Integration

- `OverlayState` gains `val isExpertEntitled: Boolean = false`.
- New event `MainScreenEvent.EntitlementChanged(entitlement: Entitlement)` dispatched by `MainViewModel` whenever the repository emits.
- New event `MainScreenEvent.ShowPaywall(trigger: PaywallTrigger)` dispatched by reducers and consumed as a side effect to display `PaywallSheet`.
- New reducer `EntitlementReducer`:
  - On `EntitlementChanged(e)`: set `isExpertEntitled = e.active`. If `!e.active` AND `state.experienceMode == EXPERT`, also force `experienceMode = BEGINNER`.
- `ToggleReducer.SetExperienceMode(EXPERT)` gains a guard:
  ```
  if (!state.isExpertEntitled) return state.copy(...) + ShowPaywall(EXPERT_TOGGLE_TAP)
  ```
- "Auto-enter Expert after purchase" is owned by `PaywallViewModel`, not by a reducer: when the sheet observes the entitlement flip to active, it dispatches `SetExperienceMode(EXPERT)` then dismisses. This keeps the paywall flow's intent in one place rather than coupling the reducer to a "pending toggle" notion. Onboarding-triggered purchases skip this dispatch (they don't carry an intent to enter Expert immediately — entering Beginner-by-default still applies; user can toggle later).

Reducers do not import any billing code. The flavor split is invisible above the repository interface.

---

## 3. Data Flow

### 3.1 App Launch (Play flavor, returning subscriber)

1. `Application.onCreate` constructs `PlayBillingEntitlementRepository` via Hilt.
2. Repository `init`: read `EntitlementCacheStore`. If cache exists and `lastVerifiedAt` is within 14 days, emit it immediately.
3. `BillingClient.startConnection` (async, with retry).
4. On connected: `queryPurchasesAsync(BillingClient.ProductType.SUBS)`.
5. Map result to `Entitlement`, write to cache, emit on `entitlement` flow.
6. `MainViewModel` collects, dispatches `EntitlementChanged`, reducer updates `OverlayState`.

The cache-then-refresh sequence ensures a paying user never sees a flash of locked Expert Mode at startup.

### 3.2 Onboarding Paywall

1. After `SplashScreen` finishes, `MainViewModel` checks `DataStore.has_seen_onboarding_paywall`.
2. If `false` AND `!isExpertEntitled`, emit `ShowPaywall(ONBOARDING)`.
3. Set `has_seen_onboarding_paywall = true` regardless of user choice.
4. Sheet shows with a `Skip` button in addition to the standard dismiss.

No flavor check is needed: in FOSS, `isExpertEntitled` is `true` from the first repository emission (which lands before splash completes), so the condition naturally fails. The shared code is unaware of which flavor it's running in.

### 3.3 Purchase Flow

1. User taps Expert toggle (or "Get Expert Mode" tile).
2. `ToggleReducer` sees `!isExpertEntitled`, emits `ShowPaywall(EXPERT_TOGGLE_TAP)` (or `NAV_TILE`).
3. `PaywallSheet` opens. `PaywallViewModel` calls `repository.queryProductDetails()` and renders cards.
4. User taps a plan card. `PaywallViewModel` calls `repository.launchPurchase(activity, basePlan)`.
5. `BillingClientWrapper.launchBillingFlow` — Play UI takes over.
6. On success, `PurchasesUpdatedListener` fires; repository acknowledges purchase if needed, calls `refresh()`, emits new `Entitlement(active=true)`.
7. `EntitlementReducer` flips `isExpertEntitled=true` and applies the pending Expert toggle.
8. Sheet observes entitlement change and dismisses.

### 3.4 Restore on Resume

`MainActivity.onResume` calls `repository.refresh()`. This is `queryPurchasesAsync` — Play surfaces all active subs tied to the user's Google account. No separate "Restore" button is required for correctness; one is included anyway because users expect it.

### 3.5 Lapse / Cancel / Refund

1. User cancels in Play, payment fails, or refund processed.
2. Next `onResume` → `refresh()` → `queryPurchasesAsync` returns either nothing or a purchase with state reflecting the change.
3. Repository emits `Entitlement(active=false, source=NONE)`.
4. `EntitlementReducer` flips `isExpertEntitled=false` and forces `experienceMode = BEGINNER`.
5. No popup. Silent downgrade.

### 3.6 Offline Behavior

- Every successful refresh updates `lastVerifiedAt`.
- If refresh fails (no network, Play disconnected), the cached entitlement continues to apply.
- If `lastVerifiedAt` is more than 14 days old AND no successful refresh has occurred, the repository emits `Entitlement.NONE` regardless of cache contents. This caps a paid-then-refunded-then-offline user at 14 days of free Expert Mode.
- While in cache mode, `source = OFFLINE_CACHED`.

### 3.7 FOSS Flavor

```
1. Hilt provides FossEntitlementRepository
2. StateFlow emits Entitlement(active=true, source=FOSS_BUILD, ...) immediately and never changes
3. Reducers see isExpertEntitled=true forever
4. ShowPaywall is never emitted; PaywallSheet never appears; nav tile never renders
5. com.android.billingclient.* is not in the APK
```

---

## 4. Paywall UI

`PaywallSheet` is a Compose `ModalBottomSheet`:

| Section | Content |
|---|---|
| Header | "Unlock Expert Mode" — single-line value prop in project voice (e.g., "Full AR table tracking, ball selection, glasses mode, and the ability to feel marginally less bad about yourself.") |
| Plan cards | Yearly (highlighted "Best Value", "Save ~30%" badge) and Monthly. Each shows live price from `queryProductDetailsAsync` and CTA "Start 7-day free trial". |
| Trial fine print | "7-day free trial, then [price]. Cancel anytime in Google Play." |
| Restore purchases | Secondary text button — calls `repository.restorePurchases()`. |
| Dismiss | "Continue in Beginner Mode" — secondary text button. |

Loading state for plan cards: skeleton placeholder. Error state: tap-to-retry with the BillingClient response code surfaced for diagnostics.

Theming follows existing AzNavRail / Material 3 conventions in the project.

---

## 5. Error Handling

### 5.1 Billing Connection Failures

- `BillingClientWrapper` retries with exponential backoff: 1s, 2s, 4s, 8s, capped at 60s.
- During disconnect, the repository continues emitting cached entitlement.
- A user with no cache and no successful query stays in `Entitlement.NONE`; Expert Mode stays locked. No user-facing error UI for transient disconnects.

### 5.2 Purchase Flow Failures

| BillingResponseCode | Behavior |
|---|---|
| `USER_CANCELED` | Silent. Sheet stays open. |
| `ITEM_UNAVAILABLE` | Show "This subscription isn't available in your region." |
| `SERVICE_DISCONNECTED`, `SERVICE_UNAVAILABLE`, `NETWORK_ERROR` | "Couldn't reach Google Play. Try again." with retry button. |
| `ITEM_ALREADY_OWNED` | Treat as success — call `refresh()`; the cached state should flip to active. |
| `DEVELOPER_ERROR`, `ERROR` | Log, show generic "Something went wrong." with retry. |

### 5.3 Pending Purchases

`Purchase.PurchaseState.PENDING` does not grant entitlement. Show "Payment is being processed." in the sheet. State updates via subsequent `onPurchasesUpdated` callbacks or `onResume` refresh.

### 5.4 Acknowledgment

Every purchase must be acknowledged within 3 days. `PlayBillingEntitlementRepository` calls `acknowledgePurchase` immediately upon observing a `PURCHASED` state on a non-acknowledged purchase. Acknowledgment failures log but do not block UI; Play retries on next query.

---

## 6. Testing Strategy

| Layer | What | How |
|---|---|---|
| Reducers | `EntitlementReducer`, `ToggleReducer` Expert-gate guard | Pure JVM unit tests — feed states + events, assert state changes (matches existing reducer test pattern) |
| `EntitlementRepository` contract | Both flavors satisfy interface: emits `Entitlement`, `launchPurchase` callable, `refresh` idempotent | Shared interface test suite, run against `FossEntitlementRepository` directly and against `PlayBillingEntitlementRepository` with a fake `BillingClientWrapper` |
| Purchase-state mapping | `PlayBillingEntitlementRepository` maps `Purchase` to `Entitlement` correctly across pending, acknowledged, expired, etc. | Unit tests with hand-built/mocked `Purchase` fixtures |
| `PaywallViewModel` | Loads product details; dispatches purchase; surfaces error codes | Unit tests with fake `EntitlementRepository` |
| End-to-end purchase | Closed testing track + license-tester accounts | Manual: monthly purchase, yearly purchase, cancel, refund, switch plan, resubscribe, offline-then-online |
| FOSS flavor | Expert never gated, no billing classes in APK | Unit test asserts `isExpertEntitled == true` from `FossEntitlementRepository`. CI step greps the FOSS APK's dex output (e.g., via `dexdump`) to assert no `com.android.billingclient` classes are present |

---

## 7. Out of Scope

- Server-side verification (no backend). May be revisited if tampering becomes a concern post-launch.
- Real-Time Developer Notifications (RTDN). Not useful without a backend.
- Multi-tier subscriptions (e.g., a separate "Pro" SKU). One product, two base plans.
- Promo codes / introductory offers beyond the standard 7-day trial. Can be added later via Play Console without code changes.
- In-app messaging for upcoming renewals or failed payments. Play handles user-facing notifications; we do not.
- Analytics implementation. `PaywallTrigger` is wired through but no analytics SDK is added in this spec.

---

## 8. Migration Notes

App is in closed testing as of 2026-05-07. There is no production install base to grandfather. The `play` flavor's `applicationId` must remain identical to the current published `applicationId` so closed-testing testers receive an upgrade rather than a side-by-side install.

The `foss` flavor uses `applicationIdSuffix = ".foss"` so a developer can have both APKs installed on the same device.

---

## 9. Open Questions

None at design time. Implementation may surface details around:
- Exact Play Billing Library version (7.x — pin during planning).
- Hilt module wiring patterns specific to the existing DI setup in `di/AppModule.kt`.
- Whether the project uses Hilt or another DI framework — confirm before writing the `*BillingModule` files.