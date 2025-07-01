package com.hereliesaz.cuedetat.ar.jetpack.rendering

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Anchor
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.checkGLError
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.loadGLShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Renders a virtual object.
 */
class ObjectRenderer {
    private val TAG = ObjectRenderer::class.java.simpleName

    // Shader names.
    private val VERTEX_SHADER_NAME = "shaders/object.vert"
    private val FRAGMENT_SHADER_NAME = "shaders/object.frag"

    // Object properties
    private val CUBE_VERTICES = floatArrayOf(
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, 0.5f, -0.5f,
        -0.5f, 0.5f, -0.5f,
        -0.5f, -0.5f, 0.5f,
        0.5f, -0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f
    )

    private val CUBE_INDICES = shortArrayOf(
        0, 1, 2, 0, 2, 3, // Front face
        4, 5, 6, 4, 6, 7, // Back face
        3, 2, 6, 3, 6, 7, // Top face
        0, 1, 5, 0, 5, 4, // Bottom face
        1, 5, 6, 1, 6, 2, // Right face
        0, 4, 7, 0, 7, 3  // Left face
    )

    private var program = 0
    private var positionAttrib = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0

    private var vertexBuffer: FloatBuffer
    private var indexBuffer: ShortBuffer

    private val modelViewProjectionMatrix = FloatArray(16)

    init {
        val bb = ByteBuffer.allocateDirect(CUBE_VERTICES.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(CUBE_VERTICES)
        vertexBuffer.position(0)

        val ib = ByteBuffer.allocateDirect(CUBE_INDICES.size * 2)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer.put(CUBE_INDICES)
        indexBuffer.position(0)
    }

    fun createOnGlThread() {
        val vertexShader = loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glUseProgram(program)

        checkGLError(TAG, "Program creation")

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")

        checkGLError(TAG, "Program parameters")
    }

    /**
     * Draws the virtual object.
     *
     * @param modelMatrix The 4x4 model matrix.
     * @param viewMatrix The 4x4 view matrix.
     * @param projectionMatrix The 4x4 projection matrix.
     * @param color The color of the object, as a 4-element RGBA float array.
     */
    // REMOVED `private` to make this function public and accessible from ArRenderer
    fun draw(
        modelMatrix: FloatArray,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        color: FloatArray
    ) {
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, modelViewProjectionMatrix, 0, modelMatrix, 0)

        GLES20.glUseProgram(program)

        GLES20.glUniform4fv(colorUniform, 1, color, 0)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0)

        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, CUBE_INDICES.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionAttrib)

        checkGLError(TAG, "Drawing object")
    }

    companion object {
        const val VERTEX_SHADER = """
            uniform mat4 u_ModelViewProjection;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_ModelViewProjection * a_Position;
            }
        """

        const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """
    }
}