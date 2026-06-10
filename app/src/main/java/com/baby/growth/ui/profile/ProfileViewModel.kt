package com.baby.growth.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.BabyInfo
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
