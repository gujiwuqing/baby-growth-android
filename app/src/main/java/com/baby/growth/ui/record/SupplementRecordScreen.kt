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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.SupplementRecord
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SupplementItem(
    val name: String,
    val dosage: String
)

class SupplementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<SupplementRecord?>(null)
    val lastRecord: State<SupplementRecord?> = _lastRecord

    private val _recentItems = mutableStateOf<List<SupplementItem>>(emptyList())
    val recentItems: State<List<SupplementItem>> = _recentItems

    init {
        viewModelScope.launch {
            _lastRecord.value = db.supplementDao().getLatest()
            // 加载最近添加的补剂（去重，最多5条）
            val allRecords = db.supplementDao().getAll().first()
            val recent = allRecords.take(10)
                .map { SupplementItem(it.supplementName, it.dosage) }
                .distinctBy { it.name + it.dosage }
                .take(5)
            _recentItems.value = recent
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SupplementRecordScreen(
    navController: NavController,
    viewModel: SupplementViewModel = viewModel()
) {
    val context = LocalContext.current
    val lastRecord by viewModel.lastRecord
    val recentItems by viewModel.recentItems

    // 当前添加的补剂列表
    var supplementItems by remember { mutableStateOf(listOf<SupplementItem>()) }
    var inputName by remember { mutableStateOf("") }
    var inputDosage by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showNoteField by remember { mutableStateOf(false) }

    // 当前时间
    val currentTime = remember { System.currentTimeMillis() }
    val dateTimeText = remember {
        SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(currentTime))
    }

    // 常见补剂
    val commonSupplements = listOf("维生素AD", "维生素D3", "益生菌", "钙", "锌", "铁", "DHA")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("营养补剂", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        if (lastRecord != null) {
                            Text(
                                "上次：${DateUtils.formatRelativeTime(lastRecord!!.recordTime)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 白色卡片区域
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 开始时间
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("开始时间", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                "$dateTimeText >",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0))

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
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加", fontSize = 13.sp)
                            }
                        }

                        // 输入框：补剂名称 | 用量
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                placeholder = { Text("输入补剂名称", color = Color.Gray, fontSize = 14.sp) },
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
                                    .background(Color(0xFFE0E0E0))
                            )
                            OutlinedTextField(
                                value = inputDosage,
                                onValueChange = { inputDosage = it },
                                placeholder = { Text("用量", color = Color.Gray, fontSize = 14.sp) },
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
                                        modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                            }
                        }

                        // 常见补剂标签
                        Text("常见补剂", fontSize = 13.sp, color = Color.Gray)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            commonSupplements.forEach { name ->
                                SuggestionChip(
                                    onClick = { inputName = name },
                                    label = { Text(name, fontSize = 13.sp) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        // 最近添加
                        if (recentItems.isNotEmpty()) {
                            Text("最近添加", fontSize = 13.sp, color = Color.Gray)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentItems.forEach { item ->
                                    val displayName = when (item.name) {
                                        "AD" -> "维生素AD"; "D3" -> "维生素D3"
                                        "calcium" -> "钙"; "probiotic" -> "益生菌"
                                        "iron" -> "铁"; "zinc" -> "锌"
                                        else -> item.name
                                    }
                                    SuggestionChip(
                                        onClick = {
                                            supplementItems = supplementItems + item
                                        },
                                        label = {
                                            Text("$displayName｜${item.dosage}", fontSize = 13.sp)
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }

                        // 已添加的补剂列表
                        if (supplementItems.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                            Text("已添加", fontSize = 13.sp, color = Color.Gray)
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
                                            modifier = Modifier.size(14.dp), tint = Color.Gray)
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
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                } else {
                    TextButton(
                        onClick = { showNoteField = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("📷 备注", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            // 底部保存按钮
            Button(
                onClick = {
                    // 如果输入框还有未添加的内容，也一并保存
                    val finalItems = if (inputName.isNotBlank()) {
                        supplementItems + SupplementItem(inputName, inputDosage)
                    } else {
                        supplementItems
                    }
                    if (finalItems.isEmpty()) {
                        Toast.makeText(context, "请至少添加一条补剂", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveRecords(finalItems, note) {
                        Toast.makeText(context, "营养记录已保存", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
