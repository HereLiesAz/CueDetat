package com.hereliesaz.cuedetat.config

object AppConfig {
    const val TAG = "CueDEtatApp"

    // Zoom Configuration
    const val MIN_ZOOM_FACTOR = 0.1f
    const val MAX_ZOOM_FACTOR = 4.0f
    const val DEFAULT_ZOOM_FACTOR = 0.4f

    // Pitch Sensor and Angle Configuration
    const val PITCH_SMOOTHING_FACTOR = 0.15f
    const val FORWARD_TILT_AS_FLAT_OFFSET_DEGREES = 15.0f
    const val PITCH_OFFSET_POWER = 2.0f // <--- ADD THIS LINE (use an appropriate value)

    // Protractor Visuals Configuration
    val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)

    // Interaction Configuration
    const val PAN_ROTATE_SENSITIVITY = 0.3f
    const val DEFAULT_ROTATION_ANGLE = 0.0f

    // General Graphics Configuration
    const val GLOW_RADIUS_FIXED = 8f

    // Ball Detection & Manual Placement
    const val DEFAULT_MANUAL_BALL_RADIUS = 25f
    const val BALL_RADIUS_SCALE_FACTOR = 0.6f // New: Scale factor for detected ball radii

    // Drag and Snap Configuration
    const val DRAG_TOUCH_TOLERANCE = 30f // Extra pixels around ball for easier grabbing
    const val SNAP_RADIUS_FACTOR = 1.2f // Factor of ball radius for snap distance (e.g., 1.2 * radius)

    // Text Size Configuration
    const val GHOST_BALL_NAME_BASE_SIZE = 40f
    const val FIT_TARGET_INSTRUCTION_BASE_SIZE_FACTOR = 0.9f
    const val PLACE_CUE_INSTRUCTION_BASE_SIZE_FACTOR = 0.9f
    const val HINT_TEXT_BASE_SIZE = 40f
    const val HINT_TEXT_SIZE_MULTIPLIER = 0.70f
    const val INVALID_SHOT_WARNING_BASE_SIZE = 160f
    const val SPECIAL_WARNING_TEXT_SMALLER_SIZE = 110f

    const val PLANE_LABEL_BASE_SIZE = 40f
    const val PROJECTED_SHOT_TEXT_SIZE_FACTOR = 1.0f
    const val TANGENT_LINE_TEXT_SIZE_FACTOR = 1.1f
    const val CUE_BALL_PATH_TEXT_SIZE_FACTOR = 1.1f
    const val POCKET_AIM_TEXT_SIZE_FACTOR = 1.25f

    const val TEXT_MIN_SCALE_FACTOR = 0.65f
    const val TEXT_MAX_SCALE_FACTOR = 1.5f

    // Default helper text color ARGB value
    const val DEFAULT_HELP_TEXT_COLOR_ARGB = 0xFFD1C4E9.toInt()

    // Paint Stroke Widths
    const val STROKE_AIM_LINE_NEAR = 4f
    const val STROKE_AIM_LINE_FAR = 2f
    const val STROKE_TARGET_LINE_GUIDE = 5f
    const val STROKE_DEFLECTION_LINE = 2f
    const val STROKE_DEFLECTION_LINE_BOLD_INCREASE = 4f
    const val STROKE_GHOST_BALL_OUTLINE = 3f
    const val STROKE_MAIN_CIRCLES = 5f
    const val STROKE_PROTRACTOR_ANGLE_LINES = 3f
    const val STROKE_AIMING_SIGHT = 2f
    const val STROKE_FOLLOW_DRAW_PATH = 3f

    // Follow/Draw Path Visuals Configuration
    const val FOLLOW_EFFECT_DEVIATION_DEGREES = -25.0f
    const val DRAW_EFFECT_DEVIATION_DEGREES = 25.0f
    const val CURVE_CONTROL_POINT_FACTOR = 0.6f
    const val PATH_DRAW_LENGTH_FACTOR = 0.7f
    const val PATH_OPACITY_ALPHA = 180

    // GitHub Release Updater Configuration
    const val GITHUB_REPO_OWNER = "HereLiesAz"
    const val GITHUB_REPO_NAME = "CueDetat"
    const val GITHUB_RELEASE_ASSET_NAME_REGEX = ".*\\.apk"


    // Warning Strings
    const val WARNING_MRS_CALLED = "Your Mrs. called, she wants her title back."
    const val WARNING_YODA_SAYS = "Yoda says, 'A do not, this is.'"
    val INSULTING_WARNING_STRINGS = listOf(
        "Nope.", "Not happening.", "Won't work.", "Please try harder.", "No.",
        "Physics says no.", WARNING_MRS_CALLED,
        "Hey batter, batter...", "In the beginning, God created a better shot.",
        WARNING_YODA_SAYS, "Am I crying from laughing, or is this just sad?",
        "Are you even trying?", "That's... an angle. Not a good one.",
        "I'm more disappointed than your father.", "Consult a physicist. Or a therapist."
    )
}