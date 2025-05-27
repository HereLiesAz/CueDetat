// app/src/main/java/com/hereliesaz/cuedetat/tracking/utils/YuvToRgbConverter.kt
package com.hereliesaz.cuedetat.tracking.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.renderscript.*
import java.nio.ByteBuffer

/**
 * Converts a YUV_420_888 Image object to an RGB Bitmap.
 *
 * This utility uses RenderScript for efficient YUV to RGB conversion.
 */
class YuvToRgbConverter(context: Context) {
    private val rs = RenderScript.create(context) // RenderScript context
    private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs)) // RenderScript intrinsic for YUV to RGB conversion
    private var pixelCount: Int = -1 // Stores the total pixel count of the last processed image
    private var inputAllocation: Allocation? = null // RenderScript allocation for input YUV data
    private var outputAllocation: Allocation? = null // RenderScript allocation for output RGB data

    /**
     * Converts an Android `Image` (YUV_420_888 format) to an RGB `Bitmap`.
     *
     * @param image The YUV_420_888 formatted image from CameraX ImageAnalysis.
     * @param outputBitmap The Bitmap to write the RGB data into. Its dimensions must match the image.
     */
    @SuppressLint("UnsafeOptInUsageError") // Suppress warning for Image.getPlanes()
    fun yuvToRgb(image: Image, outputBitmap: Bitmap) {
        if (image.format != ImageFormat.YUV_420_888) {
            throw IllegalArgumentException("Unsupported image format, expected YUV_420_888")
        }

        val width = image.width
        val height = image.height
        val totalPixelCount = width * height

        // Re-allocate RenderScript buffers if image dimensions change
        if (inputAllocation == null || pixelCount != totalPixelCount) {
            pixelCount = totalPixelCount
            // YUV420_888 uses 1.5 bytes per pixel (1 Y + 0.5 U + 0.5 V)
            val yuvBytes = ByteArray(totalPixelCount * 3 / 2)
            inputAllocation = Allocation.createSized(rs, Element.U8(rs), yuvBytes.size)
            outputAllocation = Allocation.createFromBitmap(rs, outputBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
        }

        // Copy Y, U, and V planes into a single byte array for RenderScript processing
        // This is necessary because RenderScript expects a contiguous byte array for YUV input.
        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer

        val rowStrideY = image.planes[0].rowStride
        val rowStrideU = image.planes[1].rowStride
        val rowStrideV = image.planes[2].rowStride

        val pixelStrideU = image.planes[1].pixelStride
        val pixelStrideV = image.planes[2].pixelStride

        val ySize = yPlane.remaining()
        val uSize = uPlane.remaining()
        val vSize = vPlane.remaining()

        val yuvBytes = ByteArray(ySize + uSize + vSize)

        // Copy Y plane data directly
        yPlane.get(yuvBytes, 0, ySize)

        // Copy U and V planes, handling row and pixel strides.
        // U and V planes are typically sub-sampled (e.g., 2x2 blocks have 1 U and 1 V value).
        var uvIndex = ySize // Start UV data after Y data
        for (row in 0 until height / 2) { // Iterate over rows of the UV planes (which are half height)
            val uBufferIndex = row * rowStrideU
            val vBufferIndex = row * rowStrideV
            for (col in 0 until width / 2) { // Iterate over columns (which are half width)
                yuvBytes[uvIndex++] = uPlane.get(uBufferIndex + col * pixelStrideU)
                yuvBytes[uvIndex++] = vPlane.get(vBufferIndex + col * pixelStrideV)
            }
        }

        // Copy the combined YUV byte array to the input RenderScript allocation
        inputAllocation!!.copyFrom(yuvBytes)

        // Set the input and execute the YUV to RGB conversion
        scriptYuvToRgb.setInput(inputAllocation!!)
        scriptYuvToRgb.forEach(outputAllocation!!)

        // Copy the converted RGB data from RenderScript output allocation to the target Bitmap
        outputAllocation!!.copyTo(outputBitmap)
    }
}