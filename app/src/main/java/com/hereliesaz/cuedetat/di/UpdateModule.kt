// FILE: app/src/main/java/com/hereliesaz/cuedetat/di/UpdateModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.update.AppUpdater
import com.hereliesaz.cuedetat.update.StoreManagedAppUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the non-self-installing updater for the single app variant. */
@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {

    @Binds
    @Singleton
    abstract fun bindAppUpdater(impl: StoreManagedAppUpdater): AppUpdater
}
