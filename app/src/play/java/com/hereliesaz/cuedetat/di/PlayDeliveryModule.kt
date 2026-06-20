// FILE: app/src/play/java/com/hereliesaz/cuedetat/di/PlayDeliveryModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.delivery.ArFeatureDelivery
import com.hereliesaz.cuedetat.delivery.ModelDelivery
import com.hereliesaz.cuedetat.delivery.PlayArFeatureDelivery
import com.hereliesaz.cuedetat.delivery.PlayModelDelivery
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the on-demand Play Feature Delivery deliveries (model + Expert-AR) for Play builds. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayDeliveryModule {

    @Binds
    @Singleton
    abstract fun bindModelDelivery(impl: PlayModelDelivery): ModelDelivery

    @Binds
    @Singleton
    abstract fun bindArFeatureDelivery(impl: PlayArFeatureDelivery): ArFeatureDelivery
}
