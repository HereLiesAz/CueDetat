package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.data.MetaWearableRepository
import com.hereliesaz.cuedetat.data.NoOpMetaWearableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** FOSS builds ship no Meta Wearables SDK; bind the no-op. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FossWearableModule {

    @Binds
    @Singleton
    abstract fun bindMetaWearableRepository(
        impl: NoOpMetaWearableRepository,
    ): MetaWearableRepository
}
