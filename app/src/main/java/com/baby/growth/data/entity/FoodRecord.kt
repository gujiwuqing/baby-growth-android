package com.baby.growth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueId: String = "",
    val foodName: String = "",
    val category: String = "",
    val amount: String = "",
    val unit: String = "g",
    val reaction: String = "",
    val note: String = "",
    val recordTime: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)