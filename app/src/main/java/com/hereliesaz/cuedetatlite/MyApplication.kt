package com.hereliesaz.cuedetatlite

import android.app.Application
// REMOVED: import com.hereliesaz.cuedetatlite.data.GithubRepository
import com.hereliesaz.cuedetatlite.data.SensorRepository
// REMOVED: import com.hereliesaz.cuedetatlite.data.UpdateChecker
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.domain.WarningManager
// REMOVED: import com.hereliesaz.cuedetatlite.network.GithubApi
import dagger.hilt.android.HiltAndroidApp
// REMOVED: import retrofit2.Retrofit
// REMOVED: import retrofit2.converter.gson.GsonConverterFactory

@HiltAndroidApp
class MyApplication : Application() {
    // --- Manual DI Setup ---
    // This setup is for components not managed by Hilt.

    // REMOVED: Retrofit, GithubApi, GithubRepository, and UpdateChecker lazy initializations.

    // Make dependencies public for the ViewModel Factory
    val sensorRepository: SensorRepository by lazy {
        SensorRepository(this)
    }

    // Domain Logic
    private val warningManager: WarningManager by lazy {
        WarningManager()
    }
    val stateReducer: StateReducer by lazy {
        StateReducer(warningManager)
    }
    val updateStateUseCase: UpdateStateUseCase by lazy {
        UpdateStateUseCase(warningManager)
    }
}