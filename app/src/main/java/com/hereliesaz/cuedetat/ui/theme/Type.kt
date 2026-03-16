package com.hereliesaz.cuedetat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.R

// Define the custom "Barbaro" font family.
val Barbaro = FontFamily(
    Font(R.font.barbaro, FontWeight.Normal)
)

/**
 * Material 3 Typography definitions.
 * Overrides standard styles to use the custom font and specific sizes.
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
