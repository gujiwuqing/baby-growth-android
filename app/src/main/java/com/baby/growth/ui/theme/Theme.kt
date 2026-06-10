package com.baby.growth.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Pink40 = Color(0xFFFF9EC4)
val Pink80 = Color(0xFFFFD6E8)
val Pink90 = Color(0xFFFFF0F5)
val Mint40 = Color(0xFFA8E6CF)
val Mint80 = Color(0xFFD4F5E4)
val Background = Color(0xFFFFF5F7)
val Surface = Color(0xFFFFFFFF)
val OnPrimary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF333333)
val OnSurface = Color(0xFF333333)
val TextSecondary = Color(0xFF666666)
val TextHint = Color(0xFF999999)

private val ThemeColors = mapOf(
    "coral" to ThemeColorSet(
        primary = Color(0xFFE8857A),
        primaryContainer = Color(0xFFF5C5C0),
        onPrimaryContainer = Color(0xFF5D1610)
    ),
    "lavender" to ThemeColorSet(
        primary = Color(0xFFB8A9D4),
        primaryContainer = Color(0xFFDDD3EF),
        onPrimaryContainer = Color(0xFF3B2D55)
    ),
    "mint" to ThemeColorSet(
        primary = Color(0xFF8CC9B0),
        primaryContainer = Color(0xFFC6E8D8),
        onPrimaryContainer = Color(0xFF1B4D3A)
    ),
    "sky" to ThemeColorSet(
        primary = Color(0xFF9DC4E0),
        primaryContainer = Color(0xFFD1E8F5),
        onPrimaryContainer = Color(0xFF1E4A65)
    ),
    "sunshine" to ThemeColorSet(
        primary = Color(0xFFF5C5A3),
        primaryContainer = Color(0xFFFBE0CC),
        onPrimaryContainer = Color(0xFF6B4226)
    ),
    "sakura" to ThemeColorSet(
        primary = Color(0xFFFFB6C1),
        primaryContainer = Color(0xFFFFDAE0),
        onPrimaryContainer = Color(0xFF6B3040)
    )
)

private data class ThemeColorSet(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

private fun buildColorScheme(themeKey: String): ColorScheme {
    val colors = ThemeColors[themeKey] ?: ThemeColors["coral"]!!
    return lightColorScheme(
        primary = colors.primary,
        onPrimary = OnPrimary,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = Mint40,
        onSecondary = OnPrimary,
        secondaryContainer = Mint80,
        onSecondaryContainer = Color(0xFF002114),
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = TextSecondary,
        outline = Color(0xFFE0E0E0)
    )
}

@Composable
fun BabyGrowthTheme(
    themeKey: String = "coral",
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(themeKey)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}