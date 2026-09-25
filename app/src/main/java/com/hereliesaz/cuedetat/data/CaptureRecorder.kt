package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import com.hereliesaz.cuedetat.BuildConfig
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
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
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
 * Training capture: while the user has agreed, keeps a steady, sharp camera frame about every two
 * seconds (see [CaptureSampler]) with everything the app knew at that moment, and sends it to
 * the developer for training the ball detector and the table-orientation model on the app's own
 * player's-eye view.
 *
 * Consent is asked once ([needsConsent]; the dialog is `CaptureConsentDialog`) and can be
 * changed any time from the menu. Nothing is captured until the user says yes.
 *
 * Each kept frame is two files in [dir] (`Android/data/<package>/files/captures`) until sent:
 * - `<id>.jpg`: the frame, rotated upright, full resolution.
 * - `<id>.json`: phone yaw/pitch/roll; the virtual table's pose (rotation, zoom, pan, whether
 *   locked, snap fit IoU) and its four corners in image pixels when it is on screen; the felt
 *   colour; camera intrinsics and AR pose when available; the balls the app detected, as boxes
 *   in image pixels. `ml/dataset/captures_to_labelstudio.py` turns these into Label Studio tasks
 *   with the detections as pre-labels.
 *
 * Sending: when [BuildConfig.CAPTURE_RELAY_URL] is set, pending pairs are POSTed to the relay
 * (`ml/capture-relay`, which commits them to a private GitHub repo; the app holds no GitHub
 * token), on unmetered networks only, oldest first, and deleted once accepted. With no relay
 * URL they stay on the phone. At most [MAX_PENDING] frames wait; capture pauses beyond that.
 * Each install sends a random id so frames group by phone without naming anyone.
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

    private val _enabled = MutableStateFlow(prefs.getString(KEY_CONSENT, null) == CONSENT_YES)
    /** True only after the user agreed. */
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _needsConsent = MutableStateFlow(prefs.getString(KEY_CONSENT, null) == null)
    /** True until the user has answered the consent dialog once. */
    val needsConsent: StateFlow<Boolean> = _needsConsent.asStateFlow()

    @Volatile private var pending = 0

    private val installId: String by lazy {
        prefs.getString(KEY_INSTALL_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALL_ID, it).apply()
        }
    }

    @Volatile private var lastKeptMs = 0L
    @Volatile private var lastOrientation: FloatArray? = null
    @Volatile private var lastOrientationMs = 0L

    val dir: File get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "captures")

    init {
        writer.execute {
            pending = countFrames()
            if (_enabled.value) uploadPending()
        }
    }

    /** Records the user's answer (from the consent dialog or the menu). */
    fun setEnabled(on: Boolean) {
        prefs.edit().putString(KEY_CONSENT, if (on) CONSENT_YES else CONSENT_NO).apply()
        _enabled.value = on
        _needsConsent.value = false
        if (on) writer.execute { uploadPending() }
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
        if (!_enabled.value || pending >= MAX_PENDING) return
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
                    if (write(copy, rotationDegrees, meta)) {
                        pending++
                        uploadPending()
                    } else {
                        lastKeptMs = 0L // blurred: try again on the next steady frame
                    }
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

    /**
     * Sends waiting frames to the relay, oldest first, deleting each once accepted. Stops at the
     * first failure (the next kept frame retries). Runs on [writer].
     */
    private fun uploadPending() {
        val relay = BuildConfig.CAPTURE_RELAY_URL
        if (relay.isBlank() || !_enabled.value || !unmetered()) return
        val images = dir.listFiles { f -> f.name.endsWith(".jpg") }?.sortedBy { it.name }.orEmpty()
        for (jpg in images.take(UPLOADS_PER_PASS)) {
            val json = File(dir, jpg.nameWithoutExtension + ".json")
            if (!json.exists()) { jpg.delete(); continue }
            if (!post(relay, jpg, json)) return
            jpg.delete(); json.delete()
            pending = (pending - 1).coerceAtLeast(0)
        }
    }

    private fun unmetered(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetwork != null && !cm.isActiveNetworkMetered
    }.getOrDefault(false)

    /** multipart/form-data POST of one pair to `<relay>/upload`; true on 2xx. */
    private fun post(relay: String, jpg: File, json: File): Boolean = runCatching {
        val boundary = "cuedetat" + UUID.randomUUID().toString().replace("-", "")
        val conn = (URL(relay.trimEnd('/') + "/upload").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("X-Install-Id", installId)
            setRequestProperty("X-App-Version", BuildConfig.VERSION_NAME)
        }
        try {
            DataOutputStream(conn.outputStream).use { out ->
                fun part(name: String, file: File, type: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n")
                    out.writeBytes("Content-Type: $type\r\n\r\n")
                    file.inputStream().use { it.copyTo(out) }
                    out.writeBytes("\r\n")
                }
                part("image", jpg, "image/jpeg")
                part("meta", json, "application/json")
                out.writeBytes("--$boundary--\r\n")
            }
            conn.responseCode in 200..299
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(false)

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
            "installId" to installId,
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
        const val KEY_CONSENT = "consent"
        const val KEY_INSTALL_ID = "install_id"
        const val CONSENT_YES = "yes"
        const val CONSENT_NO = "no"
        const val MAX_PENDING = 300
        const val UPLOADS_PER_PASS = 20
        const val SHARPNESS_WIDTH = 480.0
        const val JPEG_QUALITY = 92
    }
}
