package com.baby.growth.ui.growth

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.GrowthRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GrowthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _records = MutableStateFlow<List<GrowthRecord>>(emptyList())
    val records: StateFlow<List<GrowthRecord>> = _records.asStateFlow()

    private val _latestRecord = MutableStateFlow<GrowthRecord?>(null)
    val latestRecord: StateFlow<GrowthRecord?> = _latestRecord.asStateFlow()

    private val _babyGender = MutableStateFlow<Int>(0)
    val babyGender: StateFlow<Int> = _babyGender.asStateFlow()

    init {
        viewModelScope.launch {
            db.growthDao().getAll().collect { _records.value = it }
        }
        viewModelScope.launch {
            _latestRecord.value = db.growthDao().getLatest()
        }
        viewModelScope.launch {
            val babyInfo = db.babyInfoDao().getBabyInfoOnce()
            _babyGender.value = babyInfo?.gender ?: 0
        }
    }
}

data class LegendItem(val label: String, val lineColor: Color, val indexOffset: Int)
