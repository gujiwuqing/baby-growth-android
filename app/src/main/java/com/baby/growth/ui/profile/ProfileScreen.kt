package com.baby.growth.ui.profile

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.BabyInfo
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _babyInfo = MutableStateFlow<BabyInfo?>(null)
    val babyInfo = _babyInfo.asStateFlow()

    init {
        viewModelScope.launch {
            db.babyInfoDao().getBabyInfo().collect { _babyInfo.value = it }
        }
    }

    fun updateBabyInfo(name: String, gender: Int, birthday: Long, avatar: String) {
        viewModelScope.launch {
            val existing = _babyInfo.value
            val updated = (existing ?: BabyInfo()).copy(
                name = name, gender = gender, birthday = birthday, avatar = avatar,
                updatedAt = System.currentTimeMillis()
            )
            if (existing != null) db.babyInfoDao().update(updated)
            else db.babyInfoDao().insert(updated)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val babyInfo by viewModel.babyInfo.collectAsState()
    var editField by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("宝宝信息") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像区域
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable { editField = "avatar" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    babyInfo?.avatar ?: "👶",
                    fontSize = 56.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 信息列表
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val monthAge = babyInfo?.let { DateUtils.getMonthAge(it.birthday) } ?: 0
                    val dayAge = babyInfo?.let { DateUtils.getDayAge(it.birthday) } ?: 0

                    InfoRow("姓名", babyInfo?.name ?: "未设置") { editField = "name" }
                    InfoRow("性别", if (babyInfo?.gender == 1) "男宝 👦" else "女宝 👧") { editField = "gender" }
                    InfoRow("出生日期", babyInfo?.let { DateUtils.formatDate(it.birthday) } ?: "未设置") { editField = "birthday" }
                    InfoRow("月龄", "${monthAge}个月") { }
                    InfoRow("出生天数", "${dayAge}天") { }
                }
            }
        }
    }

    // 编辑对话框
    editField?.let { field ->
        when (field) {
            "name" -> EditNameDialog(
                initialValue = babyInfo?.name ?: "",
                onDismiss = { editField = null },
                onConfirm = { name ->
                    viewModel.updateBabyInfo(
                        name, babyInfo?.gender ?: 0, babyInfo?.birthday ?: System.currentTimeMillis(),
                        babyInfo?.avatar ?: "👶"
                    )
                    editField = null
                }
            )
            "gender" -> EditGenderDialog(
                initialGender = babyInfo?.gender ?: 0,
                onDismiss = { editField = null },
                onConfirm = { gender ->
                    viewModel.updateBabyInfo(
                        babyInfo?.name ?: "", gender, babyInfo?.birthday ?: System.currentTimeMillis(),
                        babyInfo?.avatar ?: "👶"
                    )
                    editField = null
                }
            )
            "birthday" -> EditBirthdayDialog(
                initialBirthday = babyInfo?.birthday ?: System.currentTimeMillis(),
                onDismiss = { editField = null },
                onConfirm = { birthday ->
                    viewModel.updateBabyInfo(
                        babyInfo?.name ?: "", babyInfo?.gender ?: 0, birthday,
                        babyInfo?.avatar ?: "👶"
                    )
                    editField = null
                }
            )
            "avatar" -> EditAvatarDialog(
                initialAvatar = babyInfo?.avatar ?: "👶",
                onDismiss = { editField = null },
                onConfirm = { avatar ->
                    viewModel.updateBabyInfo(
                        babyInfo?.name ?: "", babyInfo?.gender ?: 0,
                        babyInfo?.birthday ?: System.currentTimeMillis(), avatar
                    )
                    editField = null
                }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, "", modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditNameDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑姓名") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("宝宝姓名") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditGenderDialog(initialGender: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var gender by remember { mutableStateOf(initialGender) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择性别") },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = gender == 1, onClick = { gender = 1 }, label = { Text("男宝 👦") },
                    modifier = Modifier.weight(1f))
                FilterChip(selected = gender == 0, onClick = { gender = 0 }, label = { Text("女宝 👧") },
                    modifier = Modifier.weight(1f))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(gender) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBirthdayDialog(initialBirthday: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialBirthday,
        yearRange = IntRange(2020, 2026)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selected = datePickerState.selectedDateMillis ?: initialBirthday
                onConfirm(selected)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun EditAvatarDialog(initialAvatar: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val avatars = listOf("👶", "👦", "👧", "🧒", "👼", "🐣", "🌸", "⭐")
    var selected by remember { mutableStateOf(initialAvatar) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择头像") },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(avatars) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selected = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 32.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
