package com.baby.growth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplements")
data class SupplementRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueId: String = "",
    val supplementType: String = "vitamin_ad",
    val supplementName: String = "",
    val brand: String = "",
    val amount: String = "",
    val dosage: String = "",
    val note: String = "",
    val recordTime: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)