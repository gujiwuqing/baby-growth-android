package com.baby.growth.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.BabyInfo
import com.baby.growth.utils.DataExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _babyInfo = MutableStateFlow<BabyInfo?>(null)
    val babyInfo: StateFlow<BabyInfo?> = _babyInfo.asStateFlow()

    init {
        viewModelScope.launch {
            db.babyInfoDao().getBabyInfo().collect { _babyInfo.value = it }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            db.feedDao().deleteAll()
            db.diaperDao().deleteAll()
            db.sleepDao().deleteAll()
            db.foodDao().deleteAll()
            db.supplementDao().deleteAll()
            db.growthDao().deleteAll()
            db.vaccineDao().deleteAll()
        }
    }

    suspend fun exportData(context: Context): String {
        return DataExporter.exportData(context, db)
    }

    suspend fun importData(context: Context, json: String): Int {
        return DataExporter.importData(context, db, json)
    }
}
