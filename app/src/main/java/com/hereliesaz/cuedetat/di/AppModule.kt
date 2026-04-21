package com.hereliesaz.cuedetat.di

import android.content.Context
import android.graphics.PointF
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.hereliesaz.cuedetat.data.MergedTFLiteDetector
import com.hereliesaz.cuedetat.network.GithubApi
import com.hereliesaz.cuedetat.network.MyriadApi
import com.hereliesaz.cuedetat.ui.composables.tablescan.PocketDetector
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
     * Provides the Retrofit interface for the local MYRIAD Python Backend.
     * Hardcoded to localhost for now assuming emulator execution (10.0.2.2).
     */
    @Provides
    @Singleton
    fun provideMyriadApi(gson: Gson): MyriadApi {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MyriadApi::class.java)
    }

    /**
     * Provides the Google ML Kit Object Detector.
     */
    @Provides
    @Singleton
    @GenericDetector
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
     * Provides a shared Merged TFLite Detector instance.
     * This combines pocket and pool detection with shared preprocessing.
     */
    @Provides
    @Singleton
    fun provideMergedTFLiteDetector(@ApplicationContext context: Context): MergedTFLiteDetector {
        return MergedTFLiteDetector(context)
    }

    /**
     * Provides the singular, surviving TFLite-backed pocket detector.
     * The composite illusion has been eradicated.
     */
    @Provides
    @Singleton
    fun providePocketDetector(
        mergedDetector: MergedTFLiteDetector
    ): PocketDetector {
        return mergedDetector
    }

    /**
     * Provides a shared Gson instance.
     * Used for JSON serialization in Repositories and Network calls.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(PointF::class.java, object : TypeAdapter<PointF>() {
                override fun write(out: JsonWriter, value: PointF?) {
                    if (value == null) { out.nullValue(); return }
                    out.beginObject()
                    out.name("x").value(value.x)
                    out.name("y").value(value.y)
                    out.endObject()
                }
                override fun read(input: JsonReader): PointF? {
                    if (input.peek() == JsonToken.NULL) { input.nextNull(); return null }
                    var x = 0f; var y = 0f
                    input.beginObject()
                    while (input.hasNext()) {
                        when (input.nextName()) {
                            "x" -> x = input.nextDouble().toFloat()
                            "y" -> y = input.nextDouble().toFloat()
                            else -> input.skipValue()
                        }
                    }
                    input.endObject()
                    return PointF(x, y)
                }
            })
            .create()
    }
}