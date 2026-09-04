package com.mastercompanion.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.lightColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = AccentEmerald,
    secondary = AccentCyan,
    tertiary = AccentAmber,
    background = PureBlack,
    surface = SurfaceDark,
    surfaceVariant = SurfaceCard,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = AccentCrimson
)

private val LightColorScheme = lightColorScheme(
    primary = AccentEmerald,
    secondary = AccentCyan,
    tertiary = AccentAmber,
    background = PorcelainBackground,
    surface = PorcelainSurface,
    surfaceVariant = PorcelainCard,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = PorcelainTextPrimary,
    onSurface = PorcelainTextPrimary,
    error = AccentCrimson
)

@Composable
fun MasterCompanionTheme(
    whiteTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (whiteTheme) LightColorScheme else DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = whiteTheme
                    isAppearanceLightNavigationBars = whiteTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
