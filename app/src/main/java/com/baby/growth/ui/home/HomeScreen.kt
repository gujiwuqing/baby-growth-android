package com.baby.growth.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.baby.growth.ui.components.*
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
            title = { Text("确认删除", fontWeight = FontWeight.SemiBold) },
            text = { Text("是否删除这条${showDeleteDialog!!.title}记录？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(showDeleteDialog!!)
                    showDeleteDialog = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            },
            shape = RoundedCornerShape(Radius.xl),
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        

        // 今日统计卡片
        item { TodayStatsCard(todayStats) }

        // 快捷记录
        item { QuickRecordGrid(navController) }

        // 最近记录
        item {
            SectionHeader(
                title = "最近记录",
                trailing = {
                    TextButton(onClick = {
                        navController.navigate("records") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Text("查看全部", style = MaterialTheme.typography.labelMedium)
                    }
                },
            )
        }

        if (recentRecords.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.EventNote,
                    title = "还没有记录",
                    subtitle = "点击上方快捷入口，开始记录宝宝的每一天吧",
                )
            }
        } else {
            itemsIndexed(recentRecords) { index, record ->
                TimelineItem(
                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.time)),
                    title = record.title,
                    subtitle = record.detail.split("\n").firstOrNull() ?: "",
                    typeKey = record.type,
                    isLast = index == recentRecords.lastIndex,
                    onLongClick = { showDeleteDialog = record },
                )
            }
        }

        // 底部安全区
        item { Spacer(modifier = Modifier.height(Spacing.xl)) }
    }
}



/**
 * 今日统计卡片 - 全新设计
 */
@Composable
private fun TodayStatsCard(stats: TodayStats) {
    BabyAccentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "今日概览",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HomeStatItem(
                icon = Icons.Outlined.Restaurant,
                value = "${stats.feedCount}",
                label = "喂奶",
                color = RecordColor.Breast,
            )
            HomeStatItem(
                icon = Icons.Outlined.BabyChangingStation,
                value = "${stats.diaperCount}",
                label = "尿布",
                color = RecordColor.Diaper,
            )
            HomeStatItem(
                icon = Icons.Outlined.Bedtime,
                value = DateUtils.formatDuration(stats.sleepMinutes),
                label = "睡眠",
                color = RecordColor.Sleep,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HomeStatItem(
                icon = Icons.Outlined.RiceBowl,
                value = "${stats.foodCount}",
                label = "辅食",
                color = RecordColor.Food,
            )
            HomeStatItem(
                icon = Icons.Outlined.Medication,
                value = "${stats.supplementCount}",
                label = "营养",
                color = RecordColor.Supplement,
            )
            HomeStatItem(
                icon = Icons.Outlined.WaterDrop,
                value = "${stats.totalMilk}ml",
                label = "奶量",
                color = RecordColor.Formula,
            )
        }
    }
}

@Composable
private fun HomeStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
        )
    }
}

/**
 * 快捷记录入口 - 网格布局
 */
@Composable
private fun QuickRecordGrid(navController: NavController) {
    BabyCard {
        Text(
            text = "快捷记录",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            QuickActionButton(Icons.Outlined.Restaurant, "母乳", RecordColor.Breast,
                onClick = { navController.navigate("record/feeding") })
            QuickActionButton(Icons.Outlined.LocalDrink, "配方奶", RecordColor.Formula,
                onClick = { navController.navigate("record/feeding") })
            QuickActionButton(Icons.Outlined.BabyChangingStation, "尿布", RecordColor.Diaper,
                onClick = { navController.navigate("record/diaper") })
            QuickActionButton(Icons.Outlined.Bedtime, "睡眠", RecordColor.Sleep,
                onClick = { navController.navigate("record/sleep") })
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            QuickActionButton(Icons.Outlined.RiceBowl, "辅食", RecordColor.Food,
                onClick = { navController.navigate("record/food") })
            QuickActionButton(Icons.Outlined.Medication, "营养", RecordColor.Supplement,
                onClick = { navController.navigate("record/supplement") })
            QuickActionButton(Icons.Outlined.Straighten, "成长", RecordColor.Growth,
                onClick = { navController.navigate("record/growth") })
            QuickActionButton(Icons.Outlined.Vaccines, "疫苗", StatusColor.Error,
                onClick = { navController.navigate("vaccine") })
        }
    }
}


