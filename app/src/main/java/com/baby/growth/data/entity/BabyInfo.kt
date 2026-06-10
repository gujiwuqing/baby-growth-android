package com.baby.growth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baby_info")
data class BabyInfo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val gender: Int = 0,
    val birthday: Long = System.currentTimeMillis(),
    val avatar: String = "👶",
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)