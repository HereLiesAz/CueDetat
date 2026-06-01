// FILE: app/src/play/java/com/hereliesaz/cuedetat/di/PlayUpdateModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.update.AppUpdater
import com.hereliesaz.cuedetat.update.PlayAppUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the no-op updater for Play builds (store-managed updates). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayUpdateModule {

    @Binds
    @Singleton
    abstract fun bindAppUpdater(impl: PlayAppUpdater): AppUpdater
}
