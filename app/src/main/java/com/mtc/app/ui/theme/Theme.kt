package com.mtc.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mtc.app.data.remote.PreferencesManager

private fun getPrimaryColorScheme(colorName: String, darkTheme: Boolean): ColorScheme {
    return when (colorName) {
        "jade" -> if (darkTheme) {
            darkColorScheme(
                primary = JadeDark,
                onPrimary = Color.Black,
                primaryContainer = JadeDarkContainer,
                onPrimaryContainer = JadeOnDark,
                secondary = MtcSecondary,
                onSecondary = Color.Black,
                secondaryContainer = MtcSecondaryVariant,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                error = ErrorColor,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = JadeLight,
                onPrimary = JadeOnLight,
                primaryContainer = JadeLightContainer,
                onPrimaryContainer = JadeOnLightContainer,
                secondary = MtcSecondary,
                onSecondary = OnSecondaryLight,
                secondaryContainer = MtcSecondaryVariant,
                background = BackgroundLight,
                onBackground = OnBackgroundLight,
                surface = SurfaceLight,
                onSurface = OnSurfaceLight,
                error = ErrorColor,
                onError = OnPrimaryLight
            )
        }
        "amber" -> if (darkTheme) {
            darkColorScheme(
                primary = AmberDark,
                onPrimary = Color.Black,
                primaryContainer = AmberDarkContainer,
                onPrimaryContainer = AmberOnDark,
                secondary = MtcSecondary,
                onSecondary = Color.Black,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                error = ErrorColor,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = AmberLight,
                onPrimary = AmberOnLight,
                primaryContainer = AmberLightContainer,
                onPrimaryContainer = AmberOnLightContainer,
                secondary = MtcSecondary,
                onSecondary = OnSecondaryLight,
                background = BackgroundLight,
                onBackground = OnBackgroundLight,
                surface = SurfaceLight,
                onSurface = OnSurfaceLight,
                error = ErrorColor,
                onError = OnPrimaryLight
            )
        }
        "rose" -> if (darkTheme) {
            darkColorScheme(
                primary = RoseDark,
                onPrimary = Color.Black,
                primaryContainer = RoseDarkContainer,
                onPrimaryContainer = RoseOnDark,
                secondary = MtcSecondary,
                onSecondary = Color.Black,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                error = ErrorColor,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = RoseLight,
                onPrimary = RoseOnLight,
                primaryContainer = RoseLightContainer,
                onPrimaryContainer = RoseOnLightContainer,
                secondary = MtcSecondary,
                onSecondary = OnSecondaryLight,
                background = BackgroundLight,
                onBackground = OnBackgroundLight,
                surface = SurfaceLight,
                onSurface = OnSurfaceLight,
                error = ErrorColor,
                onError = OnPrimaryLight
            )
        }
        "sky" -> if (darkTheme) {
            darkColorScheme(
                primary = SkyDark,
                onPrimary = Color.Black,
                primaryContainer = SkyDarkContainer,
                onPrimaryContainer = SkyOnDark,
                secondary = MtcSecondary,
                onSecondary = Color.Black,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                error = ErrorColor,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = SkyLight,
                onPrimary = SkyOnLight,
                primaryContainer = SkyLightContainer,
                onPrimaryContainer = SkyOnLightContainer,
                secondary = MtcSecondary,
                onSecondary = OnSecondaryLight,
                background = BackgroundLight,
                onBackground = OnBackgroundLight,
                surface = SurfaceLight,
                onSurface = OnSurfaceLight,
                error = ErrorColor,
                onError = OnPrimaryLight
            )
        }
        "violet" -> if (darkTheme) {
            darkColorScheme(
                primary = VioletDark,
                onPrimary = Color.Black,
                primaryContainer = VioletDarkContainer,
                onPrimaryContainer = VioletOnDark,
                secondary = MtcSecondary,
                onSecondary = Color.Black,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                error = ErrorColor,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = VioletLight,
                onPrimary = VioletOnLight,
                primaryContainer = VioletLightContainer,
                onPrimaryContainer = VioletOnLightContainer,
                secondary = MtcSecondary,
                onSecondary = OnSecondaryLight,
                background = BackgroundLight,
                onBackground = OnBackgroundLight,
                surface = SurfaceLight,
                onSurface = OnSurfaceLight,
                error = ErrorColor,
                onError = OnPrimaryLight
            )
        }
        "forest" -> if (darkTheme) {
            darkColorScheme(
                primary = ForestDark,
                onPrimary = Color.Black,
                primaryContainer = ForestDarkContainer,
                onPrimaryContainer = ForestOnDark,
                secondary = MtcSecondary,
                onSecondary = Color.Black,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                error = ErrorColor,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = ForestLight,
                onPrimary = ForestOnLight,
                primaryContainer = ForestLightContainer,
                onPrimaryContainer = ForestOnLightContainer,
                secondary = MtcSecondary,
                onSecondary = OnSecondaryLight,
                background = BackgroundLight,
                onBackground = OnBackgroundLight,
                surface = SurfaceLight,
                onSurface = OnSurfaceLight,
                error = ErrorColor,
                onError = OnPrimaryLight
            )
        }
        else -> if (darkTheme) {
            darkColorScheme(
                primary = JadeDark,
                onPrimary = Color.Black,
                primaryContainer = JadeDarkContainer,
                onPrimaryContainer = JadeOnDark,
                secondary = MtcSecondary,
                onSecondary = Color.Black,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                error = ErrorColor,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = JadeLight,
                onPrimary = JadeOnLight,
                primaryContainer = JadeLightContainer,
                onPrimaryContainer = JadeOnLightContainer,
                secondary = MtcSecondary,
                onSecondary = OnSecondaryLight,
                background = BackgroundLight,
                onBackground = OnBackgroundLight,
                surface = SurfaceLight,
                onSurface = OnSurfaceLight,
                error = ErrorColor,
                onError = OnPrimaryLight
            )
        }
    }
}

@Composable
fun MtcTheme(
    darkThemeOverride: Boolean? = null,
    primaryColorOverride: String? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // 首次读取后缓存，后续重组不会重新订阅 DataStore
    var cachedThemePrefs by remember { mutableStateOf("light") }
    var cachedPrimaryColor by remember { mutableStateOf("jade") }

    LaunchedEffect(Unit) {
        PreferencesManager.getTheme(context).collect { cachedThemePrefs = it }
    }
    LaunchedEffect(Unit) {
        PreferencesManager.getPrimaryColor(context).collect { cachedPrimaryColor = it }
    }

    // 确定是否深色模式
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkThemeOverride ?: cachedThemePrefs) {
        "dark" -> true
        "light" -> false
        "auto" -> systemDark
        else -> false
    }

    val colorScheme = getPrimaryColorScheme(
        colorName = primaryColorOverride ?: cachedPrimaryColor,
        darkTheme = isDark
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
