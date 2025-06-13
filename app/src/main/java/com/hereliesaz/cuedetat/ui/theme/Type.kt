package com.hereliesaz.cuedetat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.R

val Barbaro = FontFamily(
    Font(R.font.barbaro, FontWeight.Normal)
)

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Barbaro,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    )
)
