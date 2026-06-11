package com.baby.growth.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期时间工具类
 */
object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy年M月", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatMonth(timestamp: Long): String = monthFormat.format(Date(timestamp))

    /** 获取当天0点的时间戳 */
    fun getDayStart(calendar: Calendar = Calendar.getInstance()): Long {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /** 获取当天23:59:59的时间戳 */
    fun getDayEnd(calendar: Calendar = Calendar.getInstance()): Long {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /** 获取某天的起止时间戳 */
    fun getDayRange(year: Int, month: Int, day: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }

    /** 获取本周(周一到周日)的起止时间戳 */
    fun getWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = getDayStart(cal)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = getDayEnd(cal)
        return start to end
    }

    /** 获取某月的起止时间戳 */
    fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }

    /** 计算月龄 */
    fun getMonthAge(birthday: Long): Int {
        return getMonthAge(birthday, System.currentTimeMillis())
    }

    /** 计算指定时间点的月龄 */
    fun getMonthAge(birthday: Long, atTime: Long): Int {
        val birthCal = Calendar.getInstance().apply { timeInMillis = birthday }
        val targetCal = Calendar.getInstance().apply { timeInMillis = atTime }
        var months = (targetCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12 +
                (targetCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH))
        if (targetCal.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH)) {
            months--
        }
        return months.coerceAtLeast(0)
    }

    /** 计算出生天数 */
    fun getDayAge(birthday: Long): Int {
        val birthDay = getDayStart(Calendar.getInstance().apply { timeInMillis = birthday })
        val today = getDayStart()
        return ((today - birthDay) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }

    /** 生成唯一ID */
    fun generateUniqueId(type: String): String {
        return "${System.currentTimeMillis()}_${type}_${UUID.randomUUID().toString().take(8)}"
    }

    /** 精确计算生日+N个月后的日期 */
    fun addMonthsToBirthday(birthday: Long, months: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = birthday }
        val birthDay = cal.get(Calendar.DAY_OF_MONTH)
        cal.add(Calendar.MONTH, months)
        // 处理月末边界：如果目标月份没有该日期，则取该月最后一天
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (birthDay > maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
        }
        return cal.timeInMillis
    }

    /** 获取星期几 */
    fun getWeekDayName(timestamp: Long): String {
        val dayOfWeek = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }
    }

    /** 格式化时长(分钟)为可读文本 */
    fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}分钟"
            minutes % 60 == 0 -> "${minutes / 60}小时"
            else -> "${minutes / 60}小时${minutes % 60}分钟"
        }
    }

    /** 格式化时长(毫秒)为可读文本 */
    fun formatDurationMillis(millis: Long): String {
        val minutes = (millis / 60000).toInt()
        return formatDuration(minutes)
    }

    /** 相对时间：X分钟前 / X小时X分钟前 / X天前 */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = (diff / 60000).toInt()
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> {
                val remainMin = minutes % 60
                if (remainMin == 0) "${hours}小时前"
                else "${hours}小时${remainMin}分钟前"
            }
            days < 7 -> "${days}天前"
            else -> formatDate(timestamp)
        }
    }

    /** 基于指定日期获取所在周(周一到周日)的起止时间戳 */
    fun getWeekRangeForDate(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = getDayStart(cal)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = getDayEnd(cal)
        return start to end
    }

    /** 格式化短日期，如 "06.08" */
    private val shortDateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
    fun formatShortDate(timestamp: Long): String = shortDateFormat.format(Date(timestamp))

    /** 格式化日视图日期标题："今天 · 2026年6月10日" */
    fun formatDayTitle(timestamp: Long): String {
        val todayCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val prefix = when {
            todayCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            todayCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR) -> "今天"
            else -> {
                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                if (yesterdayCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    yesterdayCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR))
                    "昨天" else null
            }
        }
        val fullDate = SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(timestamp))
        return if (prefix != null) "$prefix · $fullDate" else fullDate
    }

    /** 格式化周视图日期标题："06.08 - 06.14" */
    fun formatWeekTitle(timestamp: Long): String {
        val (start, end) = getWeekRangeForDate(timestamp)
        return "${formatShortDate(start)} - ${formatShortDate(end)}"
    }

    /** 获取指定周每天的时间戳列表(周一到周日) */
    fun getWeekDays(timestamp: Long): List<Long> {
        val (start, _) = getWeekRangeForDate(timestamp)
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        return (0..6).map {
            val dayTimestamp = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            dayTimestamp
        }
    }

    /** 获取月视图的日历数据：包含上月尾部 + 当月 + 下月头部，共6行7列 */
    fun getMonthCalendarDays(year: Int, month: Int): List<CalendarDay> {
        val result = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 本月1号是周几（转为周一=0的索引）
        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 上月尾部
        if (firstDayOfWeek > 0) {
            val prevCal = Calendar.getInstance().apply {
                set(year, month, 1)
                add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
            }
            for (i in 0 until firstDayOfWeek) {
                result.add(CalendarDay(prevCal.get(Calendar.DAY_OF_MONTH), prevCal.timeInMillis, isCurrentMonth = false))
                prevCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // 当月
        val monthCal = Calendar.getInstance().apply { set(year, month, 1, 0, 0, 0) }
        for (day in 1..daysInMonth) {
            monthCal.set(Calendar.DAY_OF_MONTH, day)
            result.add(CalendarDay(day, monthCal.timeInMillis, isCurrentMonth = true))
        }

        // 下月头部，补齐到完整行(7的倍数)
        val remaining = (7 - result.size % 7) % 7
        val nextCal = Calendar.getInstance().apply { set(year, month + 1, 1, 0, 0, 0) }
        for (i in 0 until remaining) {
            result.add(CalendarDay(nextCal.get(Calendar.DAY_OF_MONTH), nextCal.timeInMillis, isCurrentMonth = false))
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return result
    }

    /** 判断两个时间戳是否是同一天 */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

data class CalendarDay(
    val day: Int,
    val timestamp: Long,
    val isCurrentMonth: Boolean
)
