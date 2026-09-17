package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CbzGoldPrimary,
    onPrimary = CbzGoldOnPrimary,
    primaryContainer = CbzDarkSurfaceHighlight,
    onPrimaryContainer = CbzGoldPrimary,
    secondary = CbzSecondary,
    onSecondary = Color.Black,
    secondaryContainer = CbzDarkSurfaceVariant,
    onSecondaryContainer = CbzTextPrimary,
    tertiary = CbzTertiary,
    onTertiary = Color.Black,
    background = CbzDarkBackground,
    onBackground = CbzTextPrimary,
    surface = CbzDarkSurface,
    onSurface = CbzTextPrimary,
    surfaceVariant = CbzDarkSurfaceVariant,
    onSurfaceVariant = CbzTextSecondary,
    outline = CbzDarkOutline,
    surfaceContainer = CbzDarkSurfaceContainer,
    surfaceContainerHigh = CbzDarkSurfaceHighlight
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC9840E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD0),
    onPrimaryContainer = Color(0xFF3B2300),
    secondary = Color(0xFF5A6072),
    onSecondary = Color.White,
    background = CbzLightBackground,
    onBackground = CbzLightTextPrimary,
    surface = CbzLightSurface,
    onSurface = CbzLightTextPrimary,
    surfaceVariant = CbzLightSurfaceVariant,
    onSurfaceVariant = CbzLightTextSecondary,
    outline = Color(0xFFD3D7E2)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark-first for comic/manga reader
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

