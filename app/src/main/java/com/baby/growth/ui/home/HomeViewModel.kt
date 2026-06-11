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

data class SmartTip(
    val emoji: String,
    val message: String
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

    private val _smartTips = MutableStateFlow<List<SmartTip>>(emptyList())
    val smartTips: StateFlow<List<SmartTip>> = _smartTips.asStateFlow()

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
        loadSmartTips()
    }

    private fun loadSmartTips() {
        viewModelScope.launch {
            val tips = mutableListOf<SmartTip>()
            val now = System.currentTimeMillis()

            // 检查距上次喂奶时间
            val lastFeed = db.feedDao().getLatest()
            if (lastFeed != null) {
                val minutesSinceLastFeed = (now - lastFeed.recordTime) / 60000
                if (minutesSinceLastFeed > 180) {
                    val hours = minutesSinceLastFeed / 60
                    tips.add(SmartTip("🍼", "距上次喂奶已过${hours}小时，注意宝宝是否饿了"))
                }
            } else {
                tips.add(SmartTip("🍼", "今天还没有喂奶记录哦"))
            }

            // 检查睡眠情况
            val lastSleep = db.sleepDao().getLatest()
            if (lastSleep != null) {
                val minutesSinceLastSleep = (now - lastSleep.recordTime) / 60000
                if (minutesSinceLastSleep > 240) {
                    tips.add(SmartTip("😴", "宝宝醒了很久了，注意观察困倦信号"))
                }
            }

            // 检查换尿布
            val lastDiaper = db.diaperDao().getLatest()
            if (lastDiaper != null) {
                val minutesSinceLastDiaper = (now - lastDiaper.recordTime) / 60000
                if (minutesSinceLastDiaper > 180) {
                    tips.add(SmartTip("👶", "超过3小时没换纸尿裤了，检查一下吧"))
                }
            }

            // 如果没有任何提示，给一个鼓励
            if (tips.isEmpty()) {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val greeting = when {
                    hour < 9 -> "早安！新的一天开始啦 ☀️"
                    hour < 12 -> "上午好！记录宝宝的精彩上午"
                    hour < 18 -> "下午好！宝宝今天表现棒棒哒"
                    else -> "晚上好！辛苦了，记得照顾好自己 🌙"
                }
                tips.add(SmartTip("💝", greeting))
            }

            _smartTips.value = tips
        }
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
                        id = diaper.id, type = "diaper", title = "换纸尿裤",
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
