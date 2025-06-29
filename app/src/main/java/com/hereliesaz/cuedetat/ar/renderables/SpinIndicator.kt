package com.hereliesaz.cuedetat.ar.renderables

import android.content.Context
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class SpinIndicator(context: Context, color: FloatArray) {
    private var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var modelViewProjectionHandle: Int = 0
    private var colorHandle: Int = 0
    private var vertexCount: Int = 0
    private val indicatorColor: FloatArray = color

    init {
        // A simple quad (2 triangles) to represent the dot
        val vertices = floatArrayOf(
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f,  1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f
        )
        vertexCount = vertices.size / 3

        val bb = ByteBuffer.allocateDirect(vertices.size * 4).apply {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }.also { vertexBuffer = it }
        }

        val vertexShader = ShaderUtil.loadGLShader("SpinIndicator", context, GLES30.GL_VERTEX_SHADER, "shaders/object.vert")
        val fragmentShader = ShaderUtil.loadGLShader("SpinIndicator", context, GLES30.GL_FRAGMENT_SHADER, "shaders/object.frag")

        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        positionHandle = GLES30.glGetAttribLocation(program, "a_Position")
        modelViewProjectionHandle = GLES30.glGetUniformLocation(program, "u_ModelViewProjection")
        colorHandle = GLES30.glGetUniformLocation(program, "u_Color")
    }

    fun draw(modelViewProjectionMatrix: FloatArray) {
        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(modelViewProjectionHandle, 1, false, modelViewProjectionMatrix, 0)
        GLES30.glUniform4fv(colorHandle, 1, indicatorColor, 0)
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glDisableVertexAttribArray(positionHandle)
    }
}