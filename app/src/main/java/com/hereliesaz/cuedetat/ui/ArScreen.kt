package com.hereliesaz.cuedetat.ui

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.WindowManager
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun ArScreen(
    uiState: CueDetatState,
    visionRepository: VisionRepository,
    onEvent: (MainScreenEvent) -> Unit
) {
    if (!uiState.showArScreen) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Mutable state to hold the session
    var session by remember { mutableStateOf<Session?>(null) }
    var arRenderer by remember { mutableStateOf<ArRenderer?>(null) }

    // Vision data for overlay
    val visionData by visionRepository.visionDataFlow.collectAsState()

    // Lifecycle observer to pause/resume session
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        if (session == null) {
                            session = Session(context).apply {
                                val config = config
                                config.focusMode = Config.FocusMode.AUTO
                                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                                configure(config)
                            }
                        }
                        session?.resume()
                    } catch (e: Exception) {
                        Log.e("ArScreen", "Error resuming AR session", e)
                        onEvent(MainScreenEvent.ToggleArScreen) // Disable AR on error
                        // In a real app, emit a specific error event for a Toast
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    session?.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    session?.close()
                    session = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            session?.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                onEvent(MainScreenEvent.SizeChanged(size.width, size.height))
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    arRenderer?.queueTap(offset)
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    preserveEGLContextOnPause = true
                    setEGLContextClientVersion(2)
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)

                    val renderer = ArRenderer(
                        context = ctx,
                        uiState = uiState,
                        visionData = visionData,
                        onSessionChanged = { s -> session = s },
                        onProcessFrame = { image, rotation ->
                            coroutineScope.launch(Dispatchers.Default) {
                                try {
                                    visionRepository.processArFrame(image, rotation, uiState)
                                } finally {
                                    image.close()
                                }
                            }
                        },
                        onEventDispatch = onEvent
                    )
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    arRenderer = renderer
                }
            },
            update = { view ->
                arRenderer?.updateSession(session)
                arRenderer?.updateUiState(uiState)
                arRenderer?.updateVisionData(visionData)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Debug Overlay for Vision Data
        Canvas(modifier = Modifier.fillMaxSize()) {
            visionData.detectedBoundingBoxes.forEach { box ->
                val scaleX = size.width / visionData.sourceImageWidth
                val scaleY = size.height / visionData.sourceImageHeight

                if (visionData.sourceImageWidth > 0 && visionData.sourceImageHeight > 0) {
                     drawRect(
                        color = Color.Green,
                        topLeft = Offset(box.left * scaleX, box.top * scaleY),
                        size = androidx.compose.ui.geometry.Size(box.width() * scaleX, box.height() * scaleY),
                        style = Stroke(width = 5f)
                    )
                }
            }

            if (visionData.genericBalls.isNotEmpty()) {
                drawCircle(
                    color = Color.Cyan,
                    radius = 20f,
                    center = Offset(50f, 50f)
                )
            }

            // Visualize current AR table pose if set
            if (uiState.arTablePose != null) {
                // Drawing 3D pose in 2D canvas is hard without ViewMatrix/ProjectionMatrix access.
                // We'll trust the logic for now.
                drawCircle(
                    color = Color.Magenta,
                    radius = 30f,
                    center = Offset(size.width/2, size.height/2),
                    style = Stroke(width=5f)
                )
            }
        }

        Button(
            onClick = { onEvent(MainScreenEvent.ToggleArScreen) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Close AR")
        }
    }
}

class ArRenderer(
    private val context: Context,
    private var uiState: CueDetatState,
    private var visionData: VisionData,
    private val onSessionChanged: (Session?) -> Unit,
    private val onProcessFrame: (android.media.Image, Int) -> Unit,
    private val onEventDispatch: (MainScreenEvent) -> Unit
) : GLSurfaceView.Renderer {

    private var session: Session? = null
    private var backgroundRenderer = BackgroundRenderer()

    private var lastTableSnapTime = 0L
    private var lastObstacleSnapTime = 0L
    private val tapQueue = ConcurrentLinkedQueue<Offset>()
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var displayRotation = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread(null)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayRotation = windowManager.defaultDisplay.rotation
        session?.setDisplayGeometry(displayRotation, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val currentSession = session ?: return

        try {
            currentSession.setCameraTextureName(backgroundRenderer.textureId)
            val frame = currentSession.update()

            backgroundRenderer.draw(frame)

            processFrameForVision(frame)
            handleTableSnapping(frame)
            processTapQueue(frame)
            handleObstacleSnapping(frame)

        } catch (t: Throwable) {
            Log.e("ArRenderer", "Error drawing frame", t)
        }
    }

    private fun handleTableSnapping(frame: Frame) {
        if (uiState.isArTableSnapping && viewportWidth > 0 && viewportHeight > 0) {
             val currentTime = System.currentTimeMillis()
             if (currentTime - lastTableSnapTime > 100) {
                 lastTableSnapTime = currentTime

                 val hits = frame.hitTest(viewportWidth / 2f, viewportHeight / 2f)
                 val hit = hits.firstOrNull {
                     val trackable = it.trackable
                     trackable is Plane && trackable.isPoseInPolygon(it.hitPose)
                 }

                 if (hit != null) {
                     val pose = hit.hitPose
                     val poseArray = FloatArray(16)
                     pose.toMatrix(poseArray, 0)
                     onEventDispatch(MainScreenEvent.UpdateArTablePose(poseArray))
                 }
             }
        }
    }

    private fun handleObstacleSnapping(frame: Frame) {
        val tablePose = uiState.arTablePose
        if (uiState.areArObstaclesEnabled && tablePose != null && viewportWidth > 0 && viewportHeight > 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastObstacleSnapTime > 500) { // Update every 500ms
                lastObstacleSnapTime = currentTime

                val scaleX = viewportWidth.toFloat() / visionData.sourceImageWidth
                val scaleY = viewportHeight.toFloat() / visionData.sourceImageHeight
                val logicalPoints = mutableListOf<androidx.compose.ui.geometry.Offset>()

                visionData.detectedBoundingBoxes.forEach { box ->
                    val screenCX = box.centerX() * scaleX
                    val screenCY = box.centerY() * scaleY

                    val hits = frame.hitTest(screenCX, screenCY)
                    val hit = hits.firstOrNull { it.trackable is Plane }

                    if (hit != null) {
                        val pose = hit.hitPose
                        val worldPoint = floatArrayOf(pose.tx(), pose.ty(), pose.tz())
                        val logicalPoint = com.hereliesaz.cuedetat.utils.ArMath.worldToLogical(worldPoint, tablePose)
                        logicalPoints.add(androidx.compose.ui.geometry.Offset(logicalPoint.x, logicalPoint.y))
                    }
                }

                if (logicalPoints.isNotEmpty()) {
                    // Convert Offset to PointF for the event
                    val pointFs = logicalPoints.map { android.graphics.PointF(it.x, it.y) }
                    onEventDispatch(MainScreenEvent.PlaceArObstacles(pointFs))
                }
            }
        }
    }

    private fun processTapQueue(frame: Frame) {
        val tap = tapQueue.poll() ?: return
        val tablePose = uiState.arTablePose

        if (uiState.isArBallSnapping && tablePose != null) {
            val scaleX = viewportWidth.toFloat() / visionData.sourceImageWidth
            val scaleY = viewportHeight.toFloat() / visionData.sourceImageHeight

            var tappedBallCenter: android.graphics.PointF? = null

            for (box in visionData.detectedBoundingBoxes) {
                 val left = box.left * scaleX
                 val top = box.top * scaleY
                 val right = left + box.width() * scaleX
                 val bottom = top + box.height() * scaleY

                 if (tap.x >= left && tap.x <= right && tap.y >= top && tap.y <= bottom) {
                     tappedBallCenter = android.graphics.PointF(box.centerX().toFloat(), box.centerY().toFloat())
                     break
                 }
            }

            if (tappedBallCenter != null) {
                val screenCX = tappedBallCenter.x * scaleX
                val screenCY = tappedBallCenter.y * scaleY

                val hits = frame.hitTest(screenCX, screenCY)
                 val hit = hits.firstOrNull {
                     it.trackable is Plane
                 }

                 if (hit != null) {
                     val pose = hit.hitPose
                     val worldPoint = floatArrayOf(pose.tx(), pose.ty(), pose.tz())
                     val logicalPoint = com.hereliesaz.cuedetat.utils.ArMath.worldToLogical(worldPoint, tablePose)

                     onEventDispatch(MainScreenEvent.ArBallDetected(logicalPoint))
                 }
            }
        }
    }

    private fun processFrameForVision(frame: Frame) {
        val currentTime = System.currentTimeMillis()
        if (currentTime % 200 < 50) {
            try {
                 val image = frame.acquireCameraImage()
                 // Use a default rotation if not set, or pass the display rotation.
                 // CameraX uses degrees (0, 90, 180, 270). Display.rotation returns int constant (0, 1, 2, 3).
                 // We need to map them or just use a standard.
                 // For now, keeping 90 as a safe default for portrait phones but
                 // we should ideally query sensor orientation.
                 onProcessFrame(image, 90)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun queueTap(offset: Offset) {
        tapQueue.offer(offset)
    }

    fun updateSession(newSession: Session?) {
        this.session = newSession
        onSessionChanged(newSession)
    }

    fun updateUiState(newState: CueDetatState) {
        this.uiState = newState
    }

    fun updateVisionData(data: VisionData) {
        this.visionData = data
    }
}

// BackgroundRenderer class (same as before) ...
class BackgroundRenderer {
    var textureId = -1

    private val QUAD_COORDS = floatArrayOf(
        -1.0f, -1.0f, 0.0f,
        -1.0f, +1.0f, 0.0f,
        +1.0f, -1.0f, 0.0f,
        +1.0f, +1.0f, 0.0f
    )

    private val QUAD_TEXCOORDS = floatArrayOf(
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        1.0f, 0.0f
    )

    private var quadProgram: Int = 0
    private lateinit var quadPositionParam: ByteBuffer
    private lateinit var quadTexCoordParam: ByteBuffer

    fun createOnGlThread(context: Context?) {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST.toFloat())

        // Simple passthrough shader
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

        quadProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(quadProgram, vertexShader)
        GLES20.glAttachShader(quadProgram, fragmentShader)
        GLES20.glLinkProgram(quadProgram)

        val bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.size * 4)
        bbVertices.order(ByteOrder.nativeOrder())
        quadPositionParam = bbVertices
        val floatBuffer = quadPositionParam.asFloatBuffer()
        floatBuffer.put(QUAD_COORDS)
        floatBuffer.position(0)

        val bbTexCoords = ByteBuffer.allocateDirect(QUAD_TEXCOORDS.size * 4)
        bbTexCoords.order(ByteOrder.nativeOrder())
        quadTexCoordParam = bbTexCoords
        val texBuffer = quadTexCoordParam.asFloatBuffer()
        texBuffer.put(QUAD_TEXCOORDS)
        texBuffer.position(0)
    }

    fun draw(frame: Frame) {
        if (textureId == -1) return

        GLES20.glUseProgram(quadProgram)

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        val positionHandle = GLES20.glGetAttribLocation(quadProgram, "a_Position")
        val texCoordHandle = GLES20.glGetAttribLocation(quadProgram, "a_TexCoord")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, quadPositionParam.asFloatBuffer())

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, quadTexCoordParam.asFloatBuffer())

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        const val VERTEX_SHADER_CODE = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        const val FRAGMENT_SHADER_CODE = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES s_Texture;
            void main() {
                gl_FragColor = texture2D(s_Texture, v_TexCoord);
            }
        """
    }
}
