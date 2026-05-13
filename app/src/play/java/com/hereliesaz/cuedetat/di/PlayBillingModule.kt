// FILE: app/src/play/java/com/hereliesaz/cuedetat/di/PlayBillingModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.billing.EntitlementRepository
import com.hereliesaz.cuedetat.billing.PlayBillingEntitlementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt binding for the Play flavor. Resolves EntitlementRepository to the
 * Play Billing-backed implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayBillingModule {

    @Binds
    @Singleton
    abstract fun bindEntitlementRepository(
        impl: PlayBillingEntitlementRepository
    ): EntitlementRepository
}
