// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/BillingProductIds.kt

package com.hereliesaz.cuedetat.billing

/**
 * Constants matching the Google Play Console configuration. If you change
 * these, you MUST also update the corresponding in-app product in the
 * Play Console — they are the join key.
 *
 * Expert Mode is a single non-consumable in-app purchase (not a subscription).
 */
object BillingProductIds {
    const val PRODUCT_ID_EXPERT = "expert_mode_unlock"
}
