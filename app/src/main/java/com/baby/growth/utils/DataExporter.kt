package com.baby.growth.utils

import android.content.Context
import com.baby.growth.data.database.AppDatabase
import com.baby.growth.data.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据导出导入工具
 */
object DataExporter {

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    data class ExportData(
        val version: String = "1.0.0",
        val exportTime: Long = System.currentTimeMillis(),
        val babyInfo: BabyInfo?,
        val feeds: List<FeedRecord>,
        val diapers: List<DiaperRecord>,
        val sleeps: List<SleepRecord>,
        val foods: List<FoodRecord>,
        val supplements: List<SupplementRecord>,
        val growthRecords: List<GrowthRecord>,
        val vaccines: List<VaccineRecord>
    )

    suspend fun exportData(context: Context, database: AppDatabase): String = withContext(Dispatchers.IO) {
        val babyInfo = database.babyInfoDao().getBabyInfoOnce()
        val feeds = database.feedDao().getAllOnce()
        val diapers = database.diaperDao().getAllOnce()
        val sleeps = database.sleepDao().getAllOnce()
        val foods = database.foodDao().getAllOnce()
        val supplements = database.supplementDao().getAllOnce()
        val growthRecords = database.growthDao().getAllOnce()
        val vaccines = database.vaccineDao().getAllOnce()

        val exportData = ExportData(
            babyInfo = babyInfo,
            feeds = feeds,
            diapers = diapers,
            sleeps = sleeps,
            foods = foods,
            supplements = supplements,
            growthRecords = growthRecords,
            vaccines = vaccines
        )

        val json = gson.toJson(exportData)
        val fileName = "baby_growth_${dateFormat.format(Date())}.json"

        // 写入到应用私有目录
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(json.toByteArray())
        }

        // 同时写入到外部存储可访问目录
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        exportDir.mkdirs()
        val externalFile = File(exportDir, fileName)
        FileOutputStream(externalFile).use { fos ->
            fos.write(json.toByteArray())
        }

        externalFile.absolutePath
    }

    suspend fun importData(context: Context, database: AppDatabase, json: String): Int = withContext(Dispatchers.IO) {
        val exportData = gson.fromJson<ExportData>(json, object : TypeToken<ExportData>() {}.type)
        var importedCount = 0

        // 导入宝宝信息
        if (exportData.babyInfo != null) {
            val existing = database.babyInfoDao().getBabyInfoOnce()
            if (existing == null) {
                database.babyInfoDao().insert(exportData.babyInfo)
                importedCount++
            }
        }

        // 导入各表数据，基于 uniqueId 去重
        val feedUniqueIds = database.feedDao().getAllOnce().map { it.uniqueId }.toSet()
        exportData.feeds.filter { it.uniqueId.isNotEmpty() && it.uniqueId !in feedUniqueIds }.forEach {
            database.feedDao().insert(it)
            importedCount++
        }

        val diaperUniqueIds = database.diaperDao().getAllOnce().map { it.uniqueId }.toSet()
        exportData.diapers.filter { it.uniqueId.isNotEmpty() && it.uniqueId !in diaperUniqueIds }.forEach {
            database.diaperDao().insert(it)
            importedCount++
        }

        val sleepUniqueIds = database.sleepDao().getAllOnce().map { it.uniqueId }.toSet()
        exportData.sleeps.filter { it.uniqueId.isNotEmpty() && it.uniqueId !in sleepUniqueIds }.forEach {
            database.sleepDao().insert(it)
            importedCount++
        }

        val foodUniqueIds = database.foodDao().getAllOnce().map { it.uniqueId }.toSet()
        exportData.foods.filter { it.uniqueId.isNotEmpty() && it.uniqueId !in foodUniqueIds }.forEach {
            database.foodDao().insert(it)
            importedCount++
        }

        val supplementUniqueIds = database.supplementDao().getAllOnce().map { it.uniqueId }.toSet()
        exportData.supplements.filter { it.uniqueId.isNotEmpty() && it.uniqueId !in supplementUniqueIds }.forEach {
            database.supplementDao().insert(it)
            importedCount++
        }

        val growthUniqueIds = database.growthDao().getAllOnce().map { it.uniqueId }.toSet()
        exportData.growthRecords.filter { it.uniqueId.isNotEmpty() && it.uniqueId !in growthUniqueIds }.forEach {
            database.growthDao().insert(it)
            importedCount++
        }

        val vaccineUniqueIds = database.vaccineDao().getAllOnce().map { it.uniqueId }.toSet()
        exportData.vaccines.filter { it.uniqueId.isNotEmpty() && it.uniqueId !in vaccineUniqueIds }.forEach {
            database.vaccineDao().insert(it)
            importedCount++
        }

        importedCount
    }
}