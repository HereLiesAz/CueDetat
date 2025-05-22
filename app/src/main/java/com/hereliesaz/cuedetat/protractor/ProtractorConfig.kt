package com.hereliesaz.cuedetat.protractor // Or com.hereliesaz.cuedetat.protractor

object ProtractorConfig {
    const val TAG = "PoolProtractorApp"

    // Zoom
    const val MIN_ZOOM_FACTOR = 0.1f
    const val MAX_ZOOM_FACTOR = 4.0f
    const val DEFAULT_ZOOM_FACTOR = 0.4f

    // Pitch
    const val PITCH_SMOOTHING_FACTOR = 0.15f
    const val FORWARD_TILT_AS_FLAT_OFFSET_DEGREES = 15.0f

    // Angles & Interaction
    val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    const val PAN_ROTATE_SENSITIVITY = 0.3f
    const val DEFAULT_ROTATION_ANGLE = 0.0f

    // Graphics
    const val GLOW_RADIUS_FIXED = 8f

    // Text
    const val BASE_GHOST_BALL_TEXT_SIZE = 40f
    const val MIN_GHOST_BALL_TEXT_SIZE = 22f
    const val MAX_GHOST_BALL_TEXT_SIZE = 75f

    const val HELPER_TEXT_BASE_SIZE = 40f
    const val HELPER_TEXT_MIN_SIZE_FACTOR = 0.65f
    const val HELPER_TEXT_MAX_SIZE_FACTOR = 1.5f

    const val POCKET_AIM_TEXT_BASE_SIZE_FACTOR = 1.25f

    const val CENTER_CUE_INSTRUCTION_SIZE_FACTOR = 0.9f
    const val CUE_BALL_PATH_TEXT_SIZE_FACTOR = 0.75f
    const val TANGENT_LINE_TEXT_SIZE_FACTOR = 0.5f

    const val WARNING_TEXT_BASE_SIZE = 200f
    const val SPECIAL_WARNING_TEXT_SMALLER_SIZE = 110f // Significantly reduced from 145f

    // Paint Strokes
    const val O_NEAR_DEFAULT_STROKE = 4f
    const val O_FAR_DEFAULT_STROKE = 2f
    const val O_YELLOW_TARGET_LINE_STROKE = 5f
    const val BOLD_STROKE_INCREASE = 4f
    const val O_CUE_DEFLECTION_STROKE_WIDTH = 2f

    // Insulting Warnings Pool - Keep exact strings here for matching
    const val WARNING_MRS_CALLED = "Your Mrs. called, she wants her title back."
    const val WARNING_YODA_SAYS = "Yoda says, 'A do not, this is.'"
    // Other warnings
    val INSULTING_WARNINGS = listOf(
        "Nope.", "Not happening.", "Won't work.", "Please try harder.", "No.",
        "Physics says no.", WARNING_MRS_CALLED,
        "Hey batter, batter...", "In the beginning, God created a better shot.",
        WARNING_YODA_SAYS, "Am I crying from laughing, or is this just sad?"
    )
}