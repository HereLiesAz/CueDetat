package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.delivery.ArFeatureDelivery
import com.hereliesaz.cuedetat.delivery.BundledArFeatureDelivery
import com.hereliesaz.cuedetat.delivery.BundledModelDelivery
import com.hereliesaz.cuedetat.delivery.ModelDelivery
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the no-op deliveries: the model and the AR code are bundled in the app. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DeliveryModule {
    @Binds
    @Singleton
    abstract fun bindModelDelivery(impl: BundledModelDelivery): ModelDelivery

    @Binds
    @Singleton
    abstract fun bindArFeatureDelivery(impl: BundledArFeatureDelivery): ArFeatureDelivery
}
