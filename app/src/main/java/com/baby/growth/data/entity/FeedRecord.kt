package com.baby.growth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds")
data class FeedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueId: String = "",
    val type: String = "breast",
    val amount: Int = 0,
    val unit: String = "ml",
    val leftDuration: Int = 0,
    val rightDuration: Int = 0,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val side: String = "both",
    val note: String = "",
    val recordTime: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)