package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.gson.Gson
import com.hereliesaz.cuedetat.domain.CaptureSampler
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.ZoomMapping
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera intrinsics and pose for one frame, when the camera path knows them (ARCore does;
 * CameraX, here, does not).
 *
 * Intrinsics are in raw-frame pixels (sensor orientation, before [CaptureRecorder] rotates the
 * image upright). [pose] is ARCore's camera pose in world space: tx, ty, tz, qx, qy, qz, qw.
 */
data class CaptureCamera(
    val fx: Float,
    val fy: Float,
    val cx: Float,
    val cy: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    val pose: FloatArray?,
)

/**
 * Training-capture mode: while switched on, keeps a steady, sharp camera frame about every two
 * seconds (see [CaptureSampler]) with everything the app knew at that moment, for training the
 * ball detector and the table-orientation model on the app's own player's-eye view.
 *
 * Each kept frame is two files in [dir] (`Android/data/<package>/files/captures`):
 * - `<id>.jpg`: the frame, rotated upright, full resolution.
 * - `<id>.json`: phone yaw/pitch/roll; the virtual table's pose (rotation, zoom, pan, whether
 *   locked, snap fit IoU) and its four corners in image pixels when it is on screen; the felt
 *   colour; camera intrinsics and AR pose when available; the balls the app detected, as boxes
 *   in image pixels. `ml/dataset/captures_to_labelstudio.py` turns these into Label Studio tasks
 *   with the detections as pre-labels.
 *
 * The switch persists across launches. [shareZip] packs the folder for sharing.
 *
 * [offer] is called from the vision worker on every processed frame and returns at once when
 * capture is off or no frame is due; encoding and disk writes run on a single background thread.
 */
@Singleton
class CaptureRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val prefs = context.getSharedPreferences("capture", Context.MODE_PRIVATE)
    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "capture-writer") }
    private val writerBusy = AtomicBoolean(false)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _count = MutableStateFlow(0)
    /** Frames currently in [dir]. */
    val count: StateFlow<Int> = _count.asStateFlow()

    @Volatile private var lastKeptMs = 0L
    @Volatile private var lastOrientation: FloatArray? = null
    @Volatile private var lastOrientationMs = 0L

    val dir: File get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "captures")

    init {
        writer.execute { _count.value = countFrames() }
    }

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, on).apply()
        _enabled.value = on
    }

    /**
     * Keeps [frame] if capture is on and a frame is due, steady and sharp. The caller keeps
     * ownership of [frame]; it is copied before this returns.
     *
     * @param frame BGR, raw sensor orientation, full resolution
     * @param rotationDegrees clockwise rotation that makes [frame] upright
     * @param frameToView maps [frame] pixels to view pixels
     */
    fun offer(
        frame: Mat,
        rotationDegrees: Int,
        frameToView: Matrix,
        state: CueDetatState,
        balls: List<DetectedBall>,
        feltHsv: FloatArray?,
        feltStdDev: FloatArray?,
        camera: CaptureCamera?,
    ) {
        if (!_enabled.value) return
        val now = System.currentTimeMillis()
        val o = state.currentOrientation
        val orientation = floatArrayOf(o.yaw, o.pitch, o.roll)
        val steady = CaptureSampler.steady(lastOrientation, lastOrientationMs, orientation, now)
        lastOrientation = orientation
        lastOrientationMs = now
        if (!steady || !CaptureSampler.due(now, lastKeptMs)) return
        if (!writerBusy.compareAndSet(false, true)) return

        val meta = describe(now, frame.cols(), frame.rows(), rotationDegrees, frameToView, state, balls, feltHsv, feltStdDev, camera)
        val copy = frame.clone()
        lastKeptMs = now
        try {
            writer.execute {
                try {
                    if (write(copy, rotationDegrees, meta)) _count.value = _count.value + 1
                    else lastKeptMs = 0L // blurred: try again on the next steady frame
                } catch (_: Exception) {
                    // A failed write loses one frame, nothing else.
                } finally {
                    copy.release()
                    writerBusy.set(false)
                }
            }
        } catch (_: Exception) {
            copy.release()
            writerBusy.set(false)
        }
    }

    /** Zips [dir] into the cache for sharing; null when there is nothing to share. */
    fun shareZip(): File? {
        val files = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
        if (files.isEmpty()) return null
        val out = File(File(context.cacheDir, "shared").apply { mkdirs() }, "cuedetat-captures.zip")
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            for (f in files) {
                zip.putNextEntry(ZipEntry("captures/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return out
    }

    /** Deletes every captured frame. */
    fun deleteAll() {
        writer.execute {
            dir.listFiles()?.forEach { it.delete() }
            _count.value = 0
        }
    }

    private fun countFrames(): Int = dir.listFiles { f -> f.name.endsWith(".jpg") }?.size ?: 0

    private fun describe(
        now: Long,
        width: Int,
        height: Int,
        rotation: Int,
        frameToView: Matrix,
        state: CueDetatState,
        balls: List<DetectedBall>,
        feltHsv: FloatArray?,
        feltStdDev: FloatArray?,
        camera: CaptureCamera?,
    ): Map<String, Any?> {
        val (w, h) = CaptureSampler.uprightSize(width, height, rotation)
        val up = { x: Float, y: Float -> CaptureSampler.toUpright(x, y, width, height, rotation).let { listOf(it.first, it.second) } }

        val viewToFrame = Matrix()
        val corners = state.pitchMatrix
            ?.takeIf { state.table.isVisible && frameToView.invert(viewToFrame) }
            ?.let { pitch ->
                val pts = FloatArray(8)
                state.table.corners.forEachIndexed { i, c -> pts[i * 2] = c.x; pts[i * 2 + 1] = c.y }
                pitch.mapPoints(pts) // logical -> view
                viewToFrame.mapPoints(pts) // view -> raw frame
                List(4) { up(pts[it * 2], pts[it * 2 + 1]) }
            }
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode, state.isBeginnerViewLocked)
        val o = state.currentOrientation

        return mapOf(
            "id" to idOf(now),
            "timestampMs" to now,
            "image" to idOf(now) + ".jpg",
            "width" to w,
            "height" to h,
            "cameraMode" to state.cameraMode.name,
            "sensors" to mapOf("yawDeg" to o.yaw, "pitchDeg" to o.pitch, "rollDeg" to o.roll),
            "table" to mapOf(
                "size" to state.table.size.name,
                "visible" to state.table.isVisible,
                "locked" to state.isArTableLocked,
                "fitIou" to state.tableFit?.iou,
                "rotationDeg" to state.worldRotationDegrees,
                "zoom" to ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom),
                "offsetX" to state.viewOffset.x,
                "offsetY" to state.viewOffset.y,
                // TL, TR, BR, BL of the virtual table, image pixels; may fall outside the image.
                "cornersImage" to corners,
            ),
            "felt" to feltHsv?.let { mapOf("hsv" to it.toList(), "stdDev" to feltStdDev?.toList()) },
            "camera" to camera?.let {
                mapOf(
                    "fx" to it.fx, "fy" to it.fy, "cx" to it.cx, "cy" to it.cy,
                    "imageWidth" to it.imageWidth, "imageHeight" to it.imageHeight,
                    "pose" to it.pose?.toList(),
                    "note" to "intrinsics in raw sensor-orientation pixels; image rotated by rotationDegrees",
                )
            },
            "rotationDegrees" to rotation,
            "balls" to balls.mapNotNull { b ->
                val r = b.boundingBox ?: return@mapNotNull null
                val a = up(r.left.toFloat(), r.top.toFloat())
                val c = up(r.right.toFloat(), r.bottom.toFloat())
                mapOf(
                    "x" to minOf(a[0], c[0]), "y" to minOf(a[1], c[1]),
                    "w" to kotlin.math.abs(c[0] - a[0]), "h" to kotlin.math.abs(c[1] - a[1]),
                    "type" to b.type.name, "confidence" to b.confidence,
                )
            },
        )
    }

    /** Rotates, checks sharpness, writes the pair. False when the frame was too blurred. */
    private fun write(raw: Mat, rotation: Int, meta: Map<String, Any?>): Boolean {
        val upright = Mat()
        val gray = Mat()
        val small = Mat()
        val lap = Mat()
        val mean = MatOfDouble()
        val sd = MatOfDouble()
        try {
            when (((rotation % 360) + 360) % 360) {
                90 -> Core.rotate(raw, upright, Core.ROTATE_90_CLOCKWISE)
                180 -> Core.rotate(raw, upright, Core.ROTATE_180)
                270 -> Core.rotate(raw, upright, Core.ROTATE_90_COUNTERCLOCKWISE)
                else -> raw.copyTo(upright)
            }
            Imgproc.cvtColor(upright, gray, Imgproc.COLOR_BGR2GRAY)
            val scale = SHARPNESS_WIDTH / gray.cols().toDouble()
            Imgproc.resize(gray, small, Size(SHARPNESS_WIDTH, gray.rows() * scale))
            Imgproc.Laplacian(small, lap, CvType.CV_64F)
            Core.meanStdDev(lap, mean, sd)
            val variance = sd.get(0, 0)[0].let { it * it }
            if (!CaptureSampler.sharpEnough(variance)) return false

            dir.mkdirs()
            Imgproc.cvtColor(upright, upright, Imgproc.COLOR_BGR2RGBA)
            val bmp = Bitmap.createBitmap(upright.cols(), upright.rows(), Bitmap.Config.ARGB_8888)
            try {
                Utils.matToBitmap(upright, bmp)
                FileOutputStream(File(dir, meta["image"] as String)).use { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            } finally {
                bmp.recycle()
            }
            File(dir, "${meta["id"]}.json").writeText(gson.toJson(meta + ("sharpness" to variance)))
            return true
        } finally {
            upright.release(); gray.release(); small.release(); lap.release(); mean.release(); sd.release()
        }
    }

    private fun idOf(ms: Long) = "cap_$ms"

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val SHARPNESS_WIDTH = 480.0
        const val JPEG_QUALITY = 92
    }
}
