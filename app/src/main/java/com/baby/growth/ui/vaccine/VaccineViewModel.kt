package com.baby.growth.ui.vaccine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baby.growth.BabyGrowthApp
import com.baby.growth.data.entity.VaccineRecord
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.VaccineData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VaccineViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database

    private val _vaccineRecords = MutableStateFlow<List<VaccineRecord>>(emptyList())
    val vaccineRecords: StateFlow<List<VaccineRecord>> = _vaccineRecords.asStateFlow()

    private val _babyBirthday = MutableStateFlow<Long?>(null)
    val babyBirthday: StateFlow<Long?> = _babyBirthday.asStateFlow()

    init {
        viewModelScope.launch {
            db.vaccineDao().getAll().collect { records ->
                _vaccineRecords.value = records
            }
        }
        viewModelScope.launch {
            db.babyInfoDao().getBabyInfo().collect { babyInfo ->
                _babyBirthday.value = babyInfo?.birthday
            }
        }
    }

    fun initializeVaccineSchedule(birthday: Long) {
        viewModelScope.launch {
            val existingRecords = db.vaccineDao().getAllOnce()
            if (existingRecords.isNotEmpty()) return@launch

            val allVaccines = VaccineData.getAllVaccines()
            val recordsToInsert = allVaccines.map { vaccine ->
                val scheduledDate = DateUtils.addMonthsToBirthday(birthday, vaccine.ageMonths)
                VaccineRecord(
                    uniqueId = "${vaccine.name}_${vaccine.dose}_${scheduledDate}",
                    vaccineName = vaccine.name,
                    vaccineType = vaccine.type,
                    dose = vaccine.dose,
                    ageMonths = vaccine.ageMonths,
                    scheduledDate = scheduledDate,
                    status = "pending"
                )
            }
            db.vaccineDao().insertAll(recordsToInsert)
        }
    }

    fun markVaccinated(record: VaccineRecord) {
        viewModelScope.launch {
            val updated = record.copy(
                status = "done",
                actualDate = System.currentTimeMillis()
            )
            db.vaccineDao().update(updated)
        }
    }

    fun getFilteredVaccines(type: String): List<VaccineRecord> {
        return _vaccineRecords.value.filter { it.vaccineType == type }
            .sortedBy { it.scheduledDate }
    }

    fun getUpcomingVaccine(): VaccineRecord? {
        return _vaccineRecords.value
            .filter { it.status == "pending" }
            .sortedBy { it.scheduledDate }
            .firstOrNull()
    }

    fun getProgressStats(type: String): Pair<Int, Int> {
        val filtered = _vaccineRecords.value.filter { it.vaccineType == type }
        val done = filtered.count { it.status == "done" }
        return done to filtered.size
    }
}
