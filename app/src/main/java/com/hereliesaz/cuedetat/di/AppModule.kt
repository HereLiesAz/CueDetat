package com.hereliesaz.cuedetat.di

import android.content.Context
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.network.GithubApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGithubApi(): GithubApi {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideObjectDetector(@ApplicationContext context: Context): ObjectDetector {
        // Configure the TFLite ObjectDetector
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(15) // Max balls on a table
            .setScoreThreshold(0.5f) // Confidence threshold
            .build()
        // Create the detector from the model file in the assets folder
        return ObjectDetector.createFromFileAndOptions(context, "model.tflite", options)
    }
}