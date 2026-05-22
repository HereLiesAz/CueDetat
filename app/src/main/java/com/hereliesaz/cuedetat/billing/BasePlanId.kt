// FILE: app/src/main/java/com/hereliesaz/cuedetat/billing/BasePlanId.kt

package com.hereliesaz.cuedetat.billing

/**
 * The two base plans configured under the `expert_mode` subscription product
 * in Google Play Console. These tag values must match the Play Console
 * configuration exactly.
 */
enum class BasePlanId(val tag: String) {
    MONTHLY("monthly"),
    YEARLY("yearly")
}
