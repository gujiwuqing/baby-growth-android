package com.baby.growth.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.components.BabyTitledCard
import com.baby.growth.ui.components.BabyTopBar
import com.baby.growth.ui.theme.Radius
import com.baby.growth.ui.theme.Spacing
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val babyInfo by viewModel.babyInfo.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val currentTheme = ThemeManager.getThemeConfig(context)
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val filePath = viewModel.exportData(context)
                    Toast.makeText(context, "数据已导出到: $filePath", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        val count = viewModel.importData(context, json)
                        Toast.makeText(context, "成功导入 $count 条记录", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            BabyTopBar(title = "设置")
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 宝宝信息卡片
            BabyCard(
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate("profile")
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(Spacing.lg)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = babyInfo?.avatar ?: "👶",
                            fontSize = 32.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.lg))
                    Column {
                        Text(
                            text = babyInfo?.name ?: "未设置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        val monthAge = babyInfo?.let { DateUtils.getMonthAge(it.birthday) } ?: 0
                        val genderText = when (babyInfo?.gender) {
                            1 -> "男宝 👦"
                            0 -> "女宝 👧"
                            else -> "未设置"
                        }
                        Text(
                            text = "${monthAge}个月 · $genderText",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 数据管理卡片
            BabyTitledCard(title = "数据管理") {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    SettingsItem(Icons.Filled.FileDownload, "导出数据") {
                        coroutineScope.launch {
                            try {
                                val filePath = viewModel.exportData(context)
                                Toast.makeText(context, "数据已导出到: $filePath", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    SettingsItem(Icons.Filled.FileUpload, "导入数据") {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                    SettingsItem(Icons.Filled.DeleteForever, "清空所有数据", isDestructive = true) {
                        showClearDialog = true
                    }
                }
            }

            // 个性化卡片
            BabyTitledCard(title = "个性化") {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    SettingsItem(Icons.Filled.Palette, "主题切换") {
                        showThemeDialog = true
                    }
                }
            }

            // 关于卡片
            BabyTitledCard(title = "关于") {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text("宝宝成长记 v1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("记录宝宝成长的每一个瞬间 💕", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }
    }

    // 清空数据确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空") },
            text = { Text("确定要清空所有记录数据吗？此操作不可恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                    Toast.makeText(context, "数据已清空", Toast.LENGTH_SHORT).show()
                }) { Text("确认清空", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    // 主题选择对话框
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(ThemeManager.THEMES) { theme ->
                        val isSelected = theme.key == ThemeManager.getSelectedThemeKey(context)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemeManager.setTheme(context, theme.key)
                                    showThemeDialog = false
                                    Toast.makeText(context, "已切换到${theme.name}", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(Radius.xl),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            } else null
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(theme.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    theme.name,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDestructive) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isDestructive) Color.Red else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = title,
            color = if (isDestructive) Color.Red else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
