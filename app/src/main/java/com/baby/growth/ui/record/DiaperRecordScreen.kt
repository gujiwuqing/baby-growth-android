package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.baby.growth.ui.components.DateTimeInput
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
        pooColor: String, pooShape: String, note: String,
        recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.diaperDao().update(
                record.copy(
                    type = type, hasRash = hasRash,
                    pooColor = pooColor, pooShape = pooShape,
                    note = note, recordTime = recordTime
                )
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    // 切换到大便/混合时自动填充常见默认值（编辑模式不覆盖已有值）
    LaunchedEffect(diaperType) {
        if ((diaperType == "poo" || diaperType == "both") && !isEditMode) {
            if (pooColor.isEmpty()) pooColor = "黄色"
            if (pooShape.isEmpty()) pooShape = "软便"
        }
    }
    var note by remember { mutableStateOf("") }
    var recordTime by remember { mutableStateOf(System.currentTimeMillis()) }

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
            DateTimeInput(
                dateTime = recordTime,
                onDateTimeChange = { recordTime = it },
                label = "更换时间",
            )

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
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        // 区域标题 - 增强视觉层次
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text("💩", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column {
                                Text("大便详情", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "记录宝宝的大便特征，帮助了解消化状况",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 颜色选择 — 改进为更清晰的网格布局
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("颜色", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (pooColor.isNotEmpty()) {
                                    TextButton(onClick = { pooColor = "" }) {
                                        Text("清除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            
                            // 使用 FlowRow 替代固定行列，自动换行更灵活
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                RecordTypes.POO_COLORS.forEach { (name, hexColor) ->
                                    PooColorTag(
                                        name = name,
                                        hexColor = hexColor,
                                        selected = pooColor == name,
                                        onClick = { pooColor = if (pooColor == name) "" else name },
                                        modifier = Modifier.wrapContentSize()
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 形状选择 — 改进分组展示，增强视觉区分
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("形状", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (pooShape.isNotEmpty()) {
                                    TextButton(onClick = { pooShape = "" }) {
                                        Text("清除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            
                            // 常见形状 - 使用浅色背景卡片包裹
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(Radius.sm),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(Spacing.md),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("常见", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        RecordTypes.POO_SHAPES.take(6).forEach { shape ->
                                            FilterTag(
                                                text = shape,
                                                selected = pooShape == shape,
                                                onClick = { pooShape = if (pooShape == shape) "" else shape },
                                                selectedColor = RecordColor.Diaper
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // 异常形状（需关注）- 使用警告色背景
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(Radius.sm),
                                color = Color(0xFFFFF3E0).copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(Spacing.md),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF87171))
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text(
                                            "异常（需关注）",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        RecordTypes.POO_SHAPES.drop(6).forEach { shape ->
                                            val isWarning = shape in listOf("水样", "脓血便", "柏油样", "黏液便")
                                            FilterTag(
                                                text = shape,
                                                selected = pooShape == shape,
                                                onClick = { pooShape = if (pooShape == shape) "" else shape },
                                                selectedColor = if (isWarning) Color(0xFFF87171) else RecordColor.Diaper
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 皮肤状态卡片 - 增强视觉表现
            BabyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text("", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text("皮肤状态", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "检查宝宝臀部是否有红屁屁",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    FilterTag(
                        text = "✅ 正常",
                        selected = !hasRash,
                        onClick = { hasRash = false },
                        selectedColor = RecordColor.Diaper,
                        modifier = Modifier.weight(1f)
                    )
                    FilterTag(
                        text = "😣 红屁屁",
                        selected = hasRash,
                        onClick = { hasRash = true },
                        selectedColor = Color(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 备注 - 增强视觉层次
            BabyCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("备注", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "选填",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { 
                            Text(
                                "可以记录宝宝的特殊情况，如：哭闹、食欲变化等",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.md),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // 保存按钮 - 增强视觉重要性
            PrimaryButton(
                text = if (isEditMode) " 保存修改" else "✅ 保存记录",
                onClick = {
                    if (isEditMode && editRecord != null) {
                        viewModel.updateRecord(
                            record = editRecord!!,
                            type = diaperType,
                            hasRash = if (hasRash) 1 else 0,
                            pooColor = pooColor,
                            pooShape = pooShape,
                            note = note,
                            recordTime = recordTime
                        ) {
                            Toast.makeText(context, "✓ 记录已更新", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "✓ 尿布记录已保存", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

/**
 * 大便颜色标签 - 带色点指示，支持权重布局
 */
@Composable
private fun PooColorTag(
    name: String,
    hexColor: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = try {
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.outline
    }

    val selectedColor = RecordColor.Diaper
    val backgroundColor = if (selected) selectedColor.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) selectedColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (selected) Modifier.border(2.dp, selectedColor, CircleShape)
                        else Modifier
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
