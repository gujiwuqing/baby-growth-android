package com.baby.growth.ui.record

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.baby.growth.data.entity.FeedRecord
import com.baby.growth.service.FeedingTimerService
import com.baby.growth.ui.components.*
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.BreastfeedingTimer
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _lastRecord = mutableStateOf<FeedRecord?>(null)
    val lastRecord: State<FeedRecord?> = _lastRecord

    init {
        viewModelScope.launch {
            _lastRecord.value = db.feedDao().getLatest()
        }
    }

    fun saveRecord(
        type: String, amount: Int, unit: String,
        leftDuration: Int, rightDuration: Int,
        startTime: Long, endTime: Long,
        side: String, note: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            db.feedDao().insert(
                FeedRecord(
                    uniqueId = DateUtils.generateUniqueId(type),
                    type = type, amount = amount, unit = unit,
                    leftDuration = leftDuration, rightDuration = rightDuration,
                    startTime = startTime, endTime = endTime,
                    side = side, note = note
                )
            )
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingRecordScreen(
    navController: NavController,
    viewModel: FeedingViewModel = viewModel()
) {
    val context = LocalContext.current
    var feedType by remember { mutableStateOf("breast") }
    var amount by remember { mutableStateOf("") }
    var side by remember { mutableStateOf("both") }
    var note by remember { mutableStateOf("") }

    // 从持久化计时器读取状态
    val timerState by BreastfeedingTimer.state.collectAsState()

    // 实时刷新计时显示
    var leftDisplaySeconds by remember { mutableStateOf(0) }
    var rightDisplaySeconds by remember { mutableStateOf(0) }

    // 每秒刷新计时显示
    LaunchedEffect(timerState.isRunning) {
        while (true) {
            leftDisplaySeconds = BreastfeedingTimer.getLeftTotalSeconds()
            rightDisplaySeconds = BreastfeedingTimer.getRightTotalSeconds()
            delay(1000)
        }
    }

    val lastRecord by viewModel.lastRecord

    val lastRecordSubtitle = lastRecord?.let { last ->
        val relativeTime = DateUtils.formatRelativeTime(last.recordTime)
        if (last.type == "breast") {
            val parts = mutableListOf<String>()
            if (last.leftDuration > 0) parts.add("左侧${last.leftDuration}min")
            if (last.rightDuration > 0) parts.add("右侧${last.rightDuration}min")
            val durationText = if (parts.isNotEmpty()) parts.joinToString("/") else "0min"
            "上次：$durationText，$relativeTime"
        } else {
            "上次：${last.amount}ml，$relativeTime"
        }
    }

    Scaffold(
        topBar = {
            BabyTopBar(title = "喂奶记录", subtitle = lastRecordSubtitle, onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text("喂养方式", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("breast" to "🤱 母乳", "formula" to "🍼 配方奶", "bottle" to "🍼 瓶喂母乳").forEach { (value, label) ->
                    FilterChip(selected = feedType == value, onClick = {
                        feedType = value
                    }, label = { Text(label) })
                }
            }

            if (feedType == "formula" || feedType == "bottle") {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("奶量 (ml)") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.md)
                )
            }

            if (feedType == "breast") {
                Text("哺乳计时", fontWeight = FontWeight.Bold)

                // 如果有计时器正在运行，显示醒目提示
                if (timerState.isRunning) {
                    BabyAccentCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                "正在计时 ${if (timerState.side == "left") "左侧" else "右侧"}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // 左侧计时
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("左侧", fontSize = 14.sp)
                        Text(
                            formatTimerDisplay(leftDisplaySeconds),
                            fontWeight = FontWeight.Bold, fontSize = 20.sp
                        )
                        if (!timerState.isRunning || timerState.side != "left") {
                            OutlinedButton(onClick = {
                                BreastfeedingTimer.start(context, "left")
                                FeedingTimerService.start(context, "left")
                            }) { Text("开始") }
                        } else {
                            Button(onClick = {
                                BreastfeedingTimer.stop(context)
                                FeedingTimerService.stop(context)
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )) { Text("停止") }
                        }
                    }
                    // 右侧计时
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("右侧", fontSize = 14.sp)
                        Text(
                            formatTimerDisplay(rightDisplaySeconds),
                            fontWeight = FontWeight.Bold, fontSize = 20.sp
                        )
                        if (!timerState.isRunning || timerState.side != "right") {
                            OutlinedButton(onClick = {
                                BreastfeedingTimer.start(context, "right")
                                FeedingTimerService.start(context, "right")
                            }) { Text("开始") }
                        } else {
                            Button(onClick = {
                                BreastfeedingTimer.stop(context)
                                FeedingTimerService.stop(context)
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )) { Text("停止") }
                        }
                    }
                }

                // 手动输入/微调
                Text("或手动微调时长（分钟）", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = (leftDisplaySeconds / 60).toString(),
                        onValueChange = {},
                        label = { Text("左侧(分)") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md),
                        readOnly = true
                    )
                    OutlinedTextField(
                        value = (rightDisplaySeconds / 60).toString(),
                        onValueChange = {},
                        label = { Text("右侧(分)") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.md),
                        readOnly = true
                    )
                }
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md), minLines = 2
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            PrimaryButton(
                text = "保存记录",
                onClick = {
                    val now = System.currentTimeMillis()
                    // 保存时如果计时器还在运行，先停止计时
                    if (feedType == "breast" && timerState.isRunning) {
                        BreastfeedingTimer.stop(context)
                        FeedingTimerService.stop(context)
                    }
                    val leftDur = if (feedType == "breast") BreastfeedingTimer.getLeftTotalSeconds() / 60 else 0
                    val rightDur = if (feedType == "breast") BreastfeedingTimer.getRightTotalSeconds() / 60 else 0
                    val totalDur = leftDur + rightDur
                    viewModel.saveRecord(
                        type = feedType,
                        amount = amount.toIntOrNull() ?: 0,
                        unit = if (feedType == "breast") "min" else "ml",
                        leftDuration = leftDur,
                        rightDuration = rightDur,
                        startTime = if (feedType == "breast") now - totalDur * 60000L else 0,
                        endTime = if (feedType == "breast") now else 0,
                        side = side, note = note
                    ) {
                        BreastfeedingTimer.reset(context)
                        Toast.makeText(context, "喂奶记录已保存", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}

private fun formatTimerDisplay(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "${h}h${m}m${s}s"
        m > 0 -> "${m}m${s}s"
        else -> "${s}s"
    }
}
