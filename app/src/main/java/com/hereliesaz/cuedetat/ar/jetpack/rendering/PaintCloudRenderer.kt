package com.hereliesaz.cuedetat.ar.jetpack.rendering

import android.opengl.GLES20
import com.google.ar.core.PointCloud
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.checkGLError
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.loadGLShader

class PointCloudRenderer {
    private val TAG = PointCloudRenderer::class.java.simpleName

    private var program = 0
    private var positionAttrib = 0
    private var pointSizeUniform = 0
    private var colorUniform = 0
    private var modelViewProjectionUniform = 0

    private var numPoints = 0
    private var vertexBufferId = 0
    private var pointCloud: PointCloud? = null

    fun createOnGlThread() {
        checkGLError(TAG, "before create")
        val vertexShader = loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glUseProgram(program)
        checkGLError(TAG, "Program creation")

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
        pointSizeUniform = GLES20.glGetUniformLocation(program, "u_PointSize")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        checkGLError(TAG, "Program parameters")

        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        vertexBufferId = buffers[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        checkGLError(TAG, "Init VBO")
    }

    fun update(pointCloud: PointCloud) {
        if (pointCloud.timestamp == this.pointCloud?.timestamp) {
            return
        }
        this.pointCloud = pointCloud
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        numPoints = pointCloud.points.remaining() / 4
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            pointCloud.points.remaining(),
            pointCloud.points,
            GLES20.GL_DYNAMIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun draw(viewProjectionMatrix: FloatArray) {
        if (numPoints > 0) {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(positionAttrib)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
            GLES20.glVertexAttribPointer(positionAttrib, 4, GLES20.GL_FLOAT, false, 16, 0)
            GLES20.glUniform4f(colorUniform, 1.0f, 1.0f, 1.0f, 1.0f)
            GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, viewProjectionMatrix, 0)
            GLES20.glUniform1f(pointSizeUniform, 5.0f)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints)
            GLES20.glDisableVertexAttribArray(positionAttrib)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_ModelViewProjection;
            uniform float u_PointSize;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);
                gl_PointSize = u_PointSize;
            }
            """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
            """
    }
}