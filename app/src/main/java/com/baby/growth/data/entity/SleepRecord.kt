package com.baby.growth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleeps")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueId: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val duration: Int = 0,
    val quality: String = "",
    val isNextDay: Int = 0,
    val note: String = "",
    val recordTime: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)