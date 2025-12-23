package com.hereliesaz.cuedetat.ui

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
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
                                configure(config)
                            }
                        }
                        session?.resume()
                    } catch (e: Exception) {
                        e.printStackTrace()
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
    ) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    preserveEGLContextOnPause = true
                    setEGLContextClientVersion(2)
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)

                    val renderer = ArRenderer(
                        uiState = uiState,
                        onSessionChanged = { s -> session = s },
                        onProcessFrame = { image, rotation ->
                            coroutineScope.launch(Dispatchers.Default) {
                                try {
                                    visionRepository.processArFrame(image, rotation, uiState)
                                } finally {
                                    image.close()
                                }
                            }
                        }
                    )
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    arRenderer = renderer
                }
            },
            update = { view ->
                arRenderer?.updateSession(session)
                arRenderer?.updateUiState(uiState)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Debug Overlay for Vision Data
        Canvas(modifier = Modifier.fillMaxSize()) {
            visionData.detectedBoundingBoxes.forEach { box ->
                // Boxes are in image coordinates (camera resolution).
                // We need to scale them to the view size.
                // Note: This scaling assumes aspect fill or fit, which matches standard ARCore/CameraX preview behavior
                // but strictly speaking should respect the exact matrix.
                // For "Expert" debug, this is sufficient.
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
    private var uiState: CueDetatState,
    private val onSessionChanged: (Session?) -> Unit,
    private val onProcessFrame: (android.media.Image, Int) -> Unit
) : GLSurfaceView.Renderer {

    private var session: Session? = null
    private var backgroundRenderer = BackgroundRenderer()

    private val isProcessingFrame = AtomicBoolean(false)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread(null)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        session?.setDisplayGeometry(0, width, height) // 0 = ROTATION_0
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val currentSession = session ?: return

        try {
            currentSession.setCameraTextureName(backgroundRenderer.textureId)
            val frame = currentSession.update()

            backgroundRenderer.draw(frame)

            // Logic to send frame to VisionRepository
            // Only capture if not currently processing to avoid backlog/stutter
            // Simple atomic flag check.
            // NOTE: processArFrame in the repository has a throttle check too,
            // but we want to avoid even acquiring the image if possible.
            // Also we need to make sure the repository throttle doesn't conflict with our desire to close images.
            // The repository implementation assumes it receives an image.

            // We use a simple timestamp check here too or just rely on the atomic flag to
            // ensure we don't launch multiple coroutines.
            // But we can't easily know when the coroutine finishes unless we pass a callback.
            // The lambda passed `onProcessFrame` launches a job.
            // Actually, `image.close()` happens in that coroutine.

            // To properly throttle without callback:
            // Just acquire every Nth frame or use time.

            val currentTime = System.currentTimeMillis()
            if (currentTime % 200 < 50) { // Rough 5fps check
                try {
                     val image = frame.acquireCameraImage()
                     // We must ensure this image is eventually closed.
                     // The callback is responsible for it.
                     onProcessFrame(image, 90)
                } catch (e: Exception) {
                    // Ignore
                }
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun updateSession(newSession: Session?) {
        this.session = newSession
        onSessionChanged(newSession)
    }

    fun updateUiState(newState: CueDetatState) {
        this.uiState = newState
    }
}

// Minimal Background Renderer to draw camera feed
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
