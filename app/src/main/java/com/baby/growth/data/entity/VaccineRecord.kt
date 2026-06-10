package com.baby.growth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaccines")
data class VaccineRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueId: String = "",
    val vaccineName: String = "",
    val vaccineType: String = "free",
    val dose: String = "",
    val ageMonths: Int = 0,
    val scheduledDate: Long = 0,
    val actualDate: Long? = null,
    val injectionSite: String = "",
    val batchNumber: String = "",
    val manufacturer: String = "",
    val hospital: String = "",
    val doctor: String = "",
    val status: String = "pending",
    val adverseReaction: String = "",
    val reaction: String = "",
    val note: String = "",
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)