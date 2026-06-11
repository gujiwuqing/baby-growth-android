package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.DiaperRecord
import com.baby.growth.ui.components.BabyAccentCard
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.components.FilterTag
import com.baby.growth.ui.components.PrimaryButton
import com.baby.growth.ui.theme.BabyGrowthTheme
import com.baby.growth.ui.theme.Radius
import com.baby.growth.ui.theme.RecordColor
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.RecordTypes
import kotlinx.coroutines.launch
import java.util.Calendar

class DiaperViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<DiaperRecord?>(null)
    val lastRecord: State<DiaperRecord?> = _lastRecord

    private val _editRecord = mutableStateOf<DiaperRecord?>(null)
    val editRecord: State<DiaperRecord?> = _editRecord

    init {
        viewModelScope.launch {
            _lastRecord.value = db.diaperDao().getLatest()
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            _editRecord.value = db.diaperDao().getById(id)
        }
    }

    fun saveRecord(
        type: String, hasRash: Int, pooColor: String, pooShape: String,
        color: String, note: String, recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.diaperDao().insert(
                DiaperRecord(
                    uniqueId = DateUtils.generateUniqueId("diaper"),
                    type = type, hasRash = hasRash,
                    pooColor = pooColor, pooShape = pooShape,
                    color = color, note = note,
                    recordTime = recordTime
                )
            )
            onSuccess()
        }
    }

    fun updateRecord(
        record: DiaperRecord, type: String, hasRash: Int,
        pooColor: String, pooShape: String, note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.diaperDao().update(
                record.copy(
                    type = type, hasRash = hasRash,
                    pooColor = pooColor, pooShape = pooShape,
                    note = note
                )
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaperRecordScreen(
    navController: NavController,
    editId: Long? = null,
    viewModel: DiaperViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = editId != null
    val editRecord by viewModel.editRecord

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }

    var diaperType by remember { mutableStateOf("pee") }
    var hasRash by remember { mutableStateOf(false) }
    var pooColor by remember { mutableStateOf("") }
    var pooShape by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var recordTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 编辑模式回填数据
    LaunchedEffect(editRecord) {
        editRecord?.let { record ->
            diaperType = record.type
            hasRash = record.hasRash == 1
            pooColor = record.pooColor
            pooShape = record.pooShape
            note = record.note
            recordTime = record.recordTime
        }
    }

    val showPooFields = diaperType == "poo" || diaperType == "both"
    val lastRecord by viewModel.lastRecord

    val lastRecordSubtitle = lastRecord?.let { last ->
        val typeLabel = when (last.type) { "pee" -> "小便💧"; "poo" -> "大便💩"; else -> "混合💩💧" }
        val relativeTime = DateUtils.formatRelativeTime(last.recordTime)
        val parts = mutableListOf(typeLabel)
        if (last.hasRash == 1) parts.add("红屁屁")
        "上次：${parts.joinToString(" · ")}，$relativeTime"
    }

    Scaffold(
        topBar = {
            BabyTopBar(
                title = if (isEditMode) "编辑换纸尿裤记录" else "换纸尿裤",
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
            // 快捷一键记录 - 小便正常（最高频场景）
            if (!showPooFields && !hasRash && note.isEmpty() && DateUtils.isSameDay(recordTime, System.currentTimeMillis())) {
                BabyAccentCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💧", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "快捷记录：小便正常",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "一键保存，无需填写其他信息",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                        TextButton(onClick = {
                            viewModel.saveRecord(
                                type = "pee", hasRash = 0,
                                pooColor = "", pooShape = "",
                                color = "", note = "",
                                recordTime = recordTime
                            ) {
                                Toast.makeText(context, "已记录小便", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }) {
                            Text("记录", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 更换时间选择
            BabyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("更换时间", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            DateUtils.formatDateTime(recordTime),
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

            // 日期选择器
            if (showDatePicker) {
                val cal = Calendar.getInstance().apply { timeInMillis = recordTime }
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = recordTime,
                    yearRange = IntRange(cal.get(Calendar.YEAR) - 1, cal.get(Calendar.YEAR))
                )
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            val newCal = Calendar.getInstance().apply { timeInMillis = recordTime }
                            val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            newCal.set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                            newCal.set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                            newCal.set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                            recordTime = newCal.timeInMillis
                        }
                        showDatePicker = false
                    }) { Text("确认") }
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
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showTimePicker = false }) { Text("取消") }
                    TextButton(onClick = {
                        val newCal = Calendar.getInstance().apply { timeInMillis = recordTime }
                        newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        newCal.set(Calendar.MINUTE, timePickerState.minute)
                        recordTime = newCal.timeInMillis
                        showTimePicker = false
                    }) { Text("确认") }
                }
            }

            // 类型选择卡片
            BabyCard {
                Text("尿布类型", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    listOf("pee" to "💧 小便", "poo" to "💩 大便", "both" to "💩💧 混合").forEach { (value, label) ->
                        FilterTag(
                            text = label,
                            selected = diaperType == value,
                            onClick = {
                                diaperType = value
                                if (value == "pee") { pooColor = ""; pooShape = "" }
                            },
                            selectedColor = RecordColor.Diaper
                        )
                    }
                }
            }

            // 大便详情卡片（仅大便/混合时显示）
            if (showPooFields) {
                BabyCard {
                    Text("大便详情", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(Spacing.md))

                    // 大便颜色 - 带色点
                    Text("颜色", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        RecordTypes.POO_COLORS.forEach { (name, hexColor) ->
                            PooColorTag(
                                name = name,
                                hexColor = hexColor,
                                selected = pooColor == name,
                                onClick = { pooColor = name }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 大便形状 - 按紧急程度分组
                    Text("形状", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        RecordTypes.POO_SHAPES.chunked(5).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                row.forEach { shape ->
                                    FilterTag(
                                        text = shape,
                                        selected = pooShape == shape,
                                        onClick = { pooShape = shape },
                                        selectedColor = RecordColor.Diaper
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 皮肤状态卡片
            BabyCard {
                Text("皮肤状态", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    FilterTag(
                        text = "✅ 正常",
                        selected = !hasRash,
                        onClick = { hasRash = false },
                        selectedColor = RecordColor.Diaper
                    )
                    FilterTag(
                        text = "😣 红屁屁",
                        selected = hasRash,
                        onClick = { hasRash = true },
                        selectedColor = Color(0xFFF87171)
                    )
                }
            }

            // 备注
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注（选填）") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md), minLines = 2
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            PrimaryButton(
                text = if (isEditMode) "保存修改" else "保存记录",
                onClick = {
                    if (isEditMode && editRecord != null) {
                        viewModel.updateRecord(
                            record = editRecord!!,
                            type = diaperType,
                            hasRash = if (hasRash) 1 else 0,
                            pooColor = pooColor,
                            pooShape = pooShape,
                            note = note
                        ) {
                            Toast.makeText(context, "记录已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else {
                        viewModel.saveRecord(
                            type = diaperType,
                            hasRash = if (hasRash) 1 else 0,
                            pooColor = pooColor,
                            pooShape = pooShape,
                            color = pooColor,
                            note = note,
                            recordTime = recordTime
                        ) {
                            Toast.makeText(context, "尿布记录已保存", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}

/**
 * 大便颜色标签 - 带色点指示
 */
@Composable
private fun PooColorTag(
    name: String,
    hexColor: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = try {
        android.graphics.Color.parseColor(hexColor)
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.outline
    }

    val selectedColor = RecordColor.Diaper
    val backgroundColor = if (selected) selectedColor.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) selectedColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.full),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
