package com.hereliesaz.cuedetat.ar

import dev.romainguy.kotlin.math.Float3

object ARConstants {
    // Colors
    val CUE_BALL_COLOR = floatArrayOf(0.9f, 0.9f, 0.9f, 1.0f) // White
    val OBJECT_BALL_COLOR = floatArrayOf(0.8f, 0.2f, 0.2f, 1.0f) // Red
    val GHOST_BALL_COLOR = floatArrayOf(0.8f, 0.2f, 0.2f, 0.3f) // Transparent Red
    val POCKET_LINE_COLOR = floatArrayOf(0.2f, 0.8f, 0.2f, 1.0f) // Green
    val TANGENT_LINE_COLOR = floatArrayOf(0.2f, 0.2f, 0.8f, 1.0f) // Blue
    val CUE_PATH_COLOR = floatArrayOf(0.9f, 0.9f, 0.2f, 1.0f) // Yellow
    val SELECTION_COLOR = floatArrayOf(0.9f, 0.9f, 0.2f, 0.5f) // Transparent Yellow

    // Dimensions (in meters)
    const val TABLE_WIDTH = 1.27f
    const val TABLE_DEPTH = 2.54f
    const val TABLE_HEIGHT = 0.05f
    const val BALL_RADIUS = 0.028575f
    const val BALL_DIAMETER = BALL_RADIUS * 2

    // Physics Constants
    const val MAX_SQUIRT_DEGREES = 5.0f
    const val MAX_THROW_DEGREES = 3.0f

    // Pocket locations (local coordinates relative to table center)
    val POCKETS = arrayOf(
        Float3(TABLE_WIDTH / 2, 0f, TABLE_DEPTH / 2), // Top-right
        Float3(-TABLE_WIDTH / 2, 0f, TABLE_DEPTH / 2), // Top-left
        Float3(TABLE_WIDTH / 2, 0f, -TABLE_DEPTH / 2), // Bottom-right
        Float3(-TABLE_WIDTH / 2, 0f, -TABLE_DEPTH / 2), // Bottom-left
        Float3(0f, 0f, TABLE_DEPTH / 2), // Top-center
        Float3(0f, 0f, -TABLE_DEPTH / 2) // Bottom-center
    )
}
