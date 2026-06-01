// FILE: app/src/foss/java/com/hereliesaz/cuedetat/di/FossUpdateModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.update.AppUpdater
import com.hereliesaz.cuedetat.update.FossAppUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the GitHub-releases self-updater for FOSS builds. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FossUpdateModule {

    @Binds
    @Singleton
    abstract fun bindAppUpdater(impl: FossAppUpdater): AppUpdater
}
