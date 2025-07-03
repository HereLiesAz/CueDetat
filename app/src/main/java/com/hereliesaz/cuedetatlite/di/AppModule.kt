package com.hereliesaz.cuedetatlite.di

import android.content.Context
import com.hereliesaz.cuedetatlite.data.SensorRepository
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.domain.WarningManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSensorRepository(@ApplicationContext context: Context): SensorRepository {
        return SensorRepository(context)
    }

    @Provides
    @Singleton
    fun provideWarningManager(): WarningManager {
        return WarningManager()
    }

    @Provides
    @Singleton
    fun provideStateReducer(warningManager: WarningManager): StateReducer {
        return StateReducer(warningManager)
    }

    @Provides
    @Singleton
    fun provideUpdateStateUseCase(warningManager: WarningManager): UpdateStateUseCase {
        return UpdateStateUseCase(warningManager)
    }
}
