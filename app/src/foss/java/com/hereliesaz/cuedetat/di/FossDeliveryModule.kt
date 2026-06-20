// FILE: app/src/foss/java/com/hereliesaz/cuedetat/di/FossDeliveryModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.delivery.FossModelDelivery
import com.hereliesaz.cuedetat.delivery.ModelDelivery
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the no-op model delivery for FOSS builds (model bundled in the APK). */
@Module
@InstallIn(SingletonComponent::class)
abstract class FossDeliveryModule {

    @Binds
    @Singleton
    abstract fun bindModelDelivery(impl: FossModelDelivery): ModelDelivery
}
