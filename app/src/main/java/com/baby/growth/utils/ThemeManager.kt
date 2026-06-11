package com.baby.growth.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_AUTO_DARK_MODE = "auto_dark_mode"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedThemeKey(context: Context): String =
        getPrefs(context).getString(KEY_THEME, "coral") ?: "coral"

    fun setTheme(context: Context, themeKey: String) {
        getPrefs(context).edit().putString(KEY_THEME, themeKey).apply()
    }

    fun isDarkMode(context: Context): Boolean {
        val manualSetting = getPrefs(context).getBoolean(KEY_DARK_MODE, false)
        val autoDark = getPrefs(context).getBoolean(KEY_AUTO_DARK_MODE, true)
        if (autoDark) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return hour >= 22 || hour < 8
        }
        return manualSetting
    }

    fun setDarkMode(context: Context, darkMode: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, darkMode).putBoolean(KEY_AUTO_DARK_MODE, false).apply()
    }

    fun isAutoDarkMode(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_DARK_MODE, true)

    fun setAutoDarkMode(context: Context, auto: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_DARK_MODE, auto).apply()
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
        val prefs = getPrefs(context)
        val state = remember { mutableStateOf(getSelectedThemeKey(context)) }
        DisposableEffect(prefs) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_THEME) {
                    state.value = prefs.getString(KEY_THEME, "coral") ?: "coral"
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        return state
    }

    /**
     * Compose 可观察的深色模式状态，支持自动夜间模式
     */
    @Composable
    fun darkModeState(context: Context): State<Boolean> {
        val prefs = getPrefs(context)
        val state = remember { mutableStateOf(isDarkMode(context)) }
        DisposableEffect(prefs) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_DARK_MODE || key == KEY_AUTO_DARK_MODE) {
                    state.value = isDarkMode(context)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        // 每分钟检查一次时间变化（自动夜间模式）
        DisposableEffect(Unit) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    state.value = isDarkMode(context)
                    handler.postDelayed(this, 60_000L)
                }
            }
            handler.postDelayed(runnable, 60_000L)
            onDispose { handler.removeCallbacks(runnable) }
        }
        return state
    }
}
