// FILE: app/src/main/java/com/hereliesaz/cuedetat/di/ArFeatureModule.kt

package com.hereliesaz.cuedetat.di

import com.hereliesaz.cuedetat.arfeature.ArController
import com.hereliesaz.cuedetat.arfeature.ArControllerFacade
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the base [ArController] to the [ArControllerFacade], which constructs the real
 * implementation (`ArControllerImpl`, compiled into the app from feature_expert_ar's source
 * folder) once a user enters Expert AR (see ArControllerFacade).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ArFeatureModule {

    @Binds
    @Singleton
    abstract fun bindArController(impl: ArControllerFacade): ArController
}
