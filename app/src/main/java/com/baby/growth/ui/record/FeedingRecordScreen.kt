package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.FeedRecord
import com.baby.growth.service.FeedingTimerService
import com.baby.growth.ui.components.*
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.BreastfeedingTimer
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FeedingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<FeedRecord?>(null)
    val lastRecord: State<FeedRecord?> = _lastRecord

    private val _editRecord = mutableStateOf<FeedRecord?>(null)
    val editRecord: State<FeedRecord?> = _editRecord

    init {
        viewModelScope.launch {
            _lastRecord.value = db.feedDao().getLatest()
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            _editRecord.value = db.feedDao().getById(id)
        }
    }

    fun saveRecord(
        type: String, amount: Int, unit: String,
        leftDuration: Int, rightDuration: Int,
        startTime: Long, endTime: Long,
        side: String, note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.feedDao().insert(
                FeedRecord(
                    uniqueId = DateUtils.generateUniqueId(type),
                    type = type, amount = amount, unit = unit,
                    leftDuration = leftDuration, rightDuration = rightDuration,
                    startTime = startTime, endTime = endTime,
                    side = side, note = note
                )
            )
            onSuccess()
        }
    }

    fun updateRecord(
        record: FeedRecord, type: String, amount: Int, unit: String,
        leftDuration: Int, rightDuration: Int,
        side: String, note: String, recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.feedDao().update(
                record.copy(
                    type = type, amount = amount, unit = unit,
                    leftDuration = leftDuration, rightDuration = rightDuration,
                    side = side, note = note, recordTime = recordTime
                )
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingRecordScreen(
    navController: NavController,
    editId: Long? = null,
    viewModel: FeedingViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = editId != null
    val editRecord by viewModel.editRecord

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }

    var feedType by remember { mutableStateOf("breast") }
    var amount by remember { mutableStateOf("") }
    var side by remember { mutableStateOf("both") }
    var note by remember { mutableStateOf("") }
    var recordTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 手动输入的左右时长（分钟）
    var manualLeftMinutes by remember { mutableStateOf("") }
    var manualRightMinutes by remember { mutableStateOf("") }

    // 编辑模式回填数据
    LaunchedEffect(editRecord) {
        editRecord?.let { record ->
            feedType = record.type
            amount = if (record.amount > 0) record.amount.toString() else ""
            side = record.side
            note = record.note
            recordTime = record.recordTime
            if (record.leftDuration > 0) manualLeftMinutes = record.leftDuration.toString()
            if (record.rightDuration > 0) manualRightMinutes = record.rightDuration.toString()
        }
    }

    // 从持久化计时器读取状态
    val timerState by BreastfeedingTimer.state.collectAsState()

    // 实时刷新计时显示
    var leftDisplaySeconds by remember { mutableStateOf(0) }
    var rightDisplaySeconds by remember { mutableStateOf(0) }

    // 每秒刷新计时显示
    LaunchedEffect(timerState.isRunning) {
        while (true) {
            leftDisplaySeconds = BreastfeedingTimer.getLeftTotalSeconds()
            rightDisplaySeconds = BreastfeedingTimer.getRightTotalSeconds()
            delay(1000)
        }
    }

    val lastRecord by viewModel.lastRecord

    val lastRecordSubtitle = lastRecord?.let { last ->
        val relativeTime = DateUtils.formatRelativeTime(last.recordTime)
        if (last.type == "breast") {
            val parts = mutableListOf<String>()
            if (last.leftDuration > 0) parts.add("左侧${last.leftDuration}min")
            if (last.rightDuration > 0) parts.add("右侧${last.rightDuration}min")
            val durationText = if (parts.isNotEmpty()) parts.joinToString("/") else "0min"
            "上次：$durationText，$relativeTime"
        } else {
            "上次：${last.amount}ml，$relativeTime"
        }
    }

    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = recordTime
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        val cal = Calendar.getInstance().apply { timeInMillis = recordTime }
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        cal.set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                        cal.set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                        cal.set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                        recordTime = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 时间选择器
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = recordTime }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply { timeInMillis = recordTime }
                    newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    newCal.set(Calendar.MINUTE, timePickerState.minute)
                    recordTime = newCal.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            BabyTopBar(
                title = if (isEditMode) "编辑喂奶记录" else "喂奶记录",
                subtitle = if (isEditMode) null else lastRecordSubtitle,
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // 记录时间选择
            BabyCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("记录时间", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE).format(Date(recordTime)),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(Radius.md),
                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
                        ) {
                            Text("改日期", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(Radius.md),
                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
                        ) {
                            Text("改时间", fontSize = 13.sp)
                        }
                    }
                }
            }

            Text("喂养方式", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("breast" to "🤱 母乳", "formula" to "🍼 配方奶", "bottle" to "🍼 瓶喂母乳").forEach { (value, label) ->
                    FilterChip(selected = feedType == value, onClick = {
                        feedType = value
                    }, label = { Text(label) })
                }
            }

            if (feedType == "formula" || feedType == "bottle") {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("奶量 (ml)") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.md)
                )
            }

            if (feedType == "breast") {
                Text("哺乳计时", fontWeight = FontWeight.Bold)

                // 如果有计时器正在运行，显示醒目提示
                if (timerState.isRunning) {
                    BabyAccentCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                "正在计时 ${if (timerState.side == "left") "左侧" else "右侧"}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // 左侧计时
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("左侧", fontSize = 14.sp)
                        Text(
                            formatTimerDisplay(leftDisplaySeconds),
                            fontWeight = FontWeight.Bold, fontSize = 20.sp
                        )
                        if (!timerState.isRunning || timerState.side != "left") {
                            OutlinedButton(onClick = {
                                BreastfeedingTimer.start(context, "left")
                                FeedingTimerService.start(context, "left")
                            }) { Text("开始") }
                        } else {
                            Button(onClick = {
                                BreastfeedingTimer.stop(context)
                                FeedingTimerService.stop(context)
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )) { Text("停止") }
                        }
                    }
                    // 右侧计时
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("右侧", fontSize = 14.sp)
                        Text(
                            formatTimerDisplay(rightDisplaySeconds),
                            fontWeight = FontWeight.Bold, fontSize = 20.sp
                        )
                        if (!timerState.isRunning || timerState.side != "right") {
                            OutlinedButton(onClick = {
                                BreastfeedingTimer.start(context, "right")
                                FeedingTimerService.start(context, "right")
                            }) { Text("开始") }
                        } else {
                            Button(onClick = {
                                BreastfeedingTimer.stop(context)
                                FeedingTimerService.stop(context)
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )) { Text("停止") }
                        }
                    }
                }

                // 手动输入/微调
                Text("或手动输入时长（分钟）", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = manualLeftMinutes,
                        onValueChange = { manualLeftMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("左侧(分)") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md),
                        placeholder = { Text("${leftDisplaySeconds / 60}") }
                    )
                    OutlinedTextField(
                        value = manualRightMinutes,
                        onValueChange = { manualRightMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("右侧(分)") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md),
                        placeholder = { Text("${rightDisplaySeconds / 60}") }
                    )
                }
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md), minLines = 2
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            PrimaryButton(
                text = if (isEditMode) "保存修改" else "保存记录",
                onClick = {
                    val now = System.currentTimeMillis()
                    // 计算左右时长：手动输入优先，否则取计时器值
                    val effectiveLeftDur = manualLeftMinutes.toIntOrNull()
                        ?: if (feedType == "breast") BreastfeedingTimer.getLeftTotalSeconds() / 60 else 0
                    val effectiveRightDur = manualRightMinutes.toIntOrNull()
                        ?: if (feedType == "breast") BreastfeedingTimer.getRightTotalSeconds() / 60 else 0
                    val totalDur = effectiveLeftDur + effectiveRightDur

                    if (isEditMode && editRecord != null) {
                        viewModel.updateRecord(
                            record = editRecord!!,
                            type = feedType,
                            amount = amount.toIntOrNull() ?: 0,
                            unit = if (feedType == "breast") "min" else "ml",
                            leftDuration = effectiveLeftDur,
                            rightDuration = effectiveRightDur,
                            side = side, note = note,
                            recordTime = recordTime
                        ) {
                            Toast.makeText(context, "记录已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else {
                        // 保存时如果计时器还在运行，先停止计时
                        if (feedType == "breast" && timerState.isRunning) {
                            BreastfeedingTimer.stop(context)
                            FeedingTimerService.stop(context)
                        }
                        viewModel.saveRecord(
                            type = feedType,
                            amount = amount.toIntOrNull() ?: 0,
                            unit = if (feedType == "breast") "min" else "ml",
                            leftDuration = effectiveLeftDur,
                            rightDuration = effectiveRightDur,
                            startTime = if (feedType == "breast") now - totalDur * 60000L else 0,
                            endTime = if (feedType == "breast") now else 0,
                            side = side, note = note
                        ) {
                            BreastfeedingTimer.reset(context)
                            Toast.makeText(context, "喂奶记录已保存", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}

private fun formatTimerDisplay(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "${h}h${m}m${s}s"
        m > 0 -> "${m}m${s}s"
        else -> "${s}s"
    }
}
