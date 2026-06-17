package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.FoodRecord
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.components.FilterTag
import com.baby.growth.ui.components.PrimaryButton
import com.baby.growth.ui.theme.Radius
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.components.DateTimeInput

class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<FoodRecord?>(null)
    val lastRecord: State<FoodRecord?> = _lastRecord

    private val _editRecord = mutableStateOf<FoodRecord?>(null)
    val editRecord: State<FoodRecord?> = _editRecord

    init {
        viewModelScope.launch {
            _lastRecord.value = db.foodDao().getLatest()
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            _editRecord.value = db.foodDao().getById(id)
        }
    }

    fun saveRecord(
        foodName: String, category: String, amount: String, unit: String,
        reaction: String, note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.foodDao().insert(
                FoodRecord(
                    uniqueId = DateUtils.generateUniqueId("food"),
                    foodName = foodName, category = category,
                    amount = amount, unit = unit,
                    reaction = reaction, note = note
                )
            )
            onSuccess()
        }
    }

    fun updateRecord(
        record: FoodRecord, foodName: String, category: String,
        amount: String, unit: String, reaction: String, note: String,
        recordTime: Long, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.foodDao().update(
                record.copy(
                    foodName = foodName, category = category,
                    amount = amount, unit = unit,
                    reaction = reaction, note = note, recordTime = recordTime
                )
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRecordScreen(
    navController: NavController,
    editId: Long? = null,
    viewModel: FoodViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = editId != null
    val editRecord by viewModel.editRecord
    val lastRecord by viewModel.lastRecord

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }

    val lastRecordSubtitle = lastRecord?.let { last ->
        val relativeTime = DateUtils.formatRelativeTime(last.recordTime)
        val detail = "${last.foodName} ${last.amount}${last.unit}"
        "上次：$detail，$relativeTime"
    }

    var foodName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("grain") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var reaction by remember { mutableStateOf("normal") }
    var note by remember { mutableStateOf("") }
    var recordTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // 编辑模式回填数据
    LaunchedEffect(editRecord) {
        editRecord?.let { record ->
            foodName = record.foodName
            category = record.category
            amount = record.amount
            unit = record.unit
            reaction = record.reaction
            note = record.note
            recordTime = record.recordTime
        }
    }

    Scaffold(
        topBar = {
            BabyTopBar(
                title = if (isEditMode) "编辑辅食记录" else "辅食记录",
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
            DateTimeInput(
                dateTime = recordTime,
                onDateTimeChange = { recordTime = it },
            )

            OutlinedTextField(
                value = foodName, onValueChange = { foodName = it },
                label = { Text("食物名称") }, modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md)
            )

            Text("食物分类", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("grain" to "谷物", "vegetable" to "蔬菜", "fruit" to "水果").forEach { (v, l) ->
                    FilterTag(
                        text = l,
                        selected = category == v,
                        onClick = { category = v }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("meat" to "肉类", "egg" to "蛋类", "dairy" to "奶制品").forEach { (v, l) ->
                    FilterTag(
                        text = l,
                        selected = category == v,
                        onClick = { category = v }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("食用量") }, modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md)
                )
            }

            Text("单位", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("g", "ml", "勺", "个", "片").forEach { u ->
                    FilterTag(
                        text = u,
                        selected = unit == u,
                        onClick = { unit = u }
                    )
                }
            }

            Text("反应", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("normal" to "✅ 正常", "allergy" to "⚠️ 过敏", "refuse" to "❌ 拒绝").forEach { (v, l) ->
                    FilterTag(
                        text = l,
                        selected = reaction == v,
                        onClick = { reaction = v }
                    )
                }
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md), minLines = 2
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            PrimaryButton(
                text = if (isEditMode) "保存修改" else "保存记录",
                onClick = {
                    if (isEditMode && editRecord != null) {
                        viewModel.updateRecord(editRecord!!, foodName, category, amount, unit, reaction, note, recordTime) {
                            Toast.makeText(context, "记录已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    } else {
                        viewModel.saveRecord(foodName, category, amount, unit, reaction, note) {
                            Toast.makeText(context, "辅食记录已保存", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}
