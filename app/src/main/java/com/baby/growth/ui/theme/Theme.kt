package com.baby.growth.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─── 基础颜色Token ───
val OnPrimary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF2D2D3A)
val OnSurface = Color(0xFF2D2D3A)
val TextSecondary = Color(0xFF6B7280)
val TextHint = Color(0xFF9CA3AF)
val DividerColor = Color(0xFFF0F0F0)

// ─── 记录类型语义色 ───
object RecordColor {
    val Breast = Color(0xFFE8857A)
    val Formula = Color(0xFF5B8DEF)
    val Bottle = Color(0xFF9DC4E0)
    val Diaper = Color(0xFF8CC9B0)
    val Sleep = Color(0xFFB8A9D4)
    val Food = Color(0xFFF5C5A3)
    val Supplement = Color(0xFFA8D8EA)
    val Growth = Color(0xFFFFB6C1)

    fun fromKey(key: String): Color = when (key) {
        "breast" -> Breast
        "formula" -> Formula
        "bottle" -> Bottle
        "diaper" -> Diaper
        "sleep" -> Sleep
        "food" -> Food
        "supplement" -> Supplement
        "growth" -> Growth
        else -> Breast
    }
}

// ─── 状态语义色 ───
object StatusColor {
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
    val Info = Color(0xFF60A5FA)
}

// ─── 扩展颜色系统 ───
data class BabyGrowthColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val shimmer: Color,
    val tagBackground: Color,
    val tagText: Color,
    val iconTint: Color,
    val shadow: Color,
    val successLight: Color,
    val warningLight: Color,
    val errorLight: Color,
)

val LocalBabyGrowthColors = staticCompositionLocalOf {
    BabyGrowthColors(
        cardBackground = Color.White,
        cardBorder = Color(0xFFF3F4F6),
        shimmer = Color(0xFFF9FAFB),
        tagBackground = Color(0xFFF3F4F6),
        tagText = Color(0xFF6B7280),
        iconTint = Color(0xFF9CA3AF),
        shadow = Color(0x0A000000),
        successLight = Color(0xFFECFDF5),
        warningLight = Color(0xFFFFFBEB),
        errorLight = Color(0xFFFEF2F2),
    )
}

// ─── 排版系统 ───
data class BabyGrowthTypography(
    val displayLarge: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelSmall: TextStyle,
    val statValue: TextStyle,
    val statLabel: TextStyle,
)

val LocalBabyGrowthTypography = staticCompositionLocalOf {
    BabyGrowthTypography(
        displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
        statValue = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
        statLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary),
    )
}

// ─── 间距系统 ───
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

// ─── 圆角系统 ───
object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val full = 100.dp
}

// ─── 主题色板 ───
private data class ThemeColorSet(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val background: Color,
    val surfaceTint: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
)

private val ThemeColors = mapOf(
    "coral" to ThemeColorSet(
        primary = Color(0xFFE8857A),
        primaryContainer = Color(0xFFFFF0EE),
        onPrimaryContainer = Color(0xFF5D1610),
        secondary = Color(0xFFA8E6CF),
        secondaryContainer = Color(0xFFE8F8F0),
        background = Color(0xFFFFFAF9),
        surfaceTint = Color(0xFFFFF5F3),
        tertiary = Color(0xFFF5C5A3),
        tertiaryContainer = Color(0xFFFFF3EA),
    ),
    "lavender" to ThemeColorSet(
        primary = Color(0xFFB8A9D4),
        primaryContainer = Color(0xFFF3F0FA),
        onPrimaryContainer = Color(0xFF3B2D55),
        secondary = Color(0xFFA8D8EA),
        secondaryContainer = Color(0xFFE8F4FA),
        background = Color(0xFFFAF8FE),
        surfaceTint = Color(0xFFF7F5FC),
        tertiary = Color(0xFFDDD3EF),
        tertiaryContainer = Color(0xFFF0ECF7),
    ),
    "mint" to ThemeColorSet(
        primary = Color(0xFF8CC9B0),
        primaryContainer = Color(0xFFEDF8F3),
        onPrimaryContainer = Color(0xFF1B4D3A),
        secondary = Color(0xFFA8D8EA),
        secondaryContainer = Color(0xFFE8F4FA),
        background = Color(0xFFF7FCFA),
        surfaceTint = Color(0xFFF2FAF6),
        tertiary = Color(0xFFC6E8D8),
        tertiaryContainer = Color(0xFFE5F5ED),
    ),
    "sky" to ThemeColorSet(
        primary = Color(0xFF9DC4E0),
        primaryContainer = Color(0xFFEDF5FB),
        onPrimaryContainer = Color(0xFF1E4A65),
        secondary = Color(0xFFA8E6CF),
        secondaryContainer = Color(0xFFE8F8F0),
        background = Color(0xFFF7FBFE),
        surfaceTint = Color(0xFFF2F8FC),
        tertiary = Color(0xFFD1E8F5),
        tertiaryContainer = Color(0xFFE5F0F8),
    ),
    "sunshine" to ThemeColorSet(
        primary = Color(0xFFF5C5A3),
        primaryContainer = Color(0xFFFFF5EE),
        onPrimaryContainer = Color(0xFF6B4226),
        secondary = Color(0xFFA8E6CF),
        secondaryContainer = Color(0xFFE8F8F0),
        background = Color(0xFFFFFCF9),
        surfaceTint = Color(0xFFFFF9F4),
        tertiary = Color(0xFFFBE0CC),
        tertiaryContainer = Color(0xFFFFF0E5),
    ),
    "sakura" to ThemeColorSet(
        primary = Color(0xFFFFB6C1),
        primaryContainer = Color(0xFFFFF0F3),
        onPrimaryContainer = Color(0xFF6B3040),
        secondary = Color(0xFFB8A9D4),
        secondaryContainer = Color(0xFFF3F0FA),
        background = Color(0xFFFFF9FA),
        surfaceTint = Color(0xFFFFF5F7),
        tertiary = Color(0xFFFFDAE0),
        tertiaryContainer = Color(0xFFFFEDF0),
    )
)

private fun buildColorScheme(themeKey: String): ColorScheme {
    val colors = ThemeColors[themeKey] ?: ThemeColors["coral"]!!
    return lightColorScheme(
        primary = colors.primary,
        onPrimary = OnPrimary,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = colors.secondary,
        onSecondary = OnPrimary,
        secondaryContainer = colors.secondaryContainer,
        onSecondaryContainer = Color(0xFF002114),
        tertiary = colors.tertiary,
        tertiaryContainer = colors.tertiaryContainer,
        background = colors.background,
        onBackground = OnBackground,
        surface = Color.White,
        onSurface = OnSurface,
        surfaceVariant = Color(0xFFF9FAFB),
        onSurfaceVariant = TextSecondary,
        surfaceTint = colors.surfaceTint,
        outline = Color(0xFFE5E7EB),
        outlineVariant = Color(0xFFF3F4F6),
    )
}

private fun buildExtendedColors(themeKey: String): BabyGrowthColors {
    val colors = ThemeColors[themeKey] ?: ThemeColors["coral"]!!
    return BabyGrowthColors(
        cardBackground = Color.White,
        cardBorder = Color(0xFFF3F4F6),
        shimmer = Color(0xFFF9FAFB),
        tagBackground = colors.primaryContainer,
        tagText = colors.onPrimaryContainer,
        iconTint = colors.primary.copy(alpha = 0.7f),
        shadow = Color(0x08000000),
        successLight = Color(0xFFECFDF5),
        warningLight = Color(0xFFFFFBEB),
        errorLight = Color(0xFFFEF2F2),
    )
}

// ─── 全局访问入口 ───
object BabyGrowthTheme {
    val colors: BabyGrowthColors
        @Composable get() = LocalBabyGrowthColors.current

    val typography: BabyGrowthTypography
        @Composable get() = LocalBabyGrowthTypography.current
}

@Composable
fun BabyGrowthTheme(
    themeKey: String = "coral",
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(themeKey)
    val extendedColors = buildExtendedColors(themeKey)
    val babyTypography = LocalBabyGrowthTypography.current

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    CompositionLocalProvider(
        LocalBabyGrowthColors provides extendedColors,
        LocalBabyGrowthTypography provides babyTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}