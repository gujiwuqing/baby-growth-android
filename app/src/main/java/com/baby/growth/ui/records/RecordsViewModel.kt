package com.baby.growth.ui.records

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.*
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.RecordTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class DaySummary(
    val feedCount: Int = 0,
    val totalMilk: Int = 0,
    val diaperCount: Int = 0,
    val sleepMinutes: Int = 0,
    val foodCount: Int = 0,
    val supplementCount: Int = 0,
) {
    val totalCount: Int get() = feedCount + diaperCount + foodCount + supplementCount
}

data class WeekDayStat(
    val dayLabel: String,
    val count: Int,
)

data class MonthDayStat(
    val day: Int,
    val timestamp: Long,
    val count: Int,
    val isCurrentMonth: Boolean,
)

data class TimelineRecord(
    val id: Long,
    val type: String,
    val title: String,
    val detail: String,
    val time: Long,
    val tableName: String,
)

class RecordsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _currentDate = MutableStateFlow(System.currentTimeMillis())
    val currentDate = _currentDate.asStateFlow()

    private val _dimension = MutableStateFlow("day")
    val dimension = _dimension.asStateFlow()

    private val _activeTab = MutableStateFlow("all")
    val activeTab = _activeTab.asStateFlow()

    private val _summary = MutableStateFlow(DaySummary())
    val summary = _summary.asStateFlow()

    private val _timelineItems = MutableStateFlow<List<TimelineRecord>>(emptyList())
    val timelineItems = _timelineItems.asStateFlow()

    private val _babyInfo = MutableStateFlow<BabyInfo?>(null)
    val babyInfo = _babyInfo.asStateFlow()

    private val _weekDayStats = MutableStateFlow<List<WeekDayStat>>(emptyList())
    val weekDayStats = _weekDayStats.asStateFlow()

    private val _monthDayStats = MutableStateFlow<List<MonthDayStat>>(emptyList())
    val monthDayStats = _monthDayStats.asStateFlow()

    init {
        viewModelScope.launch {
            db.babyInfoDao().getBabyInfo().collect { _babyInfo.value = it }
        }
        loadData()
    }

    fun setDimension(dim: String) {
        _dimension.value = dim
        loadData()
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
        loadData()
    }

    fun navigateDate(offset: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = _currentDate.value }
        when (_dimension.value) {
            "day" -> cal.add(Calendar.DAY_OF_MONTH, offset)
            "week" -> cal.add(Calendar.WEEK_OF_YEAR, offset)
            "month" -> cal.add(Calendar.MONTH, offset)
        }
        _currentDate.value = cal.timeInMillis
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = _currentDate.value }
            val (start, end) = when (_dimension.value) {
                "day" -> DateUtils.getDayRange(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                )
                "week" -> DateUtils.getWeekRangeForDate(_currentDate.value)
                "month" -> DateUtils.getMonthRange(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)
                )
                else -> DateUtils.getDayRange(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                )
            }

            val feeds = db.feedDao().getByDateRangeOnce(start, end)
            val diapers = db.diaperDao().getByDateRangeOnce(start, end)
            val sleeps = db.sleepDao().getByDateRangeOnce(start, end)
            val foods = db.foodDao().getByDateRangeOnce(start, end)
            val supplements = db.supplementDao().getByDateRangeOnce(start, end)

            _summary.value = DaySummary(
                feedCount = feeds.size,
                totalMilk = feeds.filter { it.type != "breast" }.sumOf { it.amount },
                diaperCount = diapers.size,
                sleepMinutes = sleeps.sumOf { it.duration },
                foodCount = foods.size,
                supplementCount = supplements.size
            )

            val items = mutableListOf<TimelineRecord>()
            feeds.forEach { record ->
                val detail = if (record.type == "breast") {
                    val parts = mutableListOf<String>()
                    if (record.leftDuration > 0) parts.add("左侧${record.leftDuration}min")
                    if (record.rightDuration > 0) parts.add("右侧${record.rightDuration}min")
                    if (parts.isNotEmpty()) parts.joinToString("｜") else "0min"
                } else {
                    "${record.amount}ml"
                }
                items.add(TimelineRecord(record.id, record.type, RecordTypes.getLabel(record.type), detail, record.recordTime, "feeds"))
            }
            diapers.forEach { record ->
                val typeLabel = when (record.type) { "pee" -> "小便"; "poo" -> "大便"; else -> "混合" }
                val detailParts = mutableListOf(typeLabel)
                if (record.hasRash == 1) detailParts.add("红屁屁")
                if (record.note.isNotEmpty()) detailParts.add(record.note)
                items.add(TimelineRecord(record.id, "diaper", "换尿布", detailParts.joinToString(" · "), record.recordTime, "diapers"))
            }
            sleeps.forEach { record ->
                val timeRange = if (record.startTime > 0 && record.endTime > 0) {
                    "${DateUtils.formatTime(record.startTime)}-${DateUtils.formatTime(record.endTime)}"
                } else ""
                val detailText = buildString {
                    append(DateUtils.formatDuration(record.duration))
                    if (timeRange.isNotEmpty()) append("  $timeRange")
                }
                items.add(TimelineRecord(record.id, "sleep", "睡眠", detailText, record.recordTime, "sleeps"))
            }
            foods.forEach { record ->
                val detailText = buildString {
                    append("${record.foodName} ${record.amount}${record.unit}")
                    if (record.note.isNotEmpty()) append(" · ${record.note}")
                }
                items.add(TimelineRecord(record.id, "food", "辅食", detailText, record.recordTime, "foods"))
            }
            supplements.forEach { record ->
                val nameLabel = when (record.supplementName) {
                    "AD" -> "维生素AD"; "D3" -> "维生素D3"; "DHA" -> "DHA"
                    "calcium" -> "钙"; "probiotic" -> "益生菌"; "iron" -> "铁"; "zinc" -> "锌"
                    else -> record.supplementName
                }
                val detailText = buildString {
                    append(nameLabel)
                    if (record.dosage.isNotEmpty()) append("，${record.dosage}")
                }
                items.add(TimelineRecord(record.id, "supplement", "营养补剂", detailText, record.recordTime, "supplements"))
            }

            val tab = _activeTab.value
            val filtered = if (tab == "all") items else items.filter {
                when (tab) {
                    "feeding" -> it.type in listOf("breast", "formula", "bottle")
                    "sleep" -> it.type == "sleep"
                    "diaper" -> it.type == "diaper"
                    "food" -> it.type == "food"
                    "supplement" -> it.type == "supplement"
                    else -> true
                }
            }
            _timelineItems.value = filtered.sortedByDescending { it.time }

            if (_dimension.value == "week") {
                val weekDays = DateUtils.getWeekDays(_currentDate.value)
                val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                _weekDayStats.value = weekDays.mapIndexed { index, dayTs ->
                    val dayCal = Calendar.getInstance().apply { timeInMillis = dayTs }
                    val (dayStart, dayEnd) = DateUtils.getDayRange(
                        dayCal.get(Calendar.YEAR), dayCal.get(Calendar.MONTH), dayCal.get(Calendar.DAY_OF_MONTH)
                    )
                    val dayCount = items.count { it.time in dayStart..dayEnd }
                    WeekDayStat(dayLabels[index], dayCount)
                }
            }

            if (_dimension.value == "month") {
                val calendarDays = DateUtils.getMonthCalendarDays(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                _monthDayStats.value = calendarDays.map { calDay ->
                    val dayCal = Calendar.getInstance().apply { timeInMillis = calDay.timestamp }
                    val (dayStart, dayEnd) = DateUtils.getDayRange(
                        dayCal.get(Calendar.YEAR), dayCal.get(Calendar.MONTH), dayCal.get(Calendar.DAY_OF_MONTH)
                    )
                    val dayCount = if (calDay.isCurrentMonth) items.count { it.time in dayStart..dayEnd } else 0
                    MonthDayStat(calDay.day, calDay.timestamp, dayCount, calDay.isCurrentMonth)
                }
            }
        }
    }

    fun deleteItem(item: TimelineRecord) {
        viewModelScope.launch {
            when (item.tableName) {
                "feeds" -> db.feedDao().deleteById(item.id)
                "diapers" -> db.diaperDao().deleteById(item.id)
                "sleeps" -> db.sleepDao().deleteById(item.id)
                "foods" -> db.foodDao().deleteById(item.id)
                "supplements" -> db.supplementDao().deleteById(item.id)
                "growth_records" -> db.growthDao().deleteById(item.id)
            }
            loadData()
        }
    }
}
