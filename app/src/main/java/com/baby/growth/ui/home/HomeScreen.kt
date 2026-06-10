package com.baby.growth.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val babyInfo by viewModel.babyInfo.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val recentRecords by viewModel.recentRecords.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<RecentRecord?>(null) }

    LaunchedEffect(Unit) { viewModel.loadData() }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("是否删除这条${showDeleteDialog!!.title}记录？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(showDeleteDialog!!)
                    showDeleteDialog = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TodayStatsCard(todayStats) }
        item { QuickRecordCard(navController) }
        item {
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (recentRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无记录，点击上方快捷入口开始记录吧~", color = TextHint)
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        recentRecords.forEachIndexed { index, record ->
                            RecentRecordItem(
                                record = record,
                                isLast = index == recentRecords.lastIndex,
                                onLongClick = { showDeleteDialog = record }
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun TodayStatsCard(stats: TodayStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "今日统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("🤱", "喂奶", "${stats.feedCount}次")
                StatItem("👶", "尿布", "${stats.diaperCount}次")
                StatItem("😴", "睡眠", DateUtils.formatDuration(stats.sleepMinutes))
                StatItem("🥣", "辅食", "${stats.foodCount}次")
                StatItem("💊", "营养", "${stats.supplementCount}次")
                StatItem("🍼", "奶量", "${stats.totalMilk}ml")
            }
        }
    }
}

@Composable
fun StatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun QuickRecordCard(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "快捷记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickButton("🤱", "母乳", MaterialTheme.colorScheme.primary) { navController.navigate("record/feeding") }
                QuickButton("🍼", "配方奶", Color(0xFF5B8DEF)) { navController.navigate("record/feeding") }
                QuickButton("👶", "尿布", Color(0xFFFFB74D)) { navController.navigate("record/diaper") }
                QuickButton("😴", "睡眠", Color(0xFF7986CB)) { navController.navigate("record/sleep") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickButton("🥣", "辅食", Mint40) { navController.navigate("record/food") }
                QuickButton("💊", "营养", Color(0xFFBA68C8)) { navController.navigate("record/supplement") }
                QuickButton("📏", "成长", Color(0xFF4DB6AC)) { navController.navigate("record/growth") }
                QuickButton("💉", "疫苗", Color(0xFFE57373)) { navController.navigate("vaccine") }
            }
        }
    }
}

@Composable
fun QuickButton(emoji: String, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentRecordItem(record: RecentRecord, isLast: Boolean = false, onLongClick: () -> Unit) {
    val dotColor = when (record.type) {
        "breast", "formula", "bottle" -> Color(0xFFFF8A9B)
        "sleep" -> Color(0xFF7986CB)
        "diaper" -> Color(0xFFFFB74D)
        "food" -> Color(0xFF66BB6A)
        "supplement" -> Color(0xFF42A5F5)
        "growth" -> Color(0xFFEC407A)
        else -> Color(0xFFBDBDBD)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 左侧时间
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.time)),
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(44.dp).padding(top = 4.dp)
        )

        // 中间时间线：圆点 + 竖线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .padding(top = 6.dp)
                    .clip(CircleShape)
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
            Text(text = record.icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = OnBackground
                )
                if (record.detail.isNotEmpty()) {
                    val lines = record.detail.split("\n")
                    Text(
                        text = lines.first(),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    if (lines.size > 1) {
                        Text(
                            text = lines[1],
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
