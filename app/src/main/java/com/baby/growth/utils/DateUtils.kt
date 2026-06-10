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
        val birthCal = Calendar.getInstance().apply { timeInMillis = birthday }
        val nowCal = Calendar.getInstance()
        var months = (nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12 +
                (nowCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH))
        if (nowCal.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH)) {
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
}
