package com.hereliesaz.cuedetat.ui.theme

import androidx.compose.ui.graphics.Color

// New Theme Colors based on the 8-ball aesthetic
val AppYellow = Color(0xFFFCC506)
val AppBlack = Color(0xFF000000)
val AppWhite = Color(0xFFFFFFFF)
val AppDarkGray = Color(0xFF333333) // For containers or darker accents
val AppMediumGray = Color(0xFF725F5F) // User's provided gray, good for tertiary
val AppLightGrayTextOnDark = Color(0xFFCCCCCC) // For text on dark containers
val AppDarkYellow = Color(0xFFE5B205) // A darker variant of the main yellow
val AppPurple = Color(0xFF6200EE) // A standard purple for the extended shot line

val AppErrorRed = Color(0xFFFF5252)
val AppHelpTextDefault = Color(0xFFD1C4E9) // Default helper text color

// Specific Helper Text Colors (can reuse AppColors or define new ones)
val AppHelpTextPhoneOverCue = AppDarkYellow
val AppHelpTextTangentLine = Color(0xFFBB86FC)
val AppHelpTextPocketAim = Color(0xFF018786)

val AppHelpTextTargetBallLabel = AppPurple
// val AppHelpTextCueBallLabel = AppMediumGray // Old color for "Cue Ball"
val AppHelpTextGhostBallLabel = AppLightGrayTextOnDark // New color for "Ghost Ball" label

val AppHelpTextProjectedShotLineActual = AppDarkYellow


val AppHelpTextYellow = AppYellow
val AppHelpTextWhite = AppWhite
val AppHelpTextPurple = AppPurple


val AppWarningText = AppErrorRed