package com.example.ai_tranning.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/**
 * TaskCo light theme implementing the app's design system: a `blue-600` primary on a `gray-50`
 * background, white card surfaces, and `red-600` for errors/destructive actions. Text colors map to
 * `gray-900` (titles, `onSurface`) and `gray-600` (`onSurfaceVariant`, body).
 */
private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = SurfaceWhite,
    secondary = BluePrimary,
    onSecondary = SurfaceWhite,
    tertiary = BluePrimary,
    background = Gray50,
    onBackground = Gray900,
    surface = SurfaceWhite,
    onSurface = Gray900,
    surfaceVariant = SurfaceWhite,
    onSurfaceVariant = Gray600,
    error = DangerRed,
    onError = SurfaceWhite
)

/**
 * App-wide Material 3 theme wrapper.
 *
 * Selects a color scheme based on the system dark-mode setting and, on Android 12+, optional dynamic
 * (wallpaper-based) colors, then applies it along with the app [Typography] to [content].
 *
 * @param darkTheme whether to use the dark color scheme; defaults to the system setting.
 * @param dynamicColor whether to use dynamic colors on Android 12+; defaults to `true`.
 * @param content the composable content to theme.
 */
@Composable
fun Ai_TranningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default so the TaskCo brand palette (blue/gray-50) is used
    // consistently; it would otherwise be overridden by wallpaper colors on Android 12+.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}