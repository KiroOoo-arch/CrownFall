package com.crownfall.realm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serif display type with wide tracking reads as carved / illuminated text
 * while staying perfectly legible on a phone.
 */
val CrownfallTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = 6.sp,
        lineHeight = 50.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 4.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 2.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 1.6.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        letterSpacing = 1.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        letterSpacing = 0.4.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        letterSpacing = 0.35.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 13.sp,
        letterSpacing = 1.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    )
)
