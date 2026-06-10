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

    init {
        viewModelScope.launch {
            _lastRecord.value = db.growthDao().getLatest()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthRecordScreen(
    navController: NavController,
    viewModel: GrowthRecordViewModel = viewModel()
) {
    val context = LocalContext.current
    val lastRecord by viewModel.lastRecord.collectAsState()
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var headCircumference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            BabyTopBar(title = "成长指标", onBack = { navController.popBackStack() })
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
                text = "保存记录",
                onClick = {
                    viewModel.saveRecord(
                        height = height.toFloatOrNull(),
                        weight = weight.toFloatOrNull(),
                        headCircumference = headCircumference.toFloatOrNull(),
                        note = note
                    ) {
                        Toast.makeText(context, "成长记录已保存", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
