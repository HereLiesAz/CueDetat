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
val AppHelpTextPhoneOverCue = AppDarkYellow // As requested: #E5B205
val AppHelpTextTangentLine = Color(0xFFBB86FC) // As requested: purple_200
val AppHelpTextPocketAim = Color(0xFF018786)  // As requested: teal_700

val AppHelpTextTargetBallLabel = AppPurple // As requested: purple_500 (#FF6200EE)
val AppHelpTextCueBallLabel = AppMediumGray // As requested: app_medium_gray (#725F5F)


val AppHelpTextYellow = AppYellow // For general text related to yellow items
val AppHelpTextWhite = AppWhite   // For general text related to white items
val AppHelpTextPurple = AppPurple // For general text related to purple items (like projected shot line)


val AppWarningText = AppErrorRed

// Original purple/pink colors are no longer the primary theme.
/*
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
*/