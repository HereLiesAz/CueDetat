// app/src/main/java/com/hereliesaz/cuedetat/tracking/ball_detector/Ball.kt
package com.hereliesaz.cuedetat.tracking.ball_detector

/**
 * Represents a detected ball.
 * @param id A unique identifier for the ball, ideally from a tracking ID.
 * @param x The X coordinate of the ball's center in pixels (in the MainOverlayView's coordinate system).
 * @param y The Y coordinate of the ball's center in pixels (in the MainOverlayView's coordinate system).
 * @param radius The radius of the ball in pixels (in the MainOverlayView's coordinate system).
 */
data class Ball(
    val id: String,
    val x: Float,
    val y: Float,
    val radius: Float
)