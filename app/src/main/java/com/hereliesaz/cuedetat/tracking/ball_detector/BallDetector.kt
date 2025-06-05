// app/src/main/java/com/hereliesaz/cuedetat/tracking/ball_detector/BallDetector.kt
package com.hereliesaz.cuedetat.tracking.ball_detector

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.max
import kotlin.math.min

class BallDetector {

    private val TAG = "MLKitBallDetector"
    private var objectDetector: ObjectDetector

    init {
        // Configure ML Kit Object Detector
        // MULTIPLE_OBJECTS_MODE: Detects multiple objects in an image.
        // STREAM_MODE: Optimized for real-time video streams.
        // enableClassification: Classify objects (optional, could help differentiate balls later).
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification() // Enable classification to try and identify types of objects
            .build()

        objectDetector = ObjectDetection.getClient(options)
    }

    /**
     * Processes an ImageProxy frame from CameraX to detect balls using ML Kit.
     * @param imageProxy The ImageProxy from CameraX.
     * @param onDetectionSuccess Callback with a list of detected Balls (ML Kit specific data).
     * @param onDetectionFailure Callback for when detection fails.
     */
    fun detectBalls(imageProxy: ImageProxy, onDetectionSuccess: (List<Ball>) -> Unit, onDetectionFailure: (Exception) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Create InputImage from ImageProxy for ML Kit
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val detectedBalls = mutableListOf<Ball>()
                for (detectedObject in detectedObjects) {
                    val boundingBox = detectedObject.boundingBox
                    val trackingId = detectedObject.trackingId
                    val confidence = detectedObject.labels.firstOrNull()?.confidence ?: 0f

                    // Filter based on shape (aspect ratio close to 1:1) and confidence if needed
                    // A simple heuristic for a ball is a bounding box that is roughly square.
                    val aspectRatio = boundingBox.width().toFloat() / boundingBox.height().toFloat()
                    val isRoughlySquare = aspectRatio > 0.7 && aspectRatio < 1.3 // Allow some leeway

                    // You might want to filter by classification label if available and relevant (e.g., "ball")
                    // For general objects, classification might not be specific enough for pool balls.
                    val isBall = detectedObject.labels.any { label ->
                        label.text.equals("ball", ignoreCase = true) || // Common label for round objects
                                label.text.equals("sphere", ignoreCase = true) ||
                                label.text.equals("sport", ignoreCase = true) // General sports object
                        // Add more specific labels if ML Kit provides them for pool balls
                    }

                    if (isRoughlySquare && confidence > 0.5) { // Confidence threshold
                        // Map ML Kit bounding box to our simplified Ball model
                        val centerX = boundingBox.centerX().toFloat()
                        val centerY = boundingBox.centerY().toFloat()
                        // Use average of half width and half height as radius for a circular representation
                        val radius = (boundingBox.width() + boundingBox.height()) / 4f

                        // Use trackingId for unique identification if available, otherwise fallback to object hash
                        val id = trackingId?.toString() ?: detectedObject.hashCode().toString()
                        detectedBalls.add(Ball(id, centerX, centerY, radius))
                    }
                }
                onDetectionSuccess(detectedBalls)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ML Kit Object detection failed", e)
                onDetectionFailure(e)
            }
            .addOnCompleteListener {
                // Ensure ImageProxy is closed after processing, regardless of success or failure.
                imageProxy.close()
            }
    }

    /**
     * Releases ML Kit detector resources.
     */
    fun shutdown() {
        objectDetector.close()
    }
}