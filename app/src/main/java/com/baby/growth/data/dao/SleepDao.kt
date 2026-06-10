package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.SleepRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleeps ORDER BY recordTime DESC")
    fun getAll(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleeps ORDER BY recordTime DESC")
    suspend fun getAllOnce(): List<SleepRecord>

    @Query("SELECT * FROM sleeps WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleeps WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    suspend fun getByDateRangeOnce(startTime: Long, endTime: Long): List<SleepRecord>

    @Query("SELECT SUM(duration) FROM sleeps WHERE recordTime BETWEEN :startTime AND :endTime")
    suspend fun getTotalDurationByDateRange(startTime: Long, endTime: Long): Int?

    @Query("SELECT COUNT(*) FROM sleeps WHERE recordTime BETWEEN :startTime AND :endTime")
    suspend fun getCountByDateRange(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM sleeps WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): SleepRecord?

    @Query("SELECT * FROM sleeps ORDER BY recordTime DESC LIMIT 1")
    suspend fun getLatest(): SleepRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: SleepRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<SleepRecord>)

    @Update
    suspend fun update(record: SleepRecord)

    @Delete
    suspend fun delete(record: SleepRecord)

    @Query("DELETE FROM sleeps WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sleeps")
    suspend fun deleteAll()
}