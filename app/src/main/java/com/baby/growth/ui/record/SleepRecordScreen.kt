package com.baby.growth.ui.record

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.baby.growth.data.entity.SleepRecord
import com.baby.growth.service.SleepTimerService
import com.baby.growth.ui.components.*
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.RecordTypes
import com.baby.growth.utils.SleepTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SleepViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<SleepRecord?>(null)
    val lastRecord: State<SleepRecord?> = _lastRecord

    private val _editRecord = mutableStateOf<SleepRecord?>(null)
    val editRecord: State<SleepRecord?> = _editRecord

    init {
        viewModelScope.launch {
            _lastRecord.value = db.sleepDao().getLatest()
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            _editRecord.value = db.sleepDao().getById(id)
        }
    }

    fun saveRecord(
        startTime: Long, endTime: Long, duration: Int,
        quality: String, isNextDay: Int, note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.sleepDao().insert(
                SleepRecord(
                    uniqueId = DateUtils.generateUniqueId("sleep"),
                    startTime = startTime, endTime = endTime,
                    duration = duration, quality = quality,
                    isNextDay = isNextDay, note = note
                )
            )
            onSuccess()
        }
    }

    fun updateRecord(
        record: SleepRecord, startTime: Long, endTime: Long, duration: Int,
        quality: String, isNextDay: Int, note: String, recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.sleepDao().update(
                record.copy(
                    startTime = startTime, endTime = endTime,
                    duration = duration, quality = quality,
                    isNextDay = isNextDay, note = note, recordTime = recordTime
                )
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepRecordScreen(
    navController: NavController,
    editId: Long? = null,
    viewModel: SleepViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = editId != null
    val editRecord by viewModel.editRecord

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }

    var mode by remember { mutableStateOf("timer") } // "timer" 或 "manual"
    var quality by remember { mutableStateOf("good") }
    var note by remember { mutableStateOf("") }
    var recordTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 编辑模式回填数据
    LaunchedEffect(editRecord) {
        editRecord?.let { record ->
            mode = "manual"
            quality = record.quality
            note = record.note
            recordTime = record.recordTime
        }
    }

    // 手动模式变量
    var startHour by remember { mutableStateOf(22) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(6) }
    var endMinute by remember { mutableStateOf(0) }
    var isNextDay by remember { mutableStateOf(true) }

    // 计时器状态
    val timerState by SleepTimer.state.collectAsState()
    var displaySeconds by remember { mutableStateOf(0) }

    // 每秒刷新计时显示
    LaunchedEffect(timerState.isRunning) {
        while (true) {
            displaySeconds = SleepTimer.getTotalSeconds()
            delay(1000)
        }
    }

    // 手动模式自动计算时长
    val manualDurationMinutes = remember(startHour, startMinute, endHour, endMinute, isNextDay) {
        var startTotal = startHour * 60 + startMinute
        var endTotal = endHour * 60 + endMinute
        if (isNextDay) endTotal += 24 * 60
        if (endTotal <= startTotal) endTotal += 24 * 60
        endTotal - startTotal
    }

    val lastRecord by viewModel.lastRecord

    val lastRecordSubtitle = lastRecord?.let { last ->
        val relativeTime = DateUtils.formatRelativeTime(last.recordTime)
        "上次：${DateUtils.formatDuration(last.duration)}，$relativeTime"
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
                title = if (isEditMode) "编辑睡眠记录" else "睡眠记录",
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

            // 模式切换
            Text("记录方式", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(selected = mode == "timer", onClick = { mode = "timer" },
                    label = { Text("⏱️ 计时") })
                FilterChip(selected = mode == "manual", onClick = { mode = "manual" },
                    label = { Text("✏️ 手动") })
            }

            if (mode == "timer") {
                Text("睡眠计时", fontWeight = FontWeight.Bold)

                // 如果计时器正在运行，显示醒目提示
                if (timerState.isRunning) {
                    BabyAccentCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("😴", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                "正在计时",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 计时显示
                BabyCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (timerState.isRunning)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    cornerRadius = Radius.xl
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            formatSleepTimerDisplay(displaySeconds),
                            fontWeight = FontWeight.Bold, fontSize = 36.sp,
                            color = if (timerState.isRunning)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 开始/停止按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (!timerState.isRunning) {
                        PrimaryButton(
                            text = "开始计时",
                            onClick = {
                                SleepTimer.start(context)
                                SleepTimerService.start(context)
                            }
                        )
                    } else {
                        Button(
                            onClick = {
                                SleepTimer.stop(context)
                                SleepTimerService.stop(context)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(Radius.md),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("停止计时", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            if (mode == "manual") {
                // 入睡时间
                Text("入睡时间", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startHour.toString(), onValueChange = {
                            startHour = it.toIntOrNull()?.coerceIn(0, 23) ?: startHour
                        },
                        label = { Text("时") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md)
                    )
                    OutlinedTextField(
                        value = startMinute.toString(), onValueChange = {
                            startMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: startMinute
                        },
                        label = { Text("分") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md)
                    )
                }

                // 醒来时间
                Text("醒来时间", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = endHour.toString(), onValueChange = {
                            endHour = it.toIntOrNull()?.coerceIn(0, 23) ?: endHour
                        },
                        label = { Text("时") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md)
                    )
                    OutlinedTextField(
                        value = endMinute.toString(), onValueChange = {
                            endMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: endMinute
                        },
                        label = { Text("分") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md)
                    )
                    FilterChip(
                        selected = isNextDay, onClick = { isNextDay = !isNextDay },
                        label = { Text("次日") }
                    )
                }

                // 自动计算时长显示
                BabyCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    cornerRadius = Radius.md
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("睡眠时长: ", fontSize = 14.sp)
                        Text(
                            DateUtils.formatDuration(manualDurationMinutes),
                            fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 睡眠质量
            Text("睡眠质量", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                RecordTypes.SLEEP_QUALITIES.forEach { (label, value) ->
                    FilterChip(selected = quality == value, onClick = { quality = value },
                        label = { Text(label) })
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
                    if (isEditMode && editRecord != null) {
                        viewModel.updateRecord(
                            record = editRecord!!,
                            startTime = editRecord!!.startTime,
                            endTime = editRecord!!.endTime,
                            duration = editRecord!!.duration,
                            quality = quality,
                            isNextDay = editRecord!!.isNextDay,
                            note = note,
                            recordTime = recordTime
                        ) {
                            Toast.makeText(context, "记录已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else if (mode == "timer") {
                        // 计时模式: 停止计时并保存
                        if (timerState.isRunning) {
                            SleepTimer.stop(context)
                            SleepTimerService.stop(context)
                        }
                        val totalSeconds = SleepTimer.getTotalSeconds()
                        if (totalSeconds == 0) {
                            // 没有开始计时，无法保存
                            return@PrimaryButton
                        }
                        val durationMinutes = totalSeconds / 60
                        val startTime = SleepTimer.getStartTime()
                        val endTime = System.currentTimeMillis()

                        viewModel.saveRecord(
                            startTime = startTime, endTime = endTime,
                            duration = durationMinutes, quality = quality,
                            isNextDay = 0, note = note
                        ) {
                            SleepTimer.reset(context)
                            Toast.makeText(context, "睡眠记录已保存", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else {
                        // 手动模式
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, startHour)
                        cal.set(Calendar.MINUTE, startMinute)
                        cal.set(Calendar.SECOND, 0)
                        val startTime = cal.timeInMillis

                        val endCal = Calendar.getInstance()
                        endCal.set(Calendar.HOUR_OF_DAY, endHour)
                        endCal.set(Calendar.MINUTE, endMinute)
                        endCal.set(Calendar.SECOND, 0)
                        if (isNextDay) endCal.add(Calendar.DAY_OF_MONTH, 1)
                        val endTime = endCal.timeInMillis

                        viewModel.saveRecord(
                            startTime = startTime, endTime = endTime,
                            duration = manualDurationMinutes, quality = quality,
                            isNextDay = if (isNextDay) 1 else 0, note = note
                        ) { navController.popBackStack() }
                    }
                }
            )
        }
    }
}

private fun formatSleepTimerDisplay(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "${h}h ${m}m ${s}s"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
