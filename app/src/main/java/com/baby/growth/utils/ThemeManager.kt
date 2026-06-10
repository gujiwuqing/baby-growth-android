package com.baby.growth.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * 主题管理工具
 * 支持6种预设主题色
 */
object ThemeManager {

    data class ThemeConfig(
        val key: String,
        val name: String,
        val primaryHex: String,
        val emoji: String
    )

    val THEMES = listOf(
        ThemeConfig("coral", "珊瑚粉", "#E8857A", "🌸"),
        ThemeConfig("lavender", "薰衣草紫", "#B8A9D4", "💜"),
        ThemeConfig("mint", "薄荷绿", "#8CC9B0", "🌿"),
        ThemeConfig("sky", "天空蓝", "#9DC4E0", "☁️"),
        ThemeConfig("sunshine", "暖阳橙", "#F5C5A3", "☀️"),
        ThemeConfig("sakura", "樱花粉", "#FFB6C1", "🌺")
    )

    private const val PREFS_NAME = "baby_growth_prefs"
    private const val KEY_THEME = "selected_theme"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedThemeKey(context: Context): String =
        getPrefs(context).getString(KEY_THEME, "coral") ?: "coral"

    fun setTheme(context: Context, themeKey: String) {
        getPrefs(context).edit().putString(KEY_THEME, themeKey).apply()
    }

    fun getThemeConfig(context: Context): ThemeConfig {
        val key = getSelectedThemeKey(context)
        return THEMES.find { it.key == key } ?: THEMES[0]
    }

    fun getThemeConfigByKey(key: String): ThemeConfig =
        THEMES.find { it.key == key } ?: THEMES[0]

    /**
     * Compose 可观察的主题 key，当 SharedPreferences 变化时自动更新
     */
    @Composable
    fun selectedThemeKey(context: Context): State<String> {
        val initialKey = getSelectedThemeKey(context)
        val state = remember { mutableStateOf(initialKey) }
        val prefs = getPrefs(context)
        remember {
            prefs.registerOnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_THEME) {
                    state.value = prefs.getString(KEY_THEME, "coral") ?: "coral"
                }
            }
        }
        return state
    }
}
