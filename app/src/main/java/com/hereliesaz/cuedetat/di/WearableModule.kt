package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.data.MetaWearableRepository
import com.hereliesaz.cuedetat.data.MetaWearableRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the real Meta Wearables implementation for the single app variant. */
@Module
@InstallIn(SingletonComponent::class)
abstract class WearableModule {

    @Binds
    @Singleton
    abstract fun bindMetaWearableRepository(
        impl: MetaWearableRepositoryImpl,
    ): MetaWearableRepository
}
