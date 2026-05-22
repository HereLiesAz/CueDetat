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
}
