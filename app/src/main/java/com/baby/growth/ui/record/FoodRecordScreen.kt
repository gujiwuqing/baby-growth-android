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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.FoodRecord
import com.baby.growth.utils.DateUtils
import kotlinx.coroutines.launch

class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRecordScreen(
    navController: NavController,
    viewModel: FoodViewModel = viewModel()
) {
    val context = LocalContext.current
    var foodName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("grain") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var reaction by remember { mutableStateOf("normal") }
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("辅食记录") },
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
            OutlinedTextField(
                value = foodName, onValueChange = { foodName = it },
                label = { Text("食物名称") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("食物分类", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("grain" to "谷物", "vegetable" to "蔬菜", "fruit" to "水果").forEach { (v, l) ->
                    FilterChip(selected = category == v, onClick = { category = v }, label = { Text(l) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("meat" to "肉类", "egg" to "蛋类", "dairy" to "奶制品").forEach { (v, l) ->
                    FilterChip(selected = category == v, onClick = { category = v }, label = { Text(l) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("食用量") }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Text("单位", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("g", "ml", "勺", "个", "片").forEach { u ->
                    FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                }
            }

            Text("反应", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("normal" to "✅ 正常", "allergy" to "⚠️ 过敏", "refuse" to "❌ 拒绝").forEach { (v, l) ->
                    FilterChip(selected = reaction == v, onClick = { reaction = v }, label = { Text(l) })
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
                    viewModel.saveRecord(foodName, category, amount, unit, reaction, note) {
                        Toast.makeText(context, "辅食记录已保存", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("保存记录", fontWeight = FontWeight.Bold) }
        }
    }
}
