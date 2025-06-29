package com.hereliesaz.cuedetat.ar

object ARConstants {
    // Standard 8-foot table dimensions in meters (playing surface)
    const val TABLE_WIDTH = 1.12f  // 44 inches
    const val TABLE_DEPTH = 2.24f // 88 inches
    const val BALL_DIAMETER = 0.05715f

    // Dimensions for rendering the table rails
    const val RAIL_HEIGHT = 0.04f
    const val RAIL_WIDTH = 0.07f

    // Diamonds are spaced at 1/4 the length of the table
    val DIAMOND_SPACING_DEPTH = TABLE_DEPTH / 4.0f
    val DIAMOND_SPACING_WIDTH = TABLE_WIDTH / 4.0f
}