package com.baby.growth.ui.records

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.ui.components.*
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.RecordTypes
import java.text.SimpleDateFormat
import java.util.*

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

    var showDeleteDialog by remember { mutableStateOf<TimelineRecord?>(null) }

    val dimensionLabels = listOf("日" to "day", "周" to "week", "月" to "month")
    val tabLabels = listOf(
        "全部" to "all", "喂养" to "feeding", "睡眠" to "sleep",
        "换纸尿裤" to "diaper", "辅食" to "food", "补剂" to "supplement"
    )

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
            "day" -> "今日汇总"; "week" -> "本周汇总"; "month" -> "本月汇总"; else -> "汇总"
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除", fontWeight = FontWeight.SemiBold) },
            text = { Text("确定删除这条${item.title}记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item)
                    showDeleteDialog = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            },
            shape = RoundedCornerShape(Radius.xl),
        )
    }

    Scaffold(
        topBar = {
            BabyTopBar(
                title = "记录总览",
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // 维度切换
            item { DimensionSelector(dimension, dimensionLabels) { viewModel.setDimension(it) } }

            // 日期导航
            item { DateNavigator(dateLabel) { viewModel.navigateDate(it) } }

            // 类型筛选
            item { CategoryFilterRow(activeTab, tabLabels) { viewModel.setActiveTab(it) } }

            // 月视图日历
            if (dimension == "month") {
                item { MonthCalendarCard(currentDate, monthDayStats) }
            }

            // 汇总统计
            item { SummaryStatsCard(summaryTitle, summary) }

            // 睡眠建议
            if (dimension == "day" && summary.sleepMinutes > 0 && babyInfo != null) {
                item {
                    val monthAge = DateUtils.getMonthAge(babyInfo!!.birthday)
                    val hours = summary.sleepMinutes / 60f
                    val advice = com.baby.growth.utils.SleepAdvice.evaluateSleep(monthAge, hours)
                    BabyCard(
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        cornerRadius = Radius.md,
                        contentPadding = PaddingValues(Spacing.md),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = RecordColor.Sleep,
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = advice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // 周视图图表
            if (dimension == "week") {
                item { WeeklyBarChart(weekDayStats) }
                item { WeeklyMilkLineChart(weekDayStats) }
                item { WeeklySleepBarChart(weekDayStats) }
            }

            // 时间轴标题
            if (timelineItems.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = when (dimension) {
                            "day" -> "当天记录"; "week" -> "本周记录"; "month" -> "本月记录"; else -> "记录"
                        },
                        trailing = {
                            Text(
                                "${timelineItems.size} 条",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        },
                    )
                }
            }

            // 时间轴列表
            if (timelineItems.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventNote,
                        title = "暂无记录",
                        subtitle = "快来记录宝宝的第一次吧~",
                        emoji = "🌈",
                    )
                }
            } else {
                itemsIndexed(timelineItems) { index, item ->
                    val timeText = buildString {
                        if (dimension != "day") append(DateUtils.formatShortDate(item.time) + "\n")
                        append(DateUtils.formatTime(item.time))
                    }
                    TimelineItem(
                        time = timeText,
                        title = item.title,
                        subtitle = item.detail.split("\n").firstOrNull() ?: "",
                        typeKey = item.type,
                        isLast = index == timelineItems.lastIndex,
                        onClick = {
                            val editRoute = when (item.tableName) {
                                "feeds" -> "record/feeding/edit/${item.id}"
                                "diapers" -> "record/diaper/edit/${item.id}"
                                "sleeps" -> "record/sleep/edit/${item.id}"
                                "foods" -> "record/food/edit/${item.id}"
                                "supplements" -> "record/supplement/edit/${item.id}"
                                "growth_records" -> "record/growth/edit/${item.id}"
                                else -> null
                            }
                            editRoute?.let { navController.navigate(it) }
                        },
                        onLongClick = { showDeleteDialog = item },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.xl)) }
        }
    }
}

// ==================== 子组件 ====================

/** 维度选择器 (日/周/月) */
@Composable
private fun DimensionSelector(
    selected: String,
    labels: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            labels.forEach { (label, key) ->
                val isSelected = selected == key
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Radius.sm))
                        .clickable { onSelect(key) },
                    shape = RoundedCornerShape(Radius.sm),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** 日期导航器 */
@Composable
private fun DateNavigator(dateLabel: String, onNavigate: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { onNavigate(-1) },
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "前",
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.lg))
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.width(Spacing.lg))
        FilledTonalIconButton(
            onClick = { onNavigate(1) },
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward, "后",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 类型筛选行 */
@Composable
private fun CategoryFilterRow(
    activeTab: String,
    tabLabels: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(tabLabels) { (label, key) ->
            FilterTag(
                text = label,
                selected = activeTab == key,
                onClick = { onSelect(key) },
            )
        }
    }
}

/** 汇总统计卡片 - 全新设计 */
@Composable
private fun SummaryStatsCard(title: String, summary: DaySummary) {
    BabyTitledCard(title = title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                value = "${summary.feedCount}",
                label = "喂奶",
                icon = Icons.Outlined.Restaurant,
                iconColor = RecordColor.Breast,
            )
            StatItem(
                value = "${summary.diaperCount}",
                label = "尿布",
                icon = Icons.Outlined.BabyChangingStation,
                iconColor = RecordColor.Diaper,
            )
            StatItem(
                value = DateUtils.formatDuration(summary.sleepMinutes),
                label = "睡眠",
                icon = Icons.Outlined.Bedtime,
                iconColor = RecordColor.Sleep,
            )
            StatItem(
                value = "${summary.foodCount}",
                label = "辅食",
                icon = Icons.Outlined.RiceBowl,
                iconColor = RecordColor.Food,
            )
        }
        if (summary.totalMilk > 0) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            val avgMilk = if (summary.feedCount > 0) summary.totalMilk / summary.feedCount else 0
            Text(
                text = "总奶量 ${summary.totalMilk}ml · 均量 ${avgMilk}ml/次",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 周视图柱状图 */
@Composable
private fun WeeklyBarChart(stats: List<WeekDayStat>) {
    if (stats.isEmpty()) return
    val maxCount = stats.maxOf { it.count }.coerceAtLeast(1)

    BabyTitledCard(title = "每日记录数") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.forEach { stat ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "${stat.count}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val barHeight = if (stat.count > 0) (stat.count.toFloat() / maxCount * 70).dp else 3.dp
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = Radius.sm, topEnd = Radius.sm))
                            .background(
                                if (stat.count > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stat.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

/** 月视图日历 */
@Composable
private fun MonthCalendarCard(
    currentDate: Long,
    monthDayStats: List<MonthDayStat>,
) {
    val cal = Calendar.getInstance().apply { timeInMillis = currentDate }
    val todayTimestamp = System.currentTimeMillis()
    val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")
    val primaryColor = MaterialTheme.colorScheme.primary

    BabyCard {
        Text(
            text = "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.md))

        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (monthDayStats.isNotEmpty()) {
            monthDayStats.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    week.forEach { dayStat ->
                        val isToday = dayStat.isCurrentMonth &&
                                DateUtils.isSameDay(dayStat.timestamp, todayTimestamp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .then(
                                            if (isToday) Modifier
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = 0.12f))
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${dayStat.day}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isToday -> primaryColor
                                            dayStat.isCurrentMonth -> MaterialTheme.colorScheme.onBackground
                                            else -> MaterialTheme.colorScheme.outline
                                        },
                                    )
                                }
                                // 数据标记：用圆点表示有记录，颜色深浅反映记录数量
                                if (dayStat.isCurrentMonth) {
                                    if (dayStat.count > 0) {
                                        val dotAlpha = (0.4f + (dayStat.count.coerceAtMost(8) / 8f) * 0.6f)
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = dotAlpha)),
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * 周奶量趋势折线图
 */
@Composable
private fun WeeklyMilkLineChart(stats: List<WeekDayStat>) {
    if (stats.isEmpty() || stats.all { it.totalMilk == 0 }) return
    val maxMilk = stats.maxOf { it.totalMilk }.coerceAtLeast(50)

    BabyTitledCard(title = "奶量趋势 (ml)") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.forEach { stat ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    if (stat.totalMilk > 0) {
                        Text(
                            text = "${stat.totalMilk}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RecordColor.Formula,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((stat.totalMilk.toFloat() / maxMilk * 80).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(RecordColor.Formula),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(82.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stat.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

/**
 * 周睡眠时长柱状图
 */
@Composable
private fun WeeklySleepBarChart(stats: List<WeekDayStat>) {
    if (stats.isEmpty() || stats.all { it.sleepMinutes == 0 }) return
    val maxSleepHours = stats.maxOf { it.sleepMinutes / 60f }.coerceAtLeast(1f)

    BabyTitledCard(title = "睡眠时长 (h)") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.forEach { stat ->
                val sleepHours = stat.sleepMinutes / 60f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    if (stat.sleepMinutes > 0) {
                        Text(
                            text = "${String.format("%.1f", sleepHours)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RecordColor.Sleep,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height((sleepHours / maxSleepHours * 80).dp)
                                .clip(RoundedCornerShape(topStart = Radius.sm, topEnd = Radius.sm))
                                .background(RecordColor.Sleep.copy(alpha = 0.7f)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = Radius.sm, topEnd = Radius.sm))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stat.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
