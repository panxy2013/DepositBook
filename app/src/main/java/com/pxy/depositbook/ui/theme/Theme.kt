package com.pxy.depositbook.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Surface,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = TextPrimary,
    secondary = Secondary,
    onSecondary = Surface,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = Accent,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SecondaryContainer,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = Danger,
)

private val Color_ForDark_variant = androidx.compose.ui.graphics.Color(0xFF263039)
private val Color_ForDark_outline = androidx.compose.ui.graphics.Color(0xFF34414C)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextPrimaryDark,
    secondary = Secondary,
    onSecondary = BackgroundDark,
    secondaryContainer = PrimaryLight,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = Accent,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color_ForDark_variant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color_ForDark_outline,
    error = Danger,
)

@Composable
fun DepositBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
