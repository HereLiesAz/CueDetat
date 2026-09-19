// FILE: app/src/main/java/com/hereliesaz/cuedetat/di/ArFeatureModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.arfeature.ArController
import com.hereliesaz.cuedetat.arfeature.NoOpArController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Expert AR is intentionally disabled in the shipped app. Keep the boundary so
 * dormant AR work can return later without exposing a half-working control.
 */
@Module
@InstallIn(SingletonComponent::class)
object ArFeatureModule {
    @Provides
    @Singleton
    fun provideArController(): ArController = NoOpArController
}
