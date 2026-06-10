package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.ui.theme.Radius
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun saveRecord(
        height: Float?, weight: Float?, headCircumference: Float?,
        note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.growthDao().insert(
                GrowthRecord(
                    uniqueId = DateUtils.generateUniqueId("growth"),
                    height = height, weight = weight,
                    headCircumference = headCircumference, note = note
                )
            )
            onSuccess()
        }
    }

    fun updateRecord(
        record: GrowthRecord, height: Float?, weight: Float?,
        headCircumference: Float?, note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.growthDao().update(
                record.copy(height = height, weight = weight, headCircumference = headCircumference, note = note)
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

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }

    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var headCircumference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // 编辑模式回填数据
    LaunchedEffect(editRecord) {
        editRecord?.let { record ->
            height = record.height?.toString() ?: ""
            weight = record.weight?.toString() ?: ""
            headCircumference = record.headCircumference?.toString() ?: ""
            note = record.note
        }
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

            OutlinedTextField(
                value = height, onValueChange = { height = it },
                label = { Text("身高 (cm) — 可选") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md)
            )
            OutlinedTextField(
                value = weight, onValueChange = { weight = it },
                label = { Text("体重 (kg) — 可选") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md)
            )
            OutlinedTextField(
                value = headCircumference, onValueChange = { headCircumference = it },
                label = { Text("头围 (cm) — 可选") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md)
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
                            note = note
                        ) {
                            Toast.makeText(context, "记录已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else {
                        viewModel.saveRecord(
                            height = height.toFloatOrNull(),
                            weight = weight.toFloatOrNull(),
                            headCircumference = headCircumference.toFloatOrNull(),
                            note = note
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
