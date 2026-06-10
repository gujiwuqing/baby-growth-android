package com.baby.growth.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 母乳喂养计时器 - 持久化实现
 * 基于绝对时间戳计时，APP 切后台/切页面后恢复时自动计算经过时长
 * 只有进程被杀或手动停止才会清除计时
 */
object BreastfeedingTimer {

    data class TimerState(
        val isRunning: Boolean = false,
        val side: String = "left",       // "left" 或 "right"
        val startTimestamp: Long = 0L,    // 计时开始的绝对时间戳
        val leftAccumulatedSeconds: Int = 0, // 左侧已积累秒数（不含当前计时段）
        val rightAccumulatedSeconds: Int = 0 // 右侧已积累秒数（不含当前计时段）
    )

    private const val PREFS_NAME = "breastfeeding_timer"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_SIDE = "side"
    private const val KEY_START_TIMESTAMP = "start_timestamp"
    private const val KEY_LEFT_ACCUMULATED = "left_accumulated_seconds"
    private const val KEY_RIGHT_ACCUMULATED = "right_accumulated_seconds"

    private val _state = MutableStateFlow(loadStateFromDisk())
    val state: StateFlow<TimerState> = _state

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadStateFromDisk(): TimerState {
        // 临时加载，需要 context 才能真正读取
        // 初始默认值，真正的加载在 initWithContext 中完成
        return TimerState()
    }

    /**
     * 必须在 Application.onCreate 中调用，初始化持久化状态
     */
    fun initWithContext(context: Context) {
        val prefs = getPrefs(context)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val side = prefs.getString(KEY_SIDE, "left") ?: "left"
        val startTimestamp = prefs.getLong(KEY_START_TIMESTAMP, 0L)
        val leftAccumulated = prefs.getInt(KEY_LEFT_ACCUMULATED, 0)
        val rightAccumulated = prefs.getInt(KEY_RIGHT_ACCUMULATED, 0)

        // 如果计时仍在运行，检查是否时间过长（超过4小时视为异常，自动停止）
        val state = if (isRunning && startTimestamp > 0) {
            val elapsedSeconds = ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
            if (elapsedSeconds > 4 * 3600) {
                // 超过4小时，视为异常，停止计时
                TimerState(isRunning = false, side = side, startTimestamp = 0L,
                    leftAccumulatedSeconds = leftAccumulated, rightAccumulatedSeconds = rightAccumulated)
            } else {
                TimerState(isRunning = true, side = side, startTimestamp = startTimestamp,
                    leftAccumulatedSeconds = leftAccumulated, rightAccumulatedSeconds = rightAccumulated)
            }
        } else {
            TimerState(isRunning = false, side = side, startTimestamp = 0L,
                leftAccumulatedSeconds = leftAccumulated, rightAccumulatedSeconds = rightAccumulated)
        }
        _state.value = state
    }

    /**
     * 开始计时（左侧或右侧）
     */
    fun start(context: Context, side: String) {
        val current = _state.value
        // 如果已在计时同一侧，不做操作
        if (current.isRunning && current.side == side) return

        // 如果在计时另一侧，先停止当前侧，积累时长
        val updated = if (current.isRunning) {
            val elapsedSeconds = ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt()
            val newLeft = if (current.side == "left") current.leftAccumulatedSeconds + elapsedSeconds else current.leftAccumulatedSeconds
            val newRight = if (current.side == "right") current.rightAccumulatedSeconds + elapsedSeconds else current.rightAccumulatedSeconds
            TimerState(isRunning = true, side = side, startTimestamp = System.currentTimeMillis(),
                leftAccumulatedSeconds = newLeft, rightAccumulatedSeconds = newRight)
        } else {
            TimerState(isRunning = true, side = side, startTimestamp = System.currentTimeMillis(),
                leftAccumulatedSeconds = current.leftAccumulatedSeconds, rightAccumulatedSeconds = current.rightAccumulatedSeconds)
        }

        _state.value = updated
        saveToDisk(context, updated)
    }

    /**
     * 停止当前侧计时，积累时长
     */
    fun stop(context: Context) {
        val current = _state.value
        if (!current.isRunning) return

        val elapsedSeconds = ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt()
        val newLeft = if (current.side == "left") current.leftAccumulatedSeconds + elapsedSeconds else current.leftAccumulatedSeconds
        val newRight = if (current.side == "right") current.rightAccumulatedSeconds + elapsedSeconds else current.rightAccumulatedSeconds

        val updated = TimerState(isRunning = false, side = current.side, startTimestamp = 0L,
            leftAccumulatedSeconds = newLeft, rightAccumulatedSeconds = newRight)
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
     * 获取当前左侧总秒数（含正在计时的）
     */
    fun getLeftTotalSeconds(): Int {
        val current = _state.value
        val runningSeconds = if (current.isRunning && current.side == "left") {
            ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt()
        } else 0
        return current.leftAccumulatedSeconds + runningSeconds
    }

    /**
     * 获取当前右侧总秒数（含正在计时的）
     */
    fun getRightTotalSeconds(): Int {
        val current = _state.value
        val runningSeconds = if (current.isRunning && current.side == "right") {
            ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt()
        } else 0
        return current.rightAccumulatedSeconds + runningSeconds
    }

    private fun saveToDisk(context: Context, state: TimerState) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_RUNNING, state.isRunning)
            putString(KEY_SIDE, state.side)
            putLong(KEY_START_TIMESTAMP, state.startTimestamp)
            putInt(KEY_LEFT_ACCUMULATED, state.leftAccumulatedSeconds)
            putInt(KEY_RIGHT_ACCUMULATED, state.rightAccumulatedSeconds)
            apply()
        }
    }
}