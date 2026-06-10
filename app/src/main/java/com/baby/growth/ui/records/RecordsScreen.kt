package com.baby.growth.ui.records

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.*
import com.baby.growth.ui.theme.*
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
    val supplementCount: Int = 0
) {
    val totalCount: Int get() = feedCount + diaperCount + foodCount + supplementCount
}

/** 周视图每天的记录条数 */
data class WeekDayStat(
    val dayLabel: String,  // 一、二、三...
    val count: Int
)

/** 月视图某天是否有记录 */
data class MonthDayStat(
    val day: Int,
    val timestamp: Long,
    val count: Int,
    val isCurrentMonth: Boolean
)

data class TimelineItem(
    val id: Long,
    val type: String,
    val title: String,
    val detail: String,
    val time: Long,
    val tableName: String
)

class RecordsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _currentDate = MutableStateFlow(System.currentTimeMillis())
    val currentDate = _currentDate.asStateFlow()

    private val _dimension = MutableStateFlow("day") // day, week, month
    val dimension = _dimension.asStateFlow()

    private val _activeTab = MutableStateFlow("all")
    val activeTab = _activeTab.asStateFlow()

    private val _summary = MutableStateFlow(DaySummary())
    val summary = _summary.asStateFlow()

    private val _timelineItems = MutableStateFlow<List<TimelineItem>>(emptyList())
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

            val items = mutableListOf<TimelineItem>()
            feeds.forEach { r ->
                val detail = if (r.type == "breast") {
                    val parts = mutableListOf<String>()
                    if (r.leftDuration > 0) parts.add("左侧${r.leftDuration}min")
                    if (r.rightDuration > 0) parts.add("右侧${r.rightDuration}min")
                    val durationText = if (parts.isNotEmpty()) parts.joinToString("｜") else "0min"
                    "$durationText\n${DateUtils.formatRelativeTime(r.recordTime)}"
                } else {
                    "${r.amount}ml\n${DateUtils.formatRelativeTime(r.recordTime)}"
                }
                items.add(TimelineItem(r.id, r.type, RecordTypes.getLabel(r.type),
                    detail, r.recordTime, "feeds"))
            }
            diapers.forEach { r ->
                val typeLabel = when(r.type) { "pee" -> "小便💧"; "poo" -> "大便💩"; else -> "混合" }
                val detailParts = mutableListOf(typeLabel)
                if (r.hasRash == 1) detailParts.add("红屁屁")
                if (r.note.isNotEmpty()) detailParts.add(r.note)
                items.add(TimelineItem(r.id, "diaper", "换尿布",
                    "${detailParts.joinToString(" · ")}\n${DateUtils.formatRelativeTime(r.recordTime)}",
                    r.recordTime, "diapers"))
            }
            sleeps.forEach { r ->
                val timeRange = if (r.startTime > 0 && r.endTime > 0) {
                    "${DateUtils.formatTime(r.startTime)}-${DateUtils.formatTime(r.endTime)}"
                } else ""
                val detailText = buildString {
                    append(DateUtils.formatDuration(r.duration))
                    if (timeRange.isNotEmpty()) append("  $timeRange")
                    append("\n${DateUtils.formatRelativeTime(r.recordTime)}")
                }
                items.add(TimelineItem(r.id, "sleep", "睡眠",
                    detailText, r.recordTime, "sleeps"))
            }
            foods.forEach { r ->
                val detailText = buildString {
                    append("${r.foodName} ${r.amount}${r.unit}")
                    if (r.note.isNotEmpty()) append(" · ${r.note}")
                    append("\n${DateUtils.formatRelativeTime(r.recordTime)}")
                }
                items.add(TimelineItem(r.id, "food", "辅食",
                    detailText, r.recordTime, "foods"))
            }
            supplements.forEach { r ->
                val nameLabel = when (r.supplementName) {
                    "AD" -> "维生素AD"; "D3" -> "维生素D3"; "DHA" -> "DHA"
                    "calcium" -> "钙"; "probiotic" -> "益生菌"; "iron" -> "铁"; "zinc" -> "锌"
                    else -> r.supplementName
                }
                val detailText = buildString {
                    append(nameLabel)
                    if (r.dosage.isNotEmpty()) append("，${r.dosage}")
                    append("\n${DateUtils.formatRelativeTime(r.recordTime)}")
                }
                items.add(TimelineItem(r.id, "supplement", "营养补剂",
                    detailText, r.recordTime, "supplements"))
            }

            val tab = _activeTab.value
            val filtered = if (tab == "all") items else items.filter {
                when (tab) {
                    "feeding" -> it.type in listOf("breast", "formula", "bottle")
                    "sleep" -> it.type == "sleep"
                    "diaper" -> it.type == "diaper"
                    "food" -> it.type == "food"
                    "supplement" -> it.type == "supplement"
                    "growth" -> it.type == "growth"
                    else -> true
                }
            }
            _timelineItems.value = filtered.sortedByDescending { it.time }

            // 周视图：计算每天的记录条数
            if (_dimension.value == "week") {
                val weekDays = DateUtils.getWeekDays(_currentDate.value)
                val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                _weekDayStats.value = weekDays.mapIndexed { index, dayTs ->
                    val (dayStart, dayEnd) = DateUtils.getDayRange(
                        Calendar.getInstance().apply { timeInMillis = dayTs }.get(Calendar.YEAR),
                        Calendar.getInstance().apply { timeInMillis = dayTs }.get(Calendar.MONTH),
                        Calendar.getInstance().apply { timeInMillis = dayTs }.get(Calendar.DAY_OF_MONTH)
                    )
                    val dayCount = items.count { it.time in dayStart..dayEnd }
                    WeekDayStat(dayLabels[index], dayCount)
                }
            }

            // 月视图：计算每天的记录条数
            if (_dimension.value == "month") {
                val calendarDays = DateUtils.getMonthCalendarDays(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)
                )
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

    fun deleteItem(item: TimelineItem) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    navController: NavController,
    viewModel: RecordsViewModel = viewModel()
) {
    val currentDate by viewModel.currentDate.collectAsState()
    val dimension by viewModel.dimension.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val timelineItems by viewModel.timelineItems.collectAsState()
    val babyInfo by viewModel.babyInfo.collectAsState()
    val weekDayStats by viewModel.weekDayStats.collectAsState()
    val monthDayStats by viewModel.monthDayStats.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<TimelineItem?>(null) }

    val dimensionLabels = listOf("日" to "day", "周" to "week", "月" to "month")
    val tabLabels = listOf("全部" to "all", "喂养" to "feeding", "睡眠" to "sleep",
        "换尿布" to "diaper", "辅食" to "food", "补剂" to "supplement")

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    val dateLabel = remember(currentDate, dimension) {
        when (dimension) {
            "day" -> DateUtils.formatDayTitle(currentDate)
            "week" -> DateUtils.formatWeekTitle(currentDate)
            "month" -> DateUtils.formatMonth(currentDate)
            else -> DateUtils.formatDate(currentDate)
        }
    }

    val summaryTitle = remember(dimension) {
        when (dimension) {
            "day" -> "今日汇总"
            "week" -> "本周汇总"
            "month" -> "本月汇总"
            else -> "汇总"
        }
    }

    val timelineTitle = remember(dimension) {
        when (dimension) {
            "day" -> "当天全部"
            "week" -> "本周全部"
            "month" -> "本月全部"
            else -> "全部"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "记录总览",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== 维度切换 Tab =====
            item {
                DimensionTabBar(dimension, dimensionLabels, primaryColor) {
                    viewModel.setDimension(it)
                }
            }

            // ===== 日期导航 =====
            item {
                DateNavigator(dateLabel) { offset -> viewModel.navigateDate(offset) }
            }

            // ===== 类型筛选横向滚动 =====
            item {
                CategoryFilterRow(activeTab, tabLabels, primaryContainerColor) {
                    viewModel.setActiveTab(it)
                }
            }

            // ===== 月视图：日历组件 =====
            if (dimension == "month") {
                item {
                    MonthCalendarCard(currentDate, monthDayStats, primaryColor)
                }
            }

            // ===== 汇总统计卡片 =====
            item {
                SummaryCard(summaryTitle, summary, dimension, primaryContainerColor)
            }

            // ===== 睡眠建议 =====
            if (dimension == "day" && summary.sleepMinutes > 0 && babyInfo != null) {
                item {
                    val monthAge = DateUtils.getMonthAge(babyInfo!!.birthday)
                    val hours = summary.sleepMinutes / 60f
                    val advice = com.baby.growth.utils.SleepAdvice.evaluateSleep(monthAge, hours)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Text("💤 $advice", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                    }
                }
            }

            // ===== 周视图：每日记录数柱状图 =====
            if (dimension == "week") {
                item {
                    WeeklyBarChartCard(weekDayStats, primaryColor)
                }
            }

            // ===== 时间轴列表 =====
            item {
                TimelineCard(timelineTitle, timelineItems, dimension) { item ->
                    showDeleteDialog = item
                }
            }

            if (timelineItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📝", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("暂无记录", color = Color.Gray, fontSize = 14.sp)
                            Text("快来记录宝宝的第一次吧~", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除记录") },
            text = { Text("确定删除这条${item.title}记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item)
                    showDeleteDialog = null
                }) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}

// ==================== 子组件 ====================

/** 维度切换 Tab（日/周/月） */
@Composable
private fun DimensionTabBar(
    selected: String,
    labels: List<Pair<String, String>>,
    selectedColor: Color,
    onSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { (label, key) ->
                val isSelected = selected == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (isSelected) selectedColor else Color.Transparent)
                        .clickable { onSelect(key) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/** 日期导航（左右箭头 + 日期文字） */
@Composable
private fun DateNavigator(dateLabel: String, onNavigate: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onNavigate(-1) }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "前",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
            )
        }
        Text(
            dateLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = { onNavigate(1) }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward, "后",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 类型筛选横向滚动条 */
@Composable
private fun CategoryFilterRow(
    activeTab: String,
    tabLabels: List<Pair<String, String>>,
    selectedColor: Color,
    onSelect: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabLabels) { (label, key) ->
            val isSelected = activeTab == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

/** 汇总统计卡片 */
@Composable
private fun SummaryCard(
    title: String,
    summary: DaySummary,
    dimension: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem("${summary.totalCount}", "记录总数", accentColor)
                SummaryStatItem("${summary.feedCount}", "喂奶次数", accentColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val avgLabel = when (dimension) {
                "day" -> "日均"
                "week" -> "日均"
                "month" -> "日均"
                else -> "日均"
            }
            val avgMilk = if (summary.feedCount > 0) summary.totalMilk / summary.feedCount else 0
            Text(
                "${avgLabel} ${summary.totalCount} 条记录 · 均奶量${avgMilk}ml/次",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 周视图柱状图卡片 */
@Composable
private fun WeeklyBarChartCard(stats: List<WeekDayStat>, barColor: Color) {
    if (stats.isEmpty()) return
    val maxCount = stats.maxOf { it.count }.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("每日记录数", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 数字标签
                        Text(
                            "${stat.count}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // 柱状条
                        val barHeight = if (stat.count > 0) {
                            (stat.count.toFloat() / maxCount * 80).dp
                        } else {
                            4.dp
                        }
                        val barAlpha = if (stat.count > 0) 1f else 0.3f
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor.copy(alpha = barAlpha))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 星期标签
                        Text(stat.dayLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** 月视图日历卡片 */
@Composable
private fun MonthCalendarCard(
    currentDate: Long,
    monthDayStats: List<MonthDayStat>,
    accentColor: Color
) {
    val cal = Calendar.getInstance().apply { timeInMillis = currentDate }
    val todayTimestamp = System.currentTimeMillis()
    val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 月份标题
            Text(
                "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 星期表头
            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach { header ->
                    Text(
                        header,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 日历格子
            if (monthDayStats.isNotEmpty()) {
                val rows = monthDayStats.chunked(7)
                rows.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        week.forEach { dayStat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                val isToday = dayStat.isCurrentMonth &&
                                        DateUtils.isSameDay(dayStat.timestamp, todayTimestamp)

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .then(
                                                if (isToday) Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(accentColor.copy(alpha = 0.15f))
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${dayStat.day}",
                                            fontSize = 14.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isToday -> accentColor
                                                dayStat.isCurrentMonth -> MaterialTheme.colorScheme.onBackground
                                                else -> MaterialTheme.colorScheme.outline
                                            }
                                        )
                                    }
                                    // 有记录的标记
                                    if (dayStat.count > 0 && dayStat.isCurrentMonth) {
                                        Text(
                                            "🍼${dayStat.count}条",
                                            fontSize = 9.sp,
                                            color = accentColor,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                        // 补齐不满7个的行
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** 时间轴列表卡片 */
@Composable
private fun TimelineCard(
    title: String,
    timelineItems: List<TimelineItem>,
    dimension: String,
    onItemClick: (TimelineItem) -> Unit
) {
    if (timelineItems.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "共 ${timelineItems.size} 条",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            timelineItems.forEachIndexed { index, item ->
                val dotColor = when (item.type) {
                    "breast", "formula", "bottle" -> Color(0xFFFF8A9B)
                    "sleep" -> Color(0xFF7986CB)
                    "diaper" -> Color(0xFFFFB74D)
                    "food" -> Color(0xFF66BB6A)
                    "supplement" -> Color(0xFF42A5F5)
                    "growth" -> Color(0xFFEC407A)
                    else -> Color(0xFFBDBDBD)
                }
                val isLast = index == timelineItems.lastIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧时间
                    Column(
                        modifier = Modifier.width(44.dp).padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (dimension != "day") {
                            Text(
                                text = DateUtils.formatShortDate(item.time),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = DateUtils.formatTime(item.time),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    // 时间线圆点
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(dotColor)
                        )
                        if (!isLast) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                        }
                    }

                    // 右侧卡片内容
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp, bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(RecordTypes.getIcon(item.type), fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (item.detail.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            item.detail.split("\n").first(),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                // 周/月视图显示日期
                                if (dimension != "day") {
                                    Text(
                                        DateUtils.formatShortDate(item.time),
                                        fontSize = 11.sp,
                                        color = TextHint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
