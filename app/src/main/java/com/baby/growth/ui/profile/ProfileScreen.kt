package com.baby.growth.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.components.pickers.BabyDatePicker
import com.baby.growth.ui.components.pickers.PickerDialog
import com.baby.growth.ui.theme.BabyGrowthTheme
import com.baby.growth.ui.theme.Radius
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.utils.DateUtils

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
            BabyTopBar(title = "宝宝信息", onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像区域
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { editField = "avatar" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    babyInfo?.avatar ?: "👶",
                    fontSize = 56.sp
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "编辑头像",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xl))

            // 信息列表
            BabyCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    val monthAge = babyInfo?.let { DateUtils.getMonthAge(it.birthday) } ?: 0
                    val dayAge = babyInfo?.let { DateUtils.getDayAge(it.birthday) } ?: 0

                    InfoRow("姓名", babyInfo?.name ?: "未设置") { editField = "name" }
                    HorizontalDivider(color = BabyGrowthTheme.colors.dividerColor, modifier = Modifier.padding(horizontal = Spacing.md))
                    InfoRow("性别", if (babyInfo?.gender == 1) "男宝 👦" else "女宝 👧") { editField = "gender" }
                    HorizontalDivider(color = BabyGrowthTheme.colors.dividerColor, modifier = Modifier.padding(horizontal = Spacing.md))
                    InfoRow("出生日期", babyInfo?.let { DateUtils.formatDate(it.birthday) } ?: "未设置") { editField = "birthday" }
                    HorizontalDivider(color = BabyGrowthTheme.colors.dividerColor, modifier = Modifier.padding(horizontal = Spacing.md))
                    InfoRow("月龄", "${monthAge}个月") { }
                    HorizontalDivider(color = BabyGrowthTheme.colors.dividerColor, modifier = Modifier.padding(horizontal = Spacing.md))
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
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = RoundedCornerShape(Radius.xl)
    )
}

@Composable
private fun EditGenderDialog(initialGender: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var gender by remember { mutableStateOf(initialGender) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择性别") },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                FilterChip(selected = gender == 1, onClick = { gender = 1 }, label = { Text("男宝 👦") },
                    modifier = Modifier.weight(1f))
                FilterChip(selected = gender == 0, onClick = { gender = 0 }, label = { Text("女宝 👧") },
                    modifier = Modifier.weight(1f))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(gender) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = RoundedCornerShape(Radius.xl)
    )
}

@Composable
private fun EditBirthdayDialog(initialBirthday: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var selectedDate by remember { mutableLongStateOf(initialBirthday) }
    PickerDialog(
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selectedDate) },
        title = "选择出生日期",
    ) {
        BabyDatePicker(
            selectedDateMillis = selectedDate,
            onDateSelected = { selectedDate = it },
            maxDateMillis = System.currentTimeMillis(),
        )
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
            LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(avatars) { emoji ->
                    val isSelected = emoji == selected
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selected = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 32.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = RoundedCornerShape(Radius.xl)
    )
}
