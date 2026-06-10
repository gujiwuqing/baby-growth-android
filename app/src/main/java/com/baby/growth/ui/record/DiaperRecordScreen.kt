package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.baby.growth.data.entity.DiaperRecord
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.components.FilterTag
import com.baby.growth.ui.components.PrimaryButton
import com.baby.growth.ui.theme.BabyGrowthTheme
import com.baby.growth.ui.theme.Radius
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.RecordTypes
import kotlinx.coroutines.launch

class DiaperViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<DiaperRecord?>(null)
    val lastRecord: State<DiaperRecord?> = _lastRecord

    init {
        viewModelScope.launch {
            _lastRecord.value = db.diaperDao().getLatest()
        }
    }

    fun saveRecord(
        type: String, hasRash: Int, pooColor: String, pooShape: String,
        color: String, note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.diaperDao().insert(
                DiaperRecord(
                    uniqueId = DateUtils.generateUniqueId("diaper"),
                    type = type, hasRash = hasRash,
                    pooColor = pooColor, pooShape = pooShape,
                    color = color, note = note
                )
            )
            onSuccess()
        }
    }
}

@Composable
fun DiaperRecordScreen(
    navController: NavController,
    viewModel: DiaperViewModel = viewModel()
) {
    val context = LocalContext.current
    var diaperType by remember { mutableStateOf("pee") }
    var hasRash by remember { mutableStateOf(false) }
    var pooColor by remember { mutableStateOf("") }
    var pooShape by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val showPooFields = diaperType == "poo" || diaperType == "both"
    val lastRecord by viewModel.lastRecord

    Scaffold(
        topBar = {
            BabyTopBar(
                title = "换尿布",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // 类型选择
            Text("类型", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("pee" to "💧 小便", "poo" to "💩 大便", "both" to "混合").forEach { (value, label) ->
                    FilterTag(
                        text = label,
                        selected = diaperType == value,
                        onClick = {
                            diaperType = value
                            if (value == "pee") { pooColor = ""; pooShape = "" }
                        }
                    )
                }
            }

            // 红屁屁开关
            Text("红屁屁", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterTag(
                    text = "正常",
                    selected = !hasRash,
                    onClick = { hasRash = false }
                )
                FilterTag(
                    text = "有红屁屁 😣",
                    selected = hasRash,
                    onClick = { hasRash = true }
                )
            }

            // 大便颜色选择（仅大便/混合时显示）
            if (showPooFields) {
                Text("大便颜色", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    RecordTypes.POO_COLORS.forEach { (name, hexColor) ->
                        FilterTag(
                            text = name,
                            selected = pooColor == name,
                            onClick = { pooColor = name }
                        )
                    }
                }

                // 大便形状选择
                Text("大便形状", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    RecordTypes.POO_SHAPES.chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            row.forEach { shape ->
                                FilterTag(
                                    text = shape,
                                    selected = pooShape == shape,
                                    onClick = { pooShape = shape }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md), minLines = 2
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            PrimaryButton(
                text = "保存记录",
                onClick = {
                    viewModel.saveRecord(
                        type = diaperType,
                        hasRash = if (hasRash) 1 else 0,
                        pooColor = pooColor,
                        pooShape = pooShape,
                        color = pooColor,
                        note = note
                    ) {
                        Toast.makeText(context, "尿布记录已保存", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
