with open("app/src/main/java/com/hereliesaz/cuedetat/di/AppModule.kt", "r") as f:
    text = f.read()

import1 = """import com.google.gson.stream.JsonWriter
import com.hereliesaz.cuedetat.data.TFLitePoolDetector"""
import2 = """import com.google.gson.stream.JsonWriter
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.hereliesaz.cuedetat.data.TFLitePoolDetector"""
text = text.replace(import1, import2)

p_old = """    /**
     * Provides the TFLite Pool detector used for tracking balls and cues.
     */
    @Provides
    @Singleton
    fun providePoolDetector(@ApplicationContext context: Context): TFLitePoolDetector {
        return TFLitePoolDetector(context)
    }"""

p_new = """    /**
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
     * Provides the TFLite Pool detector used for tracking balls and cues.
     */
    @Provides
    @Singleton
    fun providePoolDetector(@ApplicationContext context: Context): TFLitePoolDetector {
        return TFLitePoolDetector(context)
    }"""
text = text.replace(p_old, p_new)

with open("app/src/main/java/com/hereliesaz/cuedetat/di/AppModule.kt", "w") as f:
    f.write(text)

