package com.baby.growth.ui.records

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
                "week" -> DateUtils.getWeekRange()
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

    var showDeleteDialog by remember { mutableStateOf<TimelineItem?>(null) }

    val dimensionLabels = listOf("日" to "day", "周" to "week", "月" to "month")
    val tabLabels = listOf("全部" to "all", "喂养" to "feeding", "睡眠" to "sleep",
        "换尿布" to "diaper", "辅食" to "food", "补剂" to "supplement")

    val dateLabel = remember(currentDate, dimension) {
        when (dimension) {
            "day" -> DateUtils.formatDate(currentDate)
            "week" -> {
                val (start, end) = DateUtils.getWeekRange()
                "${DateUtils.formatDate(start)} ~ ${DateUtils.formatDate(end)}"
            }
            "month" -> DateUtils.formatMonth(currentDate)
            else -> DateUtils.formatDate(currentDate)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录总览") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 维度切换
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    dimensionLabels.forEach { (label, key) ->
                        FilterChip(
                            selected = dimension == key,
                            onClick = { viewModel.setDimension(key) },
                            label = { Text(label) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // 日期导航
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateDate(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "前一天")
                    }
                    Text(dateLabel, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { viewModel.navigateDate(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "后一天")
                    }
                }
            }

            // 类型筛选
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabLabels.forEach { (label, key) ->
                        FilterChip(
                            selected = activeTab == key,
                            onClick = { viewModel.setActiveTab(key) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // 汇总统计卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("🍼 喂养", "${summary.feedCount}次")
                        StatItem("💧 尿布", "${summary.diaperCount}次")
                        StatItem("😴 睡眠", DateUtils.formatDuration(summary.sleepMinutes))
                        StatItem("🥣 辅食", "${summary.foodCount}次")
                    }
                }
            }

            // 睡眠建议
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
                        Text(
                            "💤 $advice",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 时间轴
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                                    .clickable { showDeleteDialog = item }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // 左侧时间
                                Text(
                                    text = DateUtils.formatTime(item.time),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(44.dp).padding(top = 4.dp)
                                )

                                // 中间时间线
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
                                                .background(Color(0xFFE8E8E8))
                                        )
                                    }
                                }

                                // 右侧内容
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(RecordTypes.getIcon(item.type), fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        if (item.detail.isNotEmpty()) {
                                            val lines = item.detail.split("\n")
                                            Text(
                                                lines.first(), fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                            if (lines.size > 1) {
                                                Text(
                                                    lines[1], fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
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

            if (timelineItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📝", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text("快来记录宝宝的第一次吧~", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}
