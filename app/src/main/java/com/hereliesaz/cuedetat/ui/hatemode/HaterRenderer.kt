package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import kotlin.math.hypot

data class Triangle(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var ax: Float = 0f,
    var ay: Float = 0f,
    var angle: Float = 0f,
    var angularVelocity: Float = 0f,
    var isTouched: Boolean = false
)

class HaterModeRenderer {

    private val triangles = mutableListOf<Triangle>()
    private val paint = Paint().apply {
        color = 0xFFAAAAAA.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var gravityX: Float = 0f
    private var gravityY: Float = 0.5f // default slow downward drift

    // Called every frame
    fun update(deltaTime: Float) {
        for (triangle in triangles) {
            // Apply gravity
            triangle.ax = gravityX
            triangle.ay = gravityY

            // Integrate acceleration to velocity (Euler)
            triangle.vx += triangle.ax * deltaTime
            triangle.vy += triangle.ay * deltaTime

            // Apply heavy viscous damping
            triangle.vx *= 0.85f
            triangle.vy *= 0.85f

            // Integrate velocity to position
            triangle.x += triangle.vx * deltaTime
            triangle.y += triangle.vy * deltaTime

            // Angular physics
            triangle.angularVelocity *= 0.85f
            triangle.angle += triangle.angularVelocity * deltaTime
        }
    }

    // Draw triangles
    fun draw(canvas: Canvas) {
        for (triangle in triangles) {
            canvas.save()
            canvas.translate(triangle.x, triangle.y)
            canvas.rotate(triangle.angle)
            val path = Path().apply {
                moveTo(0f, -30f)
                lineTo(26f, 15f)
                lineTo(-26f, 15f)
                close()
            }
            canvas.drawPath(path, paint)
            canvas.restore()
        }
    }

    // Gravity tilt input (from accelerometer or similar)
    fun setGravityVector(gx: Float, gy: Float) {
        gravityX = gx
        gravityY = gy
    }

    // Handle touch
    fun onTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                for (triangle in triangles) {
                    val dx = x - triangle.x
                    val dy = y - triangle.y
                    val dist = hypot(dx, dy)
                    if (dist < 60f) {
                        val pushX = dx * 0.05f
                        val pushY = dy * 0.05f
                        triangle.vx += pushX
                        triangle.vy += pushY

                        // Add angular momentum based on push strength
                        triangle.angularVelocity += (dx - dy) * 0.01f
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                for (triangle in triangles) {
                    val dx = x - triangle.x
                    val dy = y - triangle.y
                    if (hypot(dx, dy) < 60f) {
                        // Bobbing effect
                        triangle.vy -= 10f
                        triangle.angularVelocity += (Math.random().toFloat() - 0.5f) * 10f
                    }
                }
            }
        }
        return true
    }

    // Add a triangle (e.g., on init)
    fun addTriangle(x: Float, y: Float) {
        triangles.add(Triangle(x, y))
    }

    // Clear all triangles (if needed)
    fun reset() {
        triangles.clear()
    }
}
