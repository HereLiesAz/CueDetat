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
 * Renders a virtual sphere.
 */
class SphereRenderer {
    private val TAG = SphereRenderer::class.java.simpleName

    // Shader names are the same as the cube.
    private val VERTEX_SHADER_NAME = "shaders/object.vert"
    private val FRAGMENT_SHADER_NAME = "shaders/object.frag"

    private var program = 0
    private var positionAttrib = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0

    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var numIndices = 0

    private val modelViewProjectionMatrix = FloatArray(16)

    fun createOnGlThread() {
        val vertexShader = loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, ObjectRenderer.VERTEX_SHADER)
        val fragmentShader = loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, ObjectRenderer.FRAGMENT_SHADER)

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

        // Generate sphere vertices and indices
        generateSphere(32, 32, 0.5f) // 32 slices, 32 stacks, radius of 0.5 (unit sphere)
    }

    private fun generateSphere(slices: Int, stacks: Int, radius: Float) {
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (i in 0..stacks) {
            val phi = Math.PI.toFloat() * i.toFloat() / stacks
            for (j in 0..slices) {
                val theta = 2f * Math.PI.toFloat() * j.toFloat() / slices
                val x = radius * kotlin.math.sin(phi) * kotlin.math.cos(theta)
                val y = radius * kotlin.math.cos(phi)
                val z = radius * kotlin.math.sin(phi) * kotlin.math.sin(theta)
                vertices.add(x)
                vertices.add(y)
                vertices.add(z)
            }
        }

        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val first = (i * (slices + 1) + j).toShort()
                val second = (first + slices + 1).toShort()
                indices.add(first)
                indices.add(second)
                indices.add((first + 1).toShort())

                indices.add(second)
                indices.add((second + 1).toShort())
                indices.add((first + 1).toShort())
            }
        }

        numIndices = indices.size

        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer?.put(vertices.toFloatArray())
        vertexBuffer?.position(0)

        val ib = ByteBuffer.allocateDirect(indices.size * 2)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(indices.toShortArray())
        indexBuffer?.position(0)
    }

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

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numIndices, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionAttrib)

        checkGLError(TAG, "Drawing sphere")
    }
}