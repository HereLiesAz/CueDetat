// FILE: app/src/foss/java/com/hereliesaz/cuedetat/di/FossDeliveryModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.delivery.ArFeatureDelivery
import com.hereliesaz.cuedetat.delivery.FossArFeatureDelivery
import com.hereliesaz.cuedetat.delivery.FossModelDelivery
import com.hereliesaz.cuedetat.delivery.ModelDelivery
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the no-op deliveries for FOSS builds (model + AR code bundled in the APK). */
@Module
@InstallIn(SingletonComponent::class)
abstract class FossDeliveryModule {

    @Binds
    @Singleton
    abstract fun bindModelDelivery(impl: FossModelDelivery): ModelDelivery

    @Binds
    @Singleton
    abstract fun bindArFeatureDelivery(impl: FossArFeatureDelivery): ArFeatureDelivery
}
