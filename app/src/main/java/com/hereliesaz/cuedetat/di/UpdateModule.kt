package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.update.AppUpdater
import com.hereliesaz.cuedetat.update.GithubAppUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the source-aware updater for both Google Play and GitHub installs. */
@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {
    @Binds
    @Singleton
    abstract fun bindAppUpdater(impl: GithubAppUpdater): AppUpdater
}
