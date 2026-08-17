package com.hereliesaz.cuedetat.data

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MergedTFLiteDetectorTest {

    @Test
    fun modelLoadsAndInterpretersAreInitialised() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val detector = MergedTFLiteDetector(context)
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val result = detector.detect(bitmap)
        assertNotNull("detect() returned null — interpreter failed to load", result)
    }

    @Test
    fun modelFileExistsInAssets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val files = context.assets.list("ml") ?: emptyArray()
        assertTrue("MASTER_POOL_MODEL.tflite missing from assets/ml/",
            files.contains("MASTER_POOL_MODEL.tflite"))
    }

    /**
     * detectPool() (segment 2 / HEAD_POOL_PIVOT) was previously misinterpreted
     * downstream (in VisionRepository) as a ball/cue detector. It is not: per
     * PoolDetection.kt's KDoc, ml/metadata.yaml, and
     * ml/cuedetat_pocket_detector_kaggle.py (which always skips the ball/cue
     * source datasets for having no class-name overlap with the trained
     * MASTER_CLASSES), this model was only ever trained on the 3-class
     * table/hole/side scheme. This test doesn't have a populated test table in
     * front of the emulator camera, so it can't assert real detections — it
     * instead pins down the structural contract: the call completes without
     * throwing on a blank frame, and whatever classIds it does emit fall inside
     * the only range this model was ever trained on (0..2), guarding against a
     * silent regression back to treating out-of-range/ball-cue-style ids as
     * valid.
     */
    @Test
    fun detectPoolReturnsWellFormedTableHoleSideDetections() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val detector = MergedTFLiteDetector(context)
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)

        val detections = detector.detectPool(bitmap)

        assertNotNull("detectPool() must return a (possibly empty) list, never null", detections)
        detections.forEach { detection ->
            assertTrue(
                "classId ${detection.classId} is outside the trained table/hole/side " +
                    "range 0..2 — see PoolDetection.kt KDoc",
                detection.classId in 0..2
            )
            assertTrue(
                "confidence ${detection.confidence} must be in [0,1]",
                detection.confidence in 0f..1f
            )
        }
    }
}
