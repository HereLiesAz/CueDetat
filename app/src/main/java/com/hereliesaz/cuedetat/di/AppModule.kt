package com.hereliesaz.cuedetat.di

import android.content.Context
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.common.model.LocalModel
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.network.GithubApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
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
    @GenericDetector
    fun provideGenericObjectDetector(): ObjectDetector {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        return ObjectDetection.getClient(options)
    }

    @Provides
    @Singleton
    @CustomDetector
    fun provideCustomObjectDetector(@ApplicationContext context: Context): ObjectDetector? {
        val modelFile = "model.tflite"
        val assetExists = try {
            context.assets.open(modelFile).close(); true
        } catch (e: IOException) {
            false
        }

        return if (assetExists) {
            val localModel = LocalModel.Builder().setAssetFilePath(modelFile).build()
            val customOptions = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build()
            ObjectDetection.getClient(customOptions)
        } else {
            null // Return null if the custom model doesn't exist
        }
    }
}