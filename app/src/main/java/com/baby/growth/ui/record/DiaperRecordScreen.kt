package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = {
                    Column {
                        Text("纸尿裤记录")
                        if (lastRecord != null) {
                            val relativeTime = DateUtils.formatRelativeTime(lastRecord!!.recordTime)
                            Text("上次：$relativeTime", fontSize = 11.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 类型选择
            Text("类型", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("pee" to "💧 小便", "poo" to "💩 大便", "both" to "混合").forEach { (value, label) ->
                    FilterChip(selected = diaperType == value, onClick = {
                        diaperType = value
                        if (value == "pee") { pooColor = ""; pooShape = "" }
                    }, label = { Text(label) })
                }
            }

            // 红屁屁开关
            Text("红屁屁", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !hasRash, onClick = { hasRash = false }, label = { Text("正常") })
                FilterChip(selected = hasRash, onClick = { hasRash = true }, label = { Text("有红屁屁 😣") })
            }

            // 大便颜色选择（仅大便/混合时显示）
            if (showPooFields) {
                Text("大便颜色", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecordTypes.POO_COLORS.forEach { (name, hexColor) ->
                        FilterChip(
                            selected = pooColor == name,
                            onClick = { pooColor = name },
                            label = {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape)
                                        .background(Color(hexColor.removePrefix("#").toLong(16) or 0xFF000000)))
                                    Text(name, fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }

                // 大便形状选择
                Text("大便形状", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecordTypes.POO_SHAPES.chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { shape ->
                                FilterChip(
                                    selected = pooShape == shape,
                                    onClick = { pooShape = shape },
                                    label = { Text(shape, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
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
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("保存记录", fontWeight = FontWeight.Bold) }
        }
    }
}
