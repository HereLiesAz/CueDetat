// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ArCoreScene.kt
package com.hereliesaz.cuedetat.ui.composables

import android.content.Context
import android.opengl.GLES11Ext // Correct import for GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import com.google.ar.core.exceptions.UnavailableException
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "ArCoreScene"

/**
 * This Composable integrates ARCore's rendering and session management.
 * It serves as a placeholder for a complete AR rendering solution.
 *
 * NOTE: This is a skeletal implementation.
 * You will need to:
 * 1. Implement actual OpenGL ES rendering for AR camera background.
 * 2. Implement OpenGL ES rendering for your virtual objects (protractor, balls)
 * using ARCore's projection and view matrices.
 * 3. Handle ARCore's hit-testing and plane detection for object placement/interaction.
 * 4. Adapt your existing rendering logic from LineRenderer, BallRenderer, etc.,
 * to work with OpenGL ES in this AR context.
 */
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun ArCoreScene(
    modifier: Modifier = Modifier,
    arSession: Session?, // ARCore Session from MainActivity
    uiState: OverlayState // Pass the full UI state for rendering parameters
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2) // Use OpenGL ES 2.0
            setRenderer(ArCoreRenderer(context, arSession, uiState)) // Pass context to renderer
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY // Render continuously
            setWillNotDraw(false) // Allow drawing (important for GLSurfaceView)
        }
    }

    DisposableEffect(lifecycleOwner, arSession) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        arSession?.resume()
                        Log.d(TAG, "AR Session resumed from Lifecycle.")
                    } catch (e: CameraNotAvailableException) {
                        Log.e(TAG, "Camera not available during ARCore resume", e)
                        // Handle camera not available (e.g., show error message to user)
                    } catch (e: UnavailableException) {
                        Log.e(TAG, "ARCore unavailable during resume", e)
                        // Handle ARCore unavailable (e.g., fallback to non-AR mode)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    arSession?.pause()
                    Log.d(TAG, "AR Session paused from Lifecycle.")
                }
                Lifecycle.Event.ON_DESTROY -> {
                    arSession?.close() // Release ARCore resources
                    Log.d(TAG, "AR Session closed from Lifecycle.")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { glSurfaceView },
        modifier = modifier
    )
}

/**
 * OpenGL ES Renderer for ARCore content.
 * This class handles rendering the camera background and virtual AR objects.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ArCoreRenderer(
    private val context: Context, // Context for resource loading (e.g., shaders)
    private var arSession: Session?,
    private var uiState: OverlayState // UI state to drive rendering of virtual objects
) : GLSurfaceView.Renderer {

    // Conceptual renderer for AR camera background
    private lateinit var arBackgroundRenderer: ArBackgroundRenderer
    private var backgroundRendererInitialized = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Clear the screen to a solid color initially
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Initialize AR background renderer
        arBackgroundRenderer = ArBackgroundRenderer()
        try {
            arBackgroundRenderer.createOnGlThread(context)
            backgroundRendererInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AR background renderer", e)
        }

        // Enable depth testing for proper object occlusion
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // TODO: Initialize your OpenGL ES programs and assets for virtual objects (e.g., protractor, balls) here.
        // You will need to load shaders, create vertex buffers, etc.
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        // Update ARCore session's display geometry
        val display: Display? = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        if (display != null) {
            arSession?.setDisplayGeometry(display.rotation, width, height)
        } else {
            Log.w(TAG, "Could not get display to set ARCore display geometry.")
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (arSession == null || !backgroundRendererInitialized) {
            return
        }

        try {
            // Update the ARCore session to get the latest frame
            val frame: Frame = arSession!!.update()
            val camera = frame.camera

            // If the camera is tracking, draw the AR camera background and virtual objects
            if (camera.trackingState == TrackingState.TRACKING) {
                // Render the AR camera background (e.g., video stream)
                arBackgroundRenderer.draw(frame)

                // Get ARCore camera projection matrix (for perspective)
                val projectionMatrix = FloatArray(16)
                camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f) // Near and far planes

                // Get ARCore camera view matrix (for camera position/orientation)
                val viewMatrix = FloatArray(16)
                camera.getViewMatrix(viewMatrix, 0)

                // TODO: Here's where you would render your virtual objects (protractor lines, balls)
                // You would use `uiState` to get the logical positions/parameters of your objects,
                // and then apply `projectionMatrix` and `viewMatrix` (and potentially ARCore's `Pose` for anchors)
                // to transform these logical objects into their correct 3D AR positions.

                // Example: conceptually pass rendering data
                // YourCustomProtractorRenderer.draw(projectionMatrix, viewMatrix, uiState.protractorUnit, ...)
                // YourCustomBallRenderer.draw(projectionMatrix, viewMatrix, uiState.actualCueBall, ...)
            }
        } catch (e: SessionPausedException) {
            Log.w(TAG, "AR Session paused during onDrawFrame", e)
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            // Handle camera not available (e.g., show error message)
        } catch (e: UnavailableException) {
            Log.e(TAG, "ARCore unavailable during onDrawFrame", e)
            // Handle ARCore unavailable (e.g., fallback to non-AR mode or show error)
        } catch (t: Throwable) {
            Log.e(TAG, "Exception on draw frame", t)
        }
    }

    /**
     * Conceptual helper class for rendering the AR camera background.
     * In a real ARCore app, this would use a specific shader program to draw the camera texture.
     * This is highly simplified and serves as a placeholder.
     */
    class ArBackgroundRenderer {
        private var textureId: Int = -1
        // Corrected: Use GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        private val TEXTURE_TARGET = GLES11Ext.GL_TEXTURE_EXTERNAL_OES // For camera texture

        fun createOnGlThread(context: Context) {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(TEXTURE_TARGET, textureId)

            // Set texture parameters for linear filtering and clamping to edge
            GLES20.glTexParameteri(TEXTURE_TARGET, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(TEXTURE_TARGET, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(TEXTURE_TARGET, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(TEXTURE_TARGET, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            // TODO: In a complete implementation, you'd load shader programs (vertex and fragment shaders)
            // and set up a full-screen quad to draw the camera texture onto.
        }

        fun draw(frame: Frame) {
            // Update the camera texture with the latest frame
            // GLES20.glBindTexture(TEXTURE_TARGET, textureId) // Re-bind if necessary
            // arSession?.setCameraTextureName(textureId) // This is typically done earlier in session setup
            // frame.transformDisplayUvCoords( /* your texture coords */ );

            // TODO: In a real ARCore background renderer, you would draw the camera feed.
            // This involves drawing a quad (two triangles) that fills the screen,
            // and mapping the camera texture onto it using the correct UV coordinates.
            // The texture ID needs to be linked to the ARCore session via `session.setCameraTextureName`.
        }
    }
}