package com.baby.growth.ui.vaccine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.data.entity.VaccineRecord
import com.baby.growth.ui.components.BabyAccentCard
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.components.EmptyState
import com.baby.growth.ui.theme.Radius
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.ui.theme.TextSecondary
import com.baby.growth.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineScreen(
    navController: NavController,
    viewModel: VaccineViewModel = viewModel()
) {
    val vaccineRecords by viewModel.vaccineRecords.collectAsState()
    val babyBirthday by viewModel.babyBirthday.collectAsState()

    var selectedTab by remember { mutableStateOf("free") }
    var showMarkDialog by remember { mutableStateOf<VaccineRecord?>(null) }

    LaunchedEffect(babyBirthday) {
        babyBirthday?.let { birthday ->
            viewModel.initializeVaccineSchedule(birthday)
        }
    }

    val upcomingVaccine = viewModel.getUpcomingVaccine()
    val (doneCount, totalCount) = viewModel.getProgressStats(selectedTab)
    val filteredVaccines = viewModel.getFilteredVaccines(selectedTab)

    Scaffold(
        topBar = {
            BabyTopBar(title = "疫苗接种")
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            TabRow(
                selectedTabIndex = if (selectedTab == "free") 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == "free",
                    onClick = { selectedTab = "free" },
                    text = { Text("免费疫苗 (${viewModel.getProgressStats("free").second})") }
                )
                Tab(
                    selected = selectedTab == "paid",
                    onClick = { selectedTab = "paid" },
                    text = { Text("自费疫苗 (${viewModel.getProgressStats("paid").second})") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    ProgressCard(doneCount, totalCount)
                }

                if (upcomingVaccine != null && upcomingVaccine.vaccineType == selectedTab) {
                    item {
                        UpcomingVaccineCard(upcomingVaccine)
                    }
                }

                if (filteredVaccines.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Vaccines,
                            title = "暂无疫苗记录",
                            subtitle = "添加宝宝生日后会自动生成接种计划"
                        )
                    }
                } else {
                    items(filteredVaccines) { record ->
                        VaccineItemCard(
                            record = record,
                            onClick = { showMarkDialog = record }
                        )
                    }
                }
            }
        }
    }

    showMarkDialog?.let { record ->
        AlertDialog(
            onDismissRequest = { showMarkDialog = null },
            title = { Text("确认接种") },
            text = { Text("确认已接种 ${record.vaccineName} (${record.dose}) 吗？") },
            shape = RoundedCornerShape(Radius.xl),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markVaccinated(record)
                    showMarkDialog = null
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ProgressCard(doneCount: Int, totalCount: Int) {
    BabyAccentCard {
        Text("接种进度", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress = { if (totalCount > 0) doneCount.toFloat() / totalCount else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            "已完成 $doneCount/$totalCount (${if (totalCount > 0) (doneCount * 100 / totalCount) else 0}%)",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 12.sp
        )
    }
}

@Composable
fun UpcomingVaccineCard(record: VaccineRecord) {
    BabyCard(
        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "即将接种",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${record.vaccineName} (${record.dose})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "推荐时间: ${DateUtils.formatDate(record.scheduledDate)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun VaccineItemCard(record: VaccineRecord, onClick: () -> Unit) {
    val isDone = record.status == "done"
    BabyCard(
        modifier = Modifier.clickable(onClick = onClick),
        cornerRadius = Radius.md
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.vaccineName} (${record.dose})",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = "推荐月龄: ${record.ageMonths}个月 | 计划日期: ${DateUtils.formatDate(record.scheduledDate)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (isDone && record.actualDate != null) {
                    Text(
                        text = "实际接种: ${DateUtils.formatDate(record.actualDate)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.Schedule,
                contentDescription = null,
                tint = if (isDone) MaterialTheme.colorScheme.primary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
