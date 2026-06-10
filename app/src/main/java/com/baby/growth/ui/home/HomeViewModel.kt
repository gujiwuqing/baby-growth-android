package com.baby.growth.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.BabyInfo
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.RecordTypes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class TodayStats(
    val feedCount: Int = 0,
    val totalMilk: Int = 0,
    val diaperCount: Int = 0,
    val sleepMinutes: Int = 0,
    val foodCount: Int = 0,
    val supplementCount: Int = 0
)

data class RecentRecord(
    val id: Long,
    val type: String,
    val title: String,
    val detail: String,
    val time: Long,
    val icon: String,
    val tableName: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _babyInfo = MutableStateFlow<BabyInfo?>(null)
    val babyInfo: StateFlow<BabyInfo?> = _babyInfo.asStateFlow()

    private val _todayStats = MutableStateFlow(TodayStats())
    val todayStats: StateFlow<TodayStats> = _todayStats.asStateFlow()

    private val _recentRecords = MutableStateFlow<List<RecentRecord>>(emptyList())
    val recentRecords: StateFlow<List<RecentRecord>> = _recentRecords.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            db.babyInfoDao().getBabyInfo().collect { info ->
                _babyInfo.value = info
            }
        }
        loadTodayStats()
        loadRecentRecords()
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

            val feedCount = db.feedDao().getCountByDateRange(startOfDay, endOfDay)
            val allFeeds = db.feedDao().getByDateRangeOnce(startOfDay, endOfDay)
            val totalMilk = allFeeds.filter { it.type != "breast" }.sumOf { it.amount }
            val diaperCount = db.diaperDao().getCountByDateRange(startOfDay, endOfDay)
            val sleepMinutes = db.sleepDao().getTotalDurationByDateRange(startOfDay, endOfDay) ?: 0
            val foodCount = db.foodDao().getCountByDateRange(startOfDay, endOfDay)
            val supplementCount = db.supplementDao().getCountByDateRange(startOfDay, endOfDay)

            _todayStats.value = TodayStats(
                feedCount = feedCount,
                totalMilk = totalMilk,
                diaperCount = diaperCount,
                sleepMinutes = sleepMinutes,
                foodCount = foodCount,
                supplementCount = supplementCount
            )
        }
    }

    private fun loadRecentRecords() {
        viewModelScope.launch {
            val records = mutableListOf<RecentRecord>()

            db.feedDao().getAll().first().forEach { feed ->
                val typeLabel = RecordTypes.getLabel(feed.type)
                val detail = if (feed.type == "breast") {
                    val parts = mutableListOf<String>()
                    if (feed.leftDuration > 0) parts.add("左侧${feed.leftDuration}min")
                    if (feed.rightDuration > 0) parts.add("右侧${feed.rightDuration}min")
                    val durationText = if (parts.isNotEmpty()) parts.joinToString("｜") else "0min"
                    "$durationText\n${DateUtils.formatRelativeTime(feed.recordTime)}"
                } else {
                    "${feed.amount}ml\n${DateUtils.formatRelativeTime(feed.recordTime)}"
                }
                records.add(
                    RecentRecord(
                        id = feed.id, type = feed.type, title = typeLabel,
                        detail = detail,
                        time = feed.recordTime, icon = RecordTypes.getIcon(feed.type),
                        tableName = "feeds"
                    )
                )
            }

            db.diaperDao().getAll().first().forEach { diaper ->
                val typeLabel = when (diaper.type) {
                    "pee" -> "小便💧"
                    "poo" -> "大便💩"
                    else -> "混合"
                }
                val detailParts = mutableListOf(typeLabel)
                if (diaper.hasRash == 1) detailParts.add("红屁屁")
                if (diaper.note.isNotEmpty()) detailParts.add(diaper.note)
                records.add(
                    RecentRecord(
                        id = diaper.id, type = "diaper", title = "换尿布",
                        detail = "${detailParts.joinToString(" · ")}\n${DateUtils.formatRelativeTime(diaper.recordTime)}",
                        time = diaper.recordTime, icon = "👶",
                        tableName = "diapers"
                    )
                )
            }

            db.sleepDao().getAll().first().forEach { sleep ->
                val timeRange = if (sleep.startTime > 0 && sleep.endTime > 0) {
                    "${DateUtils.formatTime(sleep.startTime)}-${DateUtils.formatTime(sleep.endTime)}"
                } else ""
                val detailText = buildString {
                    append(DateUtils.formatDuration(sleep.duration))
                    if (timeRange.isNotEmpty()) append("  $timeRange")
                    append("\n${DateUtils.formatRelativeTime(sleep.recordTime)}")
                }
                records.add(
                    RecentRecord(
                        id = sleep.id, type = "sleep", title = "睡眠",
                        detail = detailText,
                        time = sleep.recordTime, icon = "😴",
                        tableName = "sleeps"
                    )
                )
            }

            db.foodDao().getAll().first().forEach { food ->
                val detailText = buildString {
                    append("${food.foodName} ${food.amount}${food.unit}")
                    if (food.note.isNotEmpty()) append(" · ${food.note}")
                    append("\n${DateUtils.formatRelativeTime(food.recordTime)}")
                }
                records.add(
                    RecentRecord(
                        id = food.id, type = "food", title = "辅食",
                        detail = detailText,
                        time = food.recordTime, icon = "🥣",
                        tableName = "foods"
                    )
                )
            }

            db.supplementDao().getAll().first().forEach { supplement ->
                val nameLabel = when (supplement.supplementName) {
                    "AD" -> "维生素AD"
                    "D3" -> "维生素D3"
                    "DHA" -> "DHA"
                    "calcium" -> "钙"
                    "probiotic" -> "益生菌"
                    "iron" -> "铁"
                    "zinc" -> "锌"
                    else -> supplement.supplementName
                }
                val detailText = buildString {
                    append(nameLabel)
                    if (supplement.dosage.isNotEmpty()) append("，${supplement.dosage}")
                    append("\n${DateUtils.formatRelativeTime(supplement.recordTime)}")
                }
                records.add(
                    RecentRecord(
                        id = supplement.id, type = "supplement", title = "营养补剂",
                        detail = detailText,
                        time = supplement.recordTime, icon = "💊",
                        tableName = "supplements"
                    )
                )
            }

            _recentRecords.value = records.sortedByDescending { it.time }.take(10)
        }
    }

    fun deleteRecord(item: RecentRecord) {
        viewModelScope.launch {
            when (item.tableName) {
                "feeds" -> db.feedDao().deleteById(item.id)
                "diapers" -> db.diaperDao().deleteById(item.id)
                "sleeps" -> db.sleepDao().deleteById(item.id)
                "foods" -> db.foodDao().deleteById(item.id)
                "supplements" -> db.supplementDao().deleteById(item.id)
                "growth_records" -> db.growthDao().deleteById(item.id)
            }
            loadRecentRecords()
            loadTodayStats()
        }
    }
}
