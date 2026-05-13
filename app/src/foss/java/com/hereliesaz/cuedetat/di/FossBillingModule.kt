// FILE: app/src/foss/java/com/hereliesaz/cuedetat/di/FossBillingModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.billing.EntitlementRepository
import com.hereliesaz.cuedetat.billing.FossEntitlementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt binding for the FOSS flavor. Resolves EntitlementRepository to the
 * always-active stub.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FossBillingModule {

    @Binds
    @Singleton
    abstract fun bindEntitlementRepository(
        impl: FossEntitlementRepository
    ): EntitlementRepository
}
