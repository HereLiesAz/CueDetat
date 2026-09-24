package com.hereliesaz.cuedetat.data

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.BallIslandRules
import com.hereliesaz.cuedetat.domain.TopDownBallRules
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Finds balls in a top-down view of the table, rectified from the camera frame with the table
 * pose.
 *
 * With the pose known there is nothing to guess: the table is a rectangle of known size, every
 * ball has the same radius, and a position in the view is a position on the table. The one
 * distortion left is that balls stand off the felt, so each is stretched away from the camera;
 * [TopDownBallRules] accounts for it and puts each ball at its contact point.
 *
 * Not thread-safe: reuses scratch Mats.
 */
class TopDownBallDetector {

    /**
     * @param logical ball contact point in logical table space
     * @param type ball type
     * @param confidence 0..1
     */
    data class Ball(val logical: PointF, val type: BallType, val confidence: Float)

    /**
     * Local viewing geometry at a logical point: the unit direction pointing away from the camera
     * (in logical space) and the stretch k = 1 / sin(elevation).
     */
    data class ViewDirection(val awayX: Float, val awayY: Float, val stretch: Float)

    private val topDown = Mat()
    private val hsv = Mat()
    private val feltMask = Mat()
    private val islands = Mat()
    private val labels = Mat()
    private val stats = Mat()
    private val centroids = Mat()
    private val componentMask = Mat()
    private val nonZero = Mat()
    private val openKernel: Mat by lazy { Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0)) }

    /**
     * @param frame BGR camera frame
     * @param logicalToFrame 3x3 homography (CV_64F) from logical table space to [frame] pixels
     * @param tableWidth logical table width (x extent)
     * @param tableHeight logical table height (y extent)
     * @param ballRadius logical ball radius
     * @param viewDirection viewing geometry at a logical point; null where it can't be computed
     */
    fun detect(
        frame: Mat,
        logicalToFrame: Mat,
        tableWidth: Float,
        tableHeight: Float,
        ballRadius: Float,
        feltHsv: FloatArray,
        feltStdDev: FloatArray,
        viewDirection: (Float, Float) -> ViewDirection?,
    ): List<Ball> {
        // Top-down pixels per logical unit: a ball comes out RADIUS_PX in radius.
        val s = RADIUS_PX / ballRadius
        val outW = (tableWidth * s).toInt()
        val outH = (tableHeight * s).toInt()
        if (outW <= 0 || outH <= 0 || outW * outH > MAX_PIXELS) return emptyList()

        // Top-down pixel (u, v) -> logical (u / s - W/2, v / s - H/2) -> frame.
        val topDownToLogical = Mat(3, 3, CvType.CV_64F).apply {
            put(0, 0, 1.0 / s, 0.0, -tableWidth / 2.0, 0.0, 1.0 / s, -tableHeight / 2.0, 0.0, 0.0, 1.0)
        }
        val topDownToFrame = Mat()
        val none = Mat()
        try {
            Core.gemm(logicalToFrame, topDownToLogical, 1.0, none, 0.0, topDownToFrame)
            Imgproc.warpPerspective(
                frame, topDown, topDownToFrame, Size(outW.toDouble(), outH.toDouble()),
                Imgproc.INTER_LINEAR or Imgproc.WARP_INVERSE_MAP, Core.BORDER_CONSTANT, Scalar(0.0, 0.0, 0.0),
            )
        } finally {
            topDownToLogical.release()
            topDownToFrame.release()
            none.release()
        }

        Imgproc.cvtColor(topDown, hsv, Imgproc.COLOR_BGR2HSV)
        val k = 2.5
        Core.inRange(
            hsv,
            Scalar(max(0.0, feltHsv[0] - k * feltStdDev[0] - 5.0), max(25.0, feltHsv[1] - k * feltStdDev[1]), max(25.0, feltHsv[2] - k * feltStdDev[2])),
            Scalar(min(180.0, feltHsv[0] + k * feltStdDev[0] + 5.0), min(255.0, feltHsv[1] + k * feltStdDev[1]), min(255.0, feltHsv[2] + k * feltStdDev[2])),
            feltMask,
        )
        // Islands: not felt, and not the black border of the parts of the table out of view.
        Core.inRange(hsv, Scalar(0.0, 0.0, 1.0), Scalar(180.0, 255.0, 255.0), islands)
        Core.bitwise_not(feltMask, feltMask)
        Core.bitwise_and(islands, feltMask, islands)
        // Keep off the cushion line.
        val margin = (RADIUS_PX * 0.5).toInt()
        clear(0, outH, 0, margin)
        clear(0, outH, outW - margin, outW)
        clear(0, margin, 0, outW)
        clear(outH - margin, outH, 0, outW)
        Imgproc.morphologyEx(islands, islands, Imgproc.MORPH_OPEN, openKernel)

        val count = Imgproc.connectedComponentsWithStats(islands, labels, stats, centroids, 8, CvType.CV_32S)
        val results = ArrayList<Ball>()
        for (label in 1 until count) {
            val area = stats.get(label, Imgproc.CC_STAT_AREA)[0].toInt()
            if (area < MIN_AREA) continue
            val bx = stats.get(label, Imgproc.CC_STAT_LEFT)[0].toInt()
            val by = stats.get(label, Imgproc.CC_STAT_TOP)[0].toInt()
            val bw = stats.get(label, Imgproc.CC_STAT_WIDTH)[0].toInt()
            val bh = stats.get(label, Imgproc.CC_STAT_HEIGHT)[0].toInt()
            val cu = centroids.get(label, 0)[0].toFloat()
            val cv = centroids.get(label, 1)[0].toFloat()

            val dir = viewDirection(cu / s - tableWidth / 2f, cv / s - tableHeight / 2f) ?: continue
            val pts = islandPoints(label, bx, by, bw, bh) ?: continue

            // Extents along (away from the camera) and across.
            var aMin = Float.MAX_VALUE; var aMax = -Float.MAX_VALUE
            var cMin = Float.MAX_VALUE; var cMax = -Float.MAX_VALUE
            for (i in 0 until pts.size / 2) {
                val u = pts[i * 2]; val v = pts[i * 2 + 1]
                val a = u * dir.awayX + v * dir.awayY
                val c = -u * dir.awayY + v * dir.awayX
                if (a < aMin) aMin = a; if (a > aMax) aMax = a
                if (c < cMin) cMin = c; if (c > cMax) cMax = c
            }
            val length = aMax - aMin + 1f
            val width = cMax - cMin + 1f
            val cMid = (cMin + cMax) / 2f
            val e = TopDownBallRules.elevationFromStretch(dir.stretch)
            val offset = TopDownBallRules.contactOffset(RADIUS_PX.toFloat(), e)
            val oneLength = TopDownBallRules.maxSingleLength(RADIUS_PX.toFloat(), dir.stretch) / 1.3f
            val confidence = (area / (width * length) / 0.785f).coerceIn(0f, 1f)

            // Contact point from a near end (along coordinate) and an across coordinate.
            fun ball(nearEnd: Float, across: Float, lengthOfOne: Float): Ball {
                val a = nearEnd + offset
                val u = a * dir.awayX - across * dir.awayY
                val v = a * dir.awayY + across * dir.awayX
                val type = classify(u, v, nearEnd, lengthOfOne, across, dir)
                return Ball(PointF(u / s - tableWidth / 2f, v / s - tableHeight / 2f), type, confidence)
            }

            when (TopDownBallRules.judge(width, length, area, RADIUS_PX.toFloat(), dir.stretch)) {
                TopDownBallRules.Verdict.REJECT -> Unit
                TopDownBallRules.Verdict.SINGLE -> results += ball(aMin, cMid, length)
                // The front ball's near end is visible; the back ball starts where it would end.
                TopDownBallRules.Verdict.PAIR_ALONG -> {
                    results += ball(aMin, cMid, oneLength)
                    results += ball(aMin + oneLength, cMid, oneLength)
                }
                TopDownBallRules.Verdict.PAIR_ACROSS -> {
                    results += ball(aMin, cMin + RADIUS_PX, length)
                    results += ball(aMin, cMax - RADIUS_PX, length)
                }
            }
        }
        return results
    }

    private fun clear(r0: Int, r1: Int, c0: Int, c1: Int) {
        val band = islands.submat(r0, r1, c0, c1)
        band.setTo(Scalar(0.0))
        band.release()
    }

    /** Pixel coordinates (u0, v0, u1, v1, ...) of one island, in top-down pixels. */
    private fun islandPoints(label: Int, x: Int, y: Int, w: Int, h: Int): FloatArray? {
        val crop = labels.submat(y, y + h, x, x + w)
        try {
            Core.compare(crop, Scalar(label.toDouble()), componentMask, Core.CMP_EQ)
            Core.findNonZero(componentMask, nonZero)
            val n = nonZero.rows()
            if (n == 0) return null
            val raw = IntArray(n * 2)
            nonZero.get(0, 0, raw)
            return FloatArray(n * 2) { i -> raw[i] + if (i % 2 == 0) x.toFloat() else y.toFloat() }
        } finally {
            crop.release()
        }
    }

    /**
     * White / dark shares over the ball's visible body: a disk at the middle of its stretched
     * image (the body, not the felt-tinted rim or the contact shadow).
     */
    private fun classify(u: Float, v: Float, nearEnd: Float, length: Float, across: Float, dir: ViewDirection): BallType {
        val midA = nearEnd + length / 2f
        val cu = midA * dir.awayX - across * dir.awayY
        val cv = midA * dir.awayY + across * dir.awayX
        val r = RADIUS_PX * 0.8
        val x0 = (cu - r).toInt().coerceIn(0, hsv.cols() - 1)
        val y0 = (cv - r).toInt().coerceIn(0, hsv.rows() - 1)
        val x1 = (cu + r).toInt().coerceIn(x0 + 1, hsv.cols())
        val y1 = (cv + r).toInt().coerceIn(y0 + 1, hsv.rows())
        val roi = hsv.submat(y0, y1, x0, x1)
        val copy = Mat()
        try {
            roi.copyTo(copy)
            val px = ByteArray((copy.total() * 3).toInt())
            copy.get(0, 0, px)
            var total = 0; var white = 0; var dark = 0
            for (yy in 0 until copy.rows()) for (xx in 0 until copy.cols()) {
                if (hypot((x0 + xx - cu).toDouble(), (y0 + yy - cv).toDouble()) > r) continue
                val i = (yy * copy.cols() + xx) * 3
                val sat = px[i + 1].toInt() and 0xFF
                val value = px[i + 2].toInt() and 0xFF
                total++
                if (value < 60) dark++ else if (sat < 60 && value > 150) white++
            }
            if (total == 0) return BallType.UNKNOWN
            return when (BallIslandRules.classify(white / total.toFloat(), dark / total.toFloat(), RADIUS_PX.toFloat())) {
                BallIslandRules.BallKind.CUE -> BallType.CUE
                BallIslandRules.BallKind.EIGHT -> BallType.EIGHT
                BallIslandRules.BallKind.STRIPE -> BallType.STRIPE
                BallIslandRules.BallKind.SOLID -> BallType.SOLID
                BallIslandRules.BallKind.UNKNOWN -> BallType.UNKNOWN
            }
        } finally {
            copy.release()
            roi.release()
        }
    }

    fun release() {
        listOf(topDown, hsv, feltMask, islands, labels, stats, centroids, componentMask, nonZero).forEach { it.release() }
        openKernel.release()
    }

    private companion object {
        /** Ball radius in the top-down view, pixels. 8 px keeps the view about 300 x 600. */
        const val RADIUS_PX = 8.0
        /** Anything smaller than a quarter of a ball's disk (pi * 8^2 / 4 = 50 px) is noise. */
        const val MIN_AREA = 50
        /** Safety cap on the top-down view's size. */
        const val MAX_PIXELS = 1_500_000
    }
}
