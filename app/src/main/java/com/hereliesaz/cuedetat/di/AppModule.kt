// FILE: app/src/main/java/com/hereliesaz/cuedetat/di/AppModule.kt

package com.hereliesaz.cuedetat.di

import android.content.Context
import com.google.gson.Gson
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.hereliesaz.cuedetat.data.ShakeDetector
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.network.GithubApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * The main Dependency Injection module for Hilt.
 *
 * This object defines how to create the singleton instances of core application services.
 * Hilt uses this to generate the dependency graph.
 *
 * Scope: [SingletonComponent] -> These objects live as long as the Application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the Retrofit interface for GitHub API interactions.
     * Used for checking updates and submitting automated issue reports.
     */
    @Provides
    @Singleton
    fun provideGithubApi(): GithubApi {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApi::class.java)
    }

    /**
     * Provides the repository for reading/writing local user preferences (DataStore).
     * Requires Gson for serializing complex objects (like custom color profiles).
     */
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context, gson)
    }

    /**
     * Provides the Google ML Kit Object Detector.
     *
     * Configuration:
     * - STREAM_MODE: Optimized for video (tracks objects across frames).
     * - MULTIPLE_OBJECTS: We need to find Cue Ball, Object Balls, and Obstacles.
     * - CLASSIFICATION: Enabled to distinguish broad categories (though we mostly rely on custom CV).
     */
    @Provides
    @Singleton
    @GenericDetector // Custom Qualifier to distinguish from potential other detectors.
    fun provideGenericObjectDetector(): ObjectDetector {
        return try {
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
            ObjectDetection.getClient(options)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize ML Kit Object Detector.", e)
        }
    }

    /**
     * Provides the accelerometer listener for detecting shakes.
     * Used to trigger calibration resets or "Undo" actions.
     */
    @Provides
    @Singleton
    fun provideShakeDetector(@ApplicationContext context: Context): ShakeDetector {
        return ShakeDetector(context)
    }

    /**
     * Provides a shared Gson instance.
     * Used for JSON serialization in Repositories and Network calls.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
