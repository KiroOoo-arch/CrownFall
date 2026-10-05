package com.crownfall.realm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CrownfallColors = darkColorScheme(
    primary = CrownfallPalette.Gold,
    onPrimary = CrownfallPalette.Void,
    primaryContainer = CrownfallPalette.GoldDim,
    onPrimaryContainer = CrownfallPalette.Parchment,
    secondary = CrownfallPalette.Iron,
    onSecondary = CrownfallPalette.Void,
    secondaryContainer = CrownfallPalette.IronDark,
    onSecondaryContainer = CrownfallPalette.Parchment,
    tertiary = CrownfallPalette.Royal,
    onTertiary = CrownfallPalette.Parchment,
    background = CrownfallPalette.Night,
    onBackground = CrownfallPalette.Parchment,
    surface = CrownfallPalette.StoneDeep,
    onSurface = CrownfallPalette.Parchment,
    surfaceVariant = CrownfallPalette.StoneDark,
    onSurfaceVariant = CrownfallPalette.ParchmentDim,
    outline = CrownfallPalette.StoneEdge,
    error = CrownfallPalette.Blood,
    onError = CrownfallPalette.Parchment
)

@Composable
fun CrownfallTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CrownfallColors,
        typography = CrownfallTypography,
        content = content
    )
}

