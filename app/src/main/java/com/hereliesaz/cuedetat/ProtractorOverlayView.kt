package com.hereliesaz.cuedetat

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ProtractorOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.argb(128, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private var aimAngle: Float = 0f
    private var confidence: Float? = null
    private var spinType: SpinType = SpinType.NONE
    private var tableEdges: List<Point>? = null

    fun setAimAngle(angle: Float) {
        aimAngle = angle
        invalidate()
    }

    fun setConfidence(conf: Float?) {
        confidence = conf
        invalidate()
    }

    fun setSpinType(spin: SpinType) {
        spinType = spin
        invalidate()
    }

    fun setTableEdges(edges: List<Point>?) {
        tableEdges = edges
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height.toFloat()
        val radius = width / 3f

        val baseColor = if (confidence == null) Color.GRAY else {
            val green = (255 * confidence!!).toInt().coerceIn(0, 255)
            val red = 255 - green
            Color.argb(200, red, green, 0)
        }
        paint.color = baseColor

        val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(rect, 135f, 90f, false, paint)

        paint.color = Color.WHITE
        for (angle in 135..225 step 15) {
            val rad = Math.toRadians(angle.toDouble())
            val startX = (centerX + radius * Math.cos(rad)).toFloat()
            val startY = (centerY + radius * Math.sin(rad)).toFloat()
            val endX = (centerX + (radius - 20) * Math.cos(rad)).toFloat()
            val endY = (centerY + (radius - 20) * Math.sin(rad)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        paint.color = Color.WHITE
        paint.strokeWidth = 5f
        val rad = Math.toRadians((aimAngle - 90).toDouble())
        val endX = (centerX + radius * Math.cos(rad)).toFloat()
        val endY = (centerY + radius * Math.sin(rad)).toFloat()
        canvas.drawLine(centerX, centerY, endX, endY, paint)

        confidence?.let {
            paint.color = Color.WHITE
            paint.textSize = 50f
            paint.strokeWidth = 1f
            val percent = (it * 100).toInt()
            canvas.drawText("Shot confidence: $percent%", centerX - 150f, centerY - radius - 40f, paint)
        }

        paint.color = Color.CYAN
        tableEdges?.takeIf { it.size == 4 }?.let { edges ->
            val path = Path().apply {
                moveTo(edges[0].x.toFloat(), edges[0].y.toFloat())
                for (i in 1 until edges.size) {
                    lineTo(edges[i].x.toFloat(), edges[i].y.toFloat())
                }
                close()
            }
            canvas.drawPath(path, paint)
        }

        paint.color = Color.YELLOW
        paint.textSize = 40f
        canvas.drawText("Spin: ${spinType.name}", 30f, 60f, paint)
    }
}
