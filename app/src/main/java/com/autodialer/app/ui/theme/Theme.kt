package com.autodialer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkOrangePrimary,
    onPrimary = DarkOrangeOnPrimary,
    primaryContainer = DarkOrangePrimaryContainer,
    onPrimaryContainer = DarkOrangeOnPrimaryContainer,
    secondary = OrangeAccent,
    onSecondary = DarkOrangeOnPrimary,
    secondaryContainer = DarkSurfaceContainerHigh,
    onSecondaryContainer = Color.White,
    tertiary = OrangeSecondary,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color(0xFFEDE0D8),
    surface = DarkSurface,
    onSurface = Color(0xFFEDE0D8),
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = Color(0xFFD7C2B4),
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorRedDark,
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = OrangeOnPrimaryContainer,
    secondary = OrangeSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceContainer,
    onSecondaryContainer = OrangeOnPrimaryContainer,
    tertiary = OrangeAccent,
    onTertiary = Color(0xFF2E1200),
    background = LightBackground,
    onBackground = Color(0xFF221A14),
    surface = LightSurface,
    onSurface = Color(0xFF221A14),
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = Color(0xFF52443B),
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorRed,
    onError = Color.White
)

/**
 * ================================================================================================
 * WHAT IS IT:
 * Root Material 3 theme Composable providing color schemes, shapes, and typography across the app.
 *
 * WHAT IT DOES:
 * 1. Resolves dynamic color schemes on Android 12+ (API 31+) if enabled.
 * 2. Provides custom Brand Orange [DarkColorScheme] and [LightColorScheme].
 * 3. Binds MaterialTheme composable down the tree.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Host Activity: [com.autodialer.app.MainActivity]
 * - Tokens: `Color.kt`
 * ================================================================================================
 */
@Composable
@Suppress("FunctionName")
fun AutoDialerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
