// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/MyApplication.kt
package com.hereliesaz.cuedetatlite

import android.app.Application
import com.hereliesaz.cuedetatlite.data.GithubRepository
import com.hereliesaz.cuedetatlite.data.SensorRepository
import com.hereliesaz.cuedetatlite.data.UpdateChecker
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.domain.WarningManager
import com.hereliesaz.cuedetatlite.network.GithubApi
import dagger.hilt.android.HiltAndroidApp
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@HiltAndroidApp
class MyApplication : Application() {
    // --- Manual DI Setup ---
    // This setup is for components not managed by Hilt.

    // Retrofit for network requests
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API service
    private val githubApi: GithubApi by lazy {
        retrofit.create(GithubApi::class.java)
    }

    // Repositories
    private val githubRepository: GithubRepository by lazy {
        GithubRepository(githubApi)
    }

    // Make dependencies public for the ViewModel Factory
    val sensorRepository: SensorRepository by lazy {
        SensorRepository(this)
    }
    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(githubRepository)
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
