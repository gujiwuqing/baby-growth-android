package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.GrowthRecord
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.components.PrimaryButton
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.GrowthCurveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GrowthRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = MutableStateFlow<GrowthRecord?>(null)
    val lastRecord: StateFlow<GrowthRecord?> = _lastRecord.asStateFlow()

    private val _editRecord = MutableStateFlow<GrowthRecord?>(null)
    val editRecord: StateFlow<GrowthRecord?> = _editRecord.asStateFlow()

    init {
        viewModelScope.launch {
            _lastRecord.value = db.growthDao().getLatest()
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            _editRecord.value = db.growthDao().getById(id)
        }
    }

    private val _babyInfo = MutableStateFlow<com.baby.growth.data.entity.BabyInfo?>(null)
    val babyInfo: StateFlow<com.baby.growth.data.entity.BabyInfo?> = _babyInfo.asStateFlow()

    init {
        viewModelScope.launch {
            _babyInfo.value = db.babyInfoDao().getBabyInfoOnce()
        }
    }

    fun saveRecord(
        height: Float?, weight: Float?, headCircumference: Float?,
        note: String, recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.growthDao().insert(
                GrowthRecord(
                    uniqueId = DateUtils.generateUniqueId("growth"),
                    height = height, weight = weight,
                    headCircumference = headCircumference, note = note,
                    recordTime = recordTime
                )
            )
            onSuccess()
        }
    }

    fun updateRecord(
        record: GrowthRecord, height: Float?, weight: Float?,
        headCircumference: Float?, note: String, recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.growthDao().update(
                record.copy(
                    height = height, weight = weight,
                    headCircumference = headCircumference, note = note,
                    recordTime = recordTime
                )
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthRecordScreen(
    navController: NavController,
    editId: Long? = null,
    viewModel: GrowthRecordViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = editId != null
    val editRecord by viewModel.editRecord.collectAsState()
    val lastRecord by viewModel.lastRecord.collectAsState()
    val babyInfo by viewModel.babyInfo.collectAsState()

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }

    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var headCircumference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var recordTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 编辑模式回填数据
    LaunchedEffect(editRecord) {
        editRecord?.let { record ->
            height = record.height?.toString() ?: ""
            weight = record.weight?.toString() ?: ""
            headCircumference = record.headCircumference?.toString() ?: ""
            note = record.note
            recordTime = record.recordTime
        }
    }

    // 计算当前月龄和参考值
    val monthAge = remember(babyInfo, recordTime) {
        babyInfo?.let { DateUtils.getMonthAge(it.birthday, recordTime) } ?: -1
    }
    val isMale = remember(babyInfo) { babyInfo?.gender == 1 }
    val heightRef = remember(monthAge, isMale) {
        if (monthAge < 0) null
        else getReferenceRange(monthAge, isMale, "height")
    }
    val weightRef = remember(monthAge, isMale) {
        if (monthAge < 0) null
        else getReferenceRange(monthAge, isMale, "weight")
    }
    val headRef = remember(monthAge, isMale) {
        if (monthAge < 0) null
        else getReferenceRange(monthAge, isMale, "head")
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
            BabyTopBar(title = if (isEditMode) "编辑成长指标" else "成长指标", onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 测量日期时间选择
            BabyCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("测量时间", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(Radius.sm),
                                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                            ) {
                                Text("改日期", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(Radius.sm),
                                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                            ) {
                                Text("改时间", fontSize = 13.sp)
                            }
                        }
                    }
                    
                    Text(
                        SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINESE).format(Date(recordTime)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    
                    if (monthAge >= 0) {
                        Text(
                            "宝宝 ${monthAge}个月",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // 上次记录
            lastRecord?.let { last ->
                BabyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text("📋 上次记录", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            last.height?.let { Text("身高: ${it}cm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                            last.weight?.let { Text("体重: ${it}kg", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                            last.headCircumference?.let { Text("头围: ${it}cm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                    }
                }
            }

            // 参考值提示
            if (heightRef != null || weightRef != null) {
                BabyCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        val genderLabel = if (isMale) "男宝" else "女宝"
                        Text(
                            "📊 ${monthAge}个月${genderLabel}参考范围 (WHO)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        heightRef?.let {
                            Text(
                                "身高: ${it.first}~${it.second} cm (P3~P97)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            )
                        }
                        weightRef?.let {
                            Text(
                                "体重: ${it.first}~${it.second} kg (P3~P97)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            )
                        }
                        headRef?.let {
                            Text(
                                "头围: ${it.first}~${it.second} cm (P3~P97)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = height, onValueChange = { height = it },
                label = { Text("身高 (cm) — 可选") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md),
                supportingText = heightRef?.let {
                    { Text("参考: ${it.first}~${it.second} cm", color = TextHint, fontSize = 11.sp) }
                },
            )
            OutlinedTextField(
                value = weight, onValueChange = { weight = it },
                label = { Text("体重 (kg) — 可选") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md),
                supportingText = weightRef?.let {
                    { Text("参考: ${it.first}~${it.second} kg", color = TextHint, fontSize = 11.sp) }
                },
            )
            OutlinedTextField(
                value = headCircumference, onValueChange = { headCircumference = it },
                label = { Text("头围 (cm) — 可选") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md),
                supportingText = headRef?.let {
                    { Text("参考: ${it.first}~${it.second} cm", color = TextHint, fontSize = 11.sp) }
                },
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md), minLines = 2
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            PrimaryButton(
                text = if (isEditMode) "保存修改" else "保存记录",
                onClick = {
                    if (isEditMode && editRecord != null) {
                        viewModel.updateRecord(
                            record = editRecord!!,
                            height = height.toFloatOrNull(),
                            weight = weight.toFloatOrNull(),
                            headCircumference = headCircumference.toFloatOrNull(),
                            note = note,
                            recordTime = recordTime,
                        ) {
                            Toast.makeText(context, "记录已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else {
                        viewModel.saveRecord(
                            height = height.toFloatOrNull(),
                            weight = weight.toFloatOrNull(),
                            headCircumference = headCircumference.toFloatOrNull(),
                            note = note,
                            recordTime = recordTime,
                        ) {
                            Toast.makeText(context, "成长记录已保存", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 根据月龄获取 WHO 参考值范围 (P3~P97)
 */
private fun getReferenceRange(
    monthAge: Int,
    isMale: Boolean,
    type: String
): Pair<Float, Float>? {
    val data = when (type) {
        "height" -> if (isMale) GrowthCurveData.BOY_HEIGHT else GrowthCurveData.GIRL_HEIGHT
        "weight" -> if (isMale) GrowthCurveData.BOY_WEIGHT else GrowthCurveData.GIRL_WEIGHT
        "head" -> if (isMale) GrowthCurveData.BOY_HEAD else GrowthCurveData.GIRL_HEAD
        else -> return null
    }
    val clampedAge = monthAge.coerceIn(0, 36)
    val lowerKey = data.keys.filter { it <= clampedAge }.maxOrNull() ?: return null
    val upperKey = data.keys.filter { it >= clampedAge }.minOrNull() ?: return null
    val lower = data[lowerKey] ?: return null
    val upper = data[upperKey] ?: return null

    if (lowerKey == upperKey) {
        return Pair(lower.p3, lower.p97)
    }
    val ratio = (clampedAge - lowerKey).toFloat() / (upperKey - lowerKey)
    val p3 = lower.p3 + (upper.p3 - lower.p3) * ratio
    val p97 = lower.p97 + (upper.p97 - lower.p97) * ratio
    return Pair(
        (p3 * 10).toInt() / 10f,
        (p97 * 10).toInt() / 10f
    )
}
