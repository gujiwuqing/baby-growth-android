package com.baby.growth.ui.record

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.SupplementRecord
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.components.DateTimeInput
import com.baby.growth.ui.components.PrimaryButton
import com.baby.growth.ui.components.FilterTag
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.ui.theme.Radius
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SupplementItem(
    val name: String,
    val dosage: String
)

class SupplementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<SupplementRecord?>(null)
    val lastRecord: State<SupplementRecord?> = _lastRecord

    private val _editRecord = mutableStateOf<SupplementRecord?>(null)
    val editRecord: State<SupplementRecord?> = _editRecord

    private val _recentItems = mutableStateOf<List<SupplementItem>>(emptyList())
    val recentItems: State<List<SupplementItem>> = _recentItems

    init {
        viewModelScope.launch {
            _lastRecord.value = db.supplementDao().getLatest()
            val allRecords = db.supplementDao().getAll().first()
            val recent = allRecords.take(10)
                .map { SupplementItem(it.supplementName, it.dosage) }
                .distinctBy { it.name + it.dosage }
                .take(5)
            _recentItems.value = recent
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            _editRecord.value = db.supplementDao().getById(id)
        }
    }

    fun saveRecords(items: List<SupplementItem>, note: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            items.forEach { item ->
                db.supplementDao().insert(
                    SupplementRecord(
                        uniqueId = DateUtils.generateUniqueId("supplement"),
                        supplementName = item.name,
                        supplementType = "vitamin",
                        brand = "",
                        dosage = item.dosage,
                        note = note
                    )
                )
            }
            onSuccess()
        }
    }

    fun updateRecord(
        record: SupplementRecord, name: String, dosage: String, note: String,
        recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.supplementDao().update(
                record.copy(supplementName = name, dosage = dosage, note = note, recordTime = recordTime)
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SupplementRecordScreen(
    navController: NavController,
    editId: Long? = null,
    viewModel: SupplementViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = editId != null
    val editRecord by viewModel.editRecord
    val lastRecord by viewModel.lastRecord
    val recentItems by viewModel.recentItems

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }

    val lastRecordSubtitle = lastRecord?.let { last ->
        val relativeTime = DateUtils.formatRelativeTime(last.recordTime)
        val nameLabel = last.supplementName.ifEmpty { last.supplementType }
        val detail = if (last.dosage.isNotEmpty()) "$nameLabel ${last.dosage}" else nameLabel
        "上次：$detail，$relativeTime"
    }

    // 当前添加的补剂列表
    var supplementItems by remember { mutableStateOf(listOf<SupplementItem>()) }
    var inputName by remember { mutableStateOf("") }
    var inputDosage by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showNoteField by remember { mutableStateOf(false) }

    // 记录时间（可修改）
    var recordTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(editRecord) {
        editRecord?.let { record ->
            recordTime = record.recordTime
        }
    }
    // 快捷补剂：名称 + 预填用量，一键添加
    val quickSupplements = listOf(
        SupplementItem("AD", "1滴"),
        SupplementItem("D3", "1滴"),
        SupplementItem("probiotic", "1袋"),
    )

    // 常见补剂（仅名称，需手动填用量）
    val commonSupplements = listOf("维生素AD", "维生素D3", "益生菌", "钙", "锌", "铁", "DHA")

    Scaffold(
        topBar = {
            BabyTopBar(
                title = if (isEditMode) "编辑营养补剂" else "营养补剂",
                subtitle = if (isEditMode) null else lastRecordSubtitle,
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 主体内容可滚动
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 白色卡片区域
                BabyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        // 开始时间
                        DateTimeInput(
                            dateTime = recordTime,
                            onDateTimeChange = { recordTime = it },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 营养补剂标题 + 添加按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("营养补剂", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            OutlinedButton(
                                onClick = {
                                    if (inputName.isNotBlank()) {
                                        supplementItems = supplementItems + SupplementItem(inputName, inputDosage)
                                        inputName = ""
                                        inputDosage = ""
                                    }
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(Radius.lg),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text("添加", fontSize = 13.sp)
                            }
                        }

                        // 输入框：补剂名称 | 用量
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radius.sm))
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                placeholder = { Text("输入补剂名称", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                ),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            OutlinedTextField(
                                value = inputDosage,
                                onValueChange = { inputDosage = it },
                                placeholder = { Text("用量", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                                modifier = Modifier.width(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                ),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            if (inputName.isNotEmpty() || inputDosage.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputName = ""; inputDosage = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "清除",
                                        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 快捷一键添加（带预填用量）
                        Text("快捷添加", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            quickSupplements.forEach { item ->
                                val displayName = when (item.name) {
                                    "AD" -> "维生素AD"; "D3" -> "维生素D3"
                                    "probiotic" -> "益生菌"
                                    else -> item.name
                                }
                                FilterTag(
                                    text = "$displayName ${item.dosage}",
                                    selected = supplementItems.any { it.name == item.name && it.dosage == item.dosage },
                                    onClick = {
                                        // 已存在则移除，否则一键添加
                                        val exists = supplementItems.any { it.name == item.name && it.dosage == item.dosage }
                                        supplementItems = if (exists) {
                                            supplementItems.filterNot { it.name == item.name && it.dosage == item.dosage }
                                        } else {
                                            supplementItems + item
                                        }
                                    }
                                )
                            }
                        }

                        // 常见补剂标签（仅填名称，手动填用量）
                        Text("其他补剂", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            commonSupplements.forEach { name ->
                                FilterTag(
                                    text = name,
                                    selected = false,
                                    onClick = { inputName = name }
                                )
                            }
                        }

                        // 最近添加
                        if (recentItems.isNotEmpty()) {
                            Text("最近添加", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                recentItems.forEach { item ->
                                    val displayName = when (item.name) {
                                        "AD" -> "维生素AD"; "D3" -> "维生素D3"
                                        "calcium" -> "钙"; "probiotic" -> "益生菌"
                                        "iron" -> "铁"; "zinc" -> "锌"
                                        else -> item.name
                                    }
                                    FilterTag(
                                        text = "$displayName｜${item.dosage}",
                                        selected = false,
                                        onClick = {
                                            supplementItems = supplementItems + item
                                        }
                                    )
                                }
                            }
                        }

                        // 已添加的补剂列表
                        if (supplementItems.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("已添加", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            supplementItems.forEachIndexed { index, item ->
                                val displayName = when (item.name) {
                                    "AD" -> "维生素AD"; "D3" -> "维生素D3"
                                    "calcium" -> "钙"; "probiotic" -> "益生菌"
                                    "iron" -> "铁"; "zinc" -> "锌"
                                    else -> item.name
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$displayName  ${item.dosage}", fontSize = 14.sp)
                                    IconButton(
                                        onClick = {
                                            supplementItems = supplementItems.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "删除",
                                            modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // 备注区域
                if (showNoteField) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.md),
                        minLines = 2
                    )
                } else {
                    TextButton(
                        onClick = { showNoteField = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("📝 备注", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 底部保存按钮
            PrimaryButton(
                text = if (isEditMode) "保存修改" else "保存",
                onClick = {
                    if (isEditMode && editRecord != null) {
                        viewModel.updateRecord(
                            record = editRecord!!,
                            name = editRecord!!.supplementName,
                            dosage = editRecord!!.dosage,
                            note = note,
                            recordTime = recordTime
                        ) {
                            Toast.makeText(context, "记录已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else {
                        val finalItems = if (inputName.isNotBlank()) {
                            supplementItems + SupplementItem(inputName, inputDosage)
                        } else {
                            supplementItems
                        }
                        if (finalItems.isEmpty()) {
                            Toast.makeText(context, "请至少添加一条补剂", Toast.LENGTH_SHORT).show()
                            return@PrimaryButton
                        }
                        viewModel.saveRecords(finalItems, note) {
                            Toast.makeText(context, "营养记录已保存", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
        }
    }
}
