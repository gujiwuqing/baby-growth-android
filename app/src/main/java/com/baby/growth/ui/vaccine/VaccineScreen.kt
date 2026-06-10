package com.baby.growth.ui.vaccine

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.VaccineRecord
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.VaccineData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class VaccineViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _vaccineRecords = MutableStateFlow<List<VaccineRecord>>(emptyList())
    val vaccineRecords: StateFlow<List<VaccineRecord>> = _vaccineRecords.asStateFlow()

    private val _babyBirthday = MutableStateFlow<Long?>(null)
    val babyBirthday: StateFlow<Long?> = _babyBirthday.asStateFlow()

    init {
        viewModelScope.launch {
            db.vaccineDao().getAll().collect { records ->
                _vaccineRecords.value = records
            }
        }
        viewModelScope.launch {
            db.babyInfoDao().getBabyInfo().collect { babyInfo ->
                _babyBirthday.value = babyInfo?.birthday
            }
        }
    }

    fun initializeVaccineSchedule(birthday: Long) {
        viewModelScope.launch {
            val existingRecords = db.vaccineDao().getAllOnce()
            if (existingRecords.isNotEmpty()) return@launch

            val allVaccines = VaccineData.getAllVaccines()
            val recordsToInsert = allVaccines.map { vaccine ->
                val scheduledDate = DateUtils.addMonthsToBirthday(birthday, vaccine.ageMonths)
                VaccineRecord(
                    uniqueId = "${vaccine.name}_${vaccine.dose}_${scheduledDate}",
                    vaccineName = vaccine.name,
                    vaccineType = vaccine.type,
                    dose = vaccine.dose,
                    ageMonths = vaccine.ageMonths,
                    scheduledDate = scheduledDate,
                    status = "pending"
                )
            }
            db.vaccineDao().insertAll(recordsToInsert)
        }
    }

    fun markVaccinated(record: VaccineRecord) {
        viewModelScope.launch {
            val updated = record.copy(
                status = "done",
                actualDate = System.currentTimeMillis()
            )
            db.vaccineDao().update(updated)
        }
    }

    fun getFilteredVaccines(type: String): List<VaccineRecord> {
        return _vaccineRecords.value.filter { it.vaccineType == type }
            .sortedBy { it.scheduledDate }
    }

    fun getUpcomingVaccine(): VaccineRecord? {
        return _vaccineRecords.value
            .filter { it.status == "pending" }
            .sortedBy { it.scheduledDate }
            .firstOrNull()
    }

    fun getProgressStats(type: String): Pair<Int, Int> {
        val filtered = _vaccineRecords.value.filter { it.vaccineType == type }
        val done = filtered.count { it.status == "done" }
        return done to filtered.size
    }
}

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
            TopAppBar(
                title = { Text("疫苗接种") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Background)
        ) {
            TabRow(
                selectedTabIndex = if (selectedTab == "free") 0 else 1,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ProgressCard(doneCount, totalCount)
                }

                if (upcomingVaccine != null && upcomingVaccine.vaccineType == selectedTab) {
                    item {
                        UpcomingVaccineCard(upcomingVaccine)
                    }
                }

                items(filteredVaccines) { record ->
                    VaccineItemCard(
                        record = record,
                        onClick = { showMarkDialog = record }
                    )
                }
            }
        }
    }

    showMarkDialog?.let { record ->
        AlertDialog(
            onDismissRequest = { showMarkDialog = null },
            title = { Text("确认接种") },
            text = { Text("确认已接种 ${record.vaccineName} (${record.dose}) 吗？") },
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("接种进度", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (totalCount > 0) doneCount.toFloat() / totalCount else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("已完成 $doneCount/$totalCount", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun UpcomingVaccineCard(record: VaccineRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Mint80)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏰", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("即将接种", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                Text("${record.vaccineName} (${record.dose})", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("推荐时间: ${DateUtils.formatDate(record.scheduledDate)}", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun VaccineItemCard(record: VaccineRecord, onClick: () -> Unit) {
    val isDone = record.status == "done"
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Mint80 else Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp
                    )
                }
            }
            if (isDone) {
                Text("✅", fontSize = 20.sp)
            } else {
                Text("⏰", fontSize = 20.sp)
            }
        }
    }
}
