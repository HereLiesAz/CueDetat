// app/src/main/java/com/hereliesaz/cuedetat/tracking/ball_detector/Ball.kt
package com.hereliesaz.cuedetat.tracking.ball_detector

data class Ball(
    val id: String, // Unique identifier for the ball
    val x: Int,     // X coordinate of the ball's center in pixels (from detected frame)
    val y: Int,     // Y coordinate of the ball's center in pixels (from detected frame)
    val radius: Int // Radius of the ball in pixels (from detected frame)
)