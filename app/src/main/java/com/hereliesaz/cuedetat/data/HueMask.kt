package com.hereliesaz.cuedetat.data

import com.hereliesaz.cuedetat.domain.HueMath
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

/**
 * OpenCV side of [HueMath]: colour masks and hue statistics that treat hue as the circle it is.
 * `inRange` on a hue window that crosses the 0/180 seam silently matches nothing (or the wrong
 * thing); red and maroon cloth sits across it.
 */
internal object HueMask {

    /**
     * Writes to [out] the pixels of 8-bit HSV [hsv] whose hue is in any of [windows]
     * ([HueMath.windows]) and whose saturation and value are within the given bounds. [scratch]
     * is clobbered (needed when the window wraps).
     */
    fun inRange(
        hsv: Mat,
        windows: List<ClosedFloatingPointRange<Double>>,
        sLo: Double, sHi: Double, vLo: Double, vHi: Double,
        out: Mat, scratch: Mat,
    ) {
        windows.forEachIndexed { i, w ->
            val target = if (i == 0) out else scratch
            Core.inRange(hsv, Scalar(w.start, sLo, vLo), Scalar(w.endInclusive, sHi, vHi), target)
            if (i > 0) Core.bitwise_or(out, scratch, out)
        }
    }

    /**
     * The window `centre +/- halfWidth` in hue, with saturation and value bounds as for
     * [inRange]; [halfWidth] is capped so a wide spread cannot wrap round to the whole wheel.
     */
    fun inRangeAround(
        hsv: Mat, centre: Double, halfWidth: Double,
        sLo: Double, sHi: Double, vLo: Double, vHi: Double,
        out: Mat, scratch: Mat,
    ) = inRange(hsv, HueMath.windows(centre, halfWidth.coerceAtMost(MAX_HALF_WIDTH)), sLo, sHi, vLo, vHi, out, scratch)

    /** Circular mean and spread of the hue of the pixels of [hsv] under [mask]; null if none. */
    fun stats(hsv: Mat, mask: Mat): HueMath.Circular? {
        val hist = Mat()
        val channels = MatOfInt(0)
        val size = MatOfInt(HueMath.PERIOD)
        val ranges = MatOfFloat(0f, HueMath.PERIOD.toFloat())
        try {
            Imgproc.calcHist(listOf(hsv), channels, mask, hist, size, ranges)
            val bins = FloatArray(HueMath.PERIOD)
            hist.get(0, 0, bins)
            return HueMath.circularStats(bins)
        } finally {
            hist.release(); channels.release(); size.release(); ranges.release()
        }
    }

    /** A quarter of the wheel either side: wider than any real cloth's hue spread. */
    private const val MAX_HALF_WIDTH = 45.0
}
