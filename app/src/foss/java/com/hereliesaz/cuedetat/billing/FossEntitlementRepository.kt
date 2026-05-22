// FILE: app/src/foss/java/com/hereliesaz/cuedetat/billing/FossEntitlementRepository.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS-flavor implementation. The user is permanently entitled. Billing-related
 * methods are no-ops; product details emit a sentinel "not applicable" state.
 *
 * This file is only compiled into the foss APK. The play APK uses
 * PlayBillingEntitlementRepository instead.
 */
@Singleton
class FossEntitlementRepository @Inject constructor() : EntitlementRepository {

    private val _entitlement = MutableStateFlow(
        Entitlement(
            active = true,
            source = EntitlementSource.FOSS_BUILD,
            expiresAtMillis = null,
            productId = null,
            lastVerifiedAtMillis = System.currentTimeMillis(),
            isDeviceGenuine = true
        )
    )
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    override suspend fun launchPurchase(activity: Activity, basePlan: BasePlanId) {
        // No-op. The FOSS APK has no billing UI.
    }

    override suspend fun refresh() {
        // No-op.
    }

    override fun productDetails(): Flow<ProductDetailsState> =
        flowOf(ProductDetailsState.NotApplicable)
}
