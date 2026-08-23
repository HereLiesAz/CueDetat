package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.data.MetaWearableRepository
import com.hereliesaz.cuedetat.data.PlayMetaWearableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Play builds carry the Meta Wearables SDK; bind the real implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayWearableModule {

    @Binds
    @Singleton
    abstract fun bindMetaWearableRepository(
        impl: PlayMetaWearableRepository,
    ): MetaWearableRepository
}
