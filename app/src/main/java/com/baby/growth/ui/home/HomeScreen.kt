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
import com.baby.growth.data.entity.BabyInfo
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
    val smartTips by viewModel.smartTips.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<RecentRecord?>(null) }

    var showWelcomeGuide by remember { mutableStateOf(false) }

    LaunchedEffect(babyInfo) {
        if (babyInfo != null && babyInfo!!.name.isEmpty()) {
            showWelcomeGuide = true
        }
    }

    LaunchedEffect(Unit) { viewModel.loadData() }

    // 新用户欢迎引导
    if (showWelcomeGuide) {
        AlertDialog(
            onDismissRequest = { showWelcomeGuide = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("👶", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text("欢迎使用宝宝成长", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "记录宝宝的每一个珍贵瞬间\n\n先设置宝宝的信息，让我们更好地为您服务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWelcomeGuide = false
                    navController.navigate("profile")
                }) { Text("去设置", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showWelcomeGuide = false }) { Text("稍后再说") }
            },
            shape = RoundedCornerShape(Radius.xl),
        )
    }

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
        // 宝宝信息卡片
        item {
            BabyInfoHeader(
                babyInfo = babyInfo,
                lastFeedTime = todayStats.let { null },
                onProfileClick = { navController.navigate("profile") }
            )
        }

        // 智能提示
        if (smartTips.isNotEmpty()) {
            item {
                SmartTipsBanner(tips = smartTips)
            }
        }

        // 今日统计卡片（可点击）
        item { TodayStatsCard(todayStats, navController) }

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
                    emoji = "📝",
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
                    onClick = {
                        val editRoute = when (record.tableName) {
                            "feeds" -> "record/feeding/edit/${record.id}"
                            "diapers" -> "record/diaper/edit/${record.id}"
                            "sleeps" -> "record/sleep/edit/${record.id}"
                            "foods" -> "record/food/edit/${record.id}"
                            "supplements" -> "record/supplement/edit/${record.id}"
                            else -> null
                        }
                        editRoute?.let { navController.navigate(it) }
                    },
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
private fun TodayStatsCard(stats: TodayStats, navController: NavController) {
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
        ) {
            HomeStatItem(Icons.Outlined.Restaurant, "${stats.feedCount}", "喂奶", RecordColor.Breast,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("record/feeding") })
            HomeStatItem(Icons.Outlined.BabyChangingStation, "${stats.diaperCount}", "尿布", RecordColor.Diaper,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("record/diaper") })
            HomeStatItem(Icons.Outlined.RiceBowl, "${stats.foodCount}", "辅食", RecordColor.Food,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("record/food") })
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            HomeStatItem(Icons.Outlined.Bedtime, DateUtils.formatDuration(stats.sleepMinutes), "睡眠", RecordColor.Sleep,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("record/sleep") })
            HomeStatItem(Icons.Outlined.Medication, "${stats.supplementCount}", "营养", RecordColor.Supplement,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("record/supplement") })
        }
    }
}

@Composable
private fun HomeStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = Spacing.sm),
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
            maxLines = 1,
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

/**
 * 宝宝信息头部卡片
 */
@Composable
private fun BabyInfoHeader(
    babyInfo: BabyInfo?,
    lastFeedTime: Long?,
    onProfileClick: () -> Unit
) {
    BabyCard(
        cornerRadius = Radius.xl,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = babyInfo?.avatar ?: "👶",
                    fontSize = 28.sp,
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = babyInfo?.name?.ifEmpty { "设置宝宝信息" } ?: "设置宝宝信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (babyInfo != null && babyInfo.name.isNotEmpty()) {
                    val monthAge = DateUtils.getMonthAge(babyInfo.birthday)
                    val dayAge = DateUtils.getDayAge(babyInfo.birthday)
                    val ageText = if (monthAge > 0) "${monthAge}个月${dayAge % 30}天" else "${dayAge}天"
                    Text(
                        text = ageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "点击录入宝宝的生日和昵称 →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // 右侧装饰
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "编辑",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * 智能提示横幅
 */
@Composable
private fun SmartTipsBanner(tips: List<SmartTip>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        tips.forEach { tip ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = tip.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = tip.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
