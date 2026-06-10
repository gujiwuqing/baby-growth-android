package com.baby.growth.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 睡眠计时器 - 持久化实现
 * 基于绝对时间戳计时，APP 切后台/切页面后恢复时自动计算经过时长
 */
object SleepTimer {

    data class TimerState(
        val isRunning: Boolean = false,
        val startTimestamp: Long = 0L,
        val accumulatedSeconds: Int = 0,
        val originalStartTime: Long = 0L  // 最初开始计时的时间戳，停止后仍保留
    )

    private const val PREFS_NAME = "sleep_timer"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_START_TIMESTAMP = "start_timestamp"
    private const val KEY_ACCUMULATED = "accumulated_seconds"
    private const val KEY_ORIGINAL_START = "original_start_time"

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 必须在 Application.onCreate 中调用，初始化持久化状态
     */
    fun initWithContext(context: Context) {
        val prefs = getPrefs(context)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val startTimestamp = prefs.getLong(KEY_START_TIMESTAMP, 0L)
        val accumulated = prefs.getInt(KEY_ACCUMULATED, 0)
        val originalStart = prefs.getLong(KEY_ORIGINAL_START, 0L)

        // 如果计时仍在运行，检查是否时间过长（超过12小时视为异常，自动停止）
        val state = if (isRunning && startTimestamp > 0) {
            val elapsedSeconds = ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
            if (elapsedSeconds > 12 * 3600) {
                TimerState(isRunning = false, startTimestamp = 0L,
                    accumulatedSeconds = accumulated + elapsedSeconds,
                    originalStartTime = originalStart)
            } else {
                TimerState(isRunning = true, startTimestamp = startTimestamp,
                    accumulatedSeconds = accumulated, originalStartTime = originalStart)
            }
        } else {
            TimerState(isRunning = false, startTimestamp = 0L,
                accumulatedSeconds = accumulated, originalStartTime = originalStart)
        }
        _state.value = state
    }

    /**
     * 开始计时
     */
    fun start(context: Context) {
        val current = _state.value
        if (current.isRunning) return

        val now = System.currentTimeMillis()
        // 如果已有积累时长（resume场景），保留originalStartTime；否则设为当前时间
        val original = if (current.accumulatedSeconds > 0 && current.originalStartTime > 0) {
            current.originalStartTime
        } else {
            now
        }
        val updated = TimerState(
            isRunning = true,
            startTimestamp = now,
            accumulatedSeconds = current.accumulatedSeconds,
            originalStartTime = original
        )
        _state.value = updated
        saveToDisk(context, updated)
    }

    /**
     * 停止计时，积累时长
     * 即使不在运行状态也安全调用（不会崩溃）
     */
    fun stop(context: Context) {
        val current = _state.value
        if (!current.isRunning) return

        val elapsedSeconds = ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt()
        val updated = TimerState(
            isRunning = false,
            startTimestamp = 0L,
            accumulatedSeconds = current.accumulatedSeconds + elapsedSeconds,
            originalStartTime = current.originalStartTime
        )
        _state.value = updated
        saveToDisk(context, updated)
    }

    /**
     * 清除所有计时数据
     */
    fun reset(context: Context) {
        val updated = TimerState()
        _state.value = updated
        saveToDisk(context, updated)
    }

    /**
     * 获取当前总秒数（含正在计时的）
     */
    fun getTotalSeconds(): Int {
        val current = _state.value
        val runningSeconds = if (current.isRunning && current.startTimestamp > 0) {
            ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt()
        } else 0
        return current.accumulatedSeconds + runningSeconds
    }

    /**
     * 获取计时开始的绝对时间戳（用于记录 startTime）
     */
    fun getStartTime(): Long {
        val current = _state.value
        return if (current.originalStartTime > 0) {
            current.originalStartTime
        } else if (current.isRunning && current.startTimestamp > 0) {
            current.startTimestamp
        } else {
            System.currentTimeMillis()  // fallback
        }
    }

    /**
     * 是否有有效的计时数据（至少1秒）
     */
    fun hasValidData(): Boolean {
        return getTotalSeconds() > 0
    }

    private fun saveToDisk(context: Context, state: TimerState) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_RUNNING, state.isRunning)
            putLong(KEY_START_TIMESTAMP, state.startTimestamp)
            putInt(KEY_ACCUMULATED, state.accumulatedSeconds)
            putLong(KEY_ORIGINAL_START, state.originalStartTime)
            apply()
        }
    }
}
