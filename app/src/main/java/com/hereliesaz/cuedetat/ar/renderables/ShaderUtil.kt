package com.hereliesaz.cuedetat.ar.renderables

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.io.IOException

object ShaderUtil {
    fun loadGLShader(tag: String, context: Context, type: Int, filename: String): Int {
        val code = readShaderFileFromAssets(context, filename)
        var shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, code)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(tag, "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            shader = 0
        }
        if (shader == 0) {
            throw RuntimeException("Error creating shader.")
        }
        return shader
    }

    private fun readShaderFileFromAssets(context: Context, filename: String): String {
        try {
            context.assets.open(filename).use { inputStream ->
                return inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: IOException) {
            throw RuntimeException("Could not open shader file: $filename")
        }
    }
}