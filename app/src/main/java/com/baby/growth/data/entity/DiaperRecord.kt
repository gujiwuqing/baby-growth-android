package com.baby.growth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diapers")
data class DiaperRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueId: String = "",
    val type: String = "pee",
    val hasRash: Int = 0,
    val pooColor: String = "",
    val pooShape: String = "",
    val color: String = "",
    val note: String = "",
    val recordTime: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)