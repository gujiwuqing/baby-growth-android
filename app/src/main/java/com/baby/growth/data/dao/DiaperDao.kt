package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.DiaperRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaperDao {
    @Query("SELECT * FROM diapers ORDER BY recordTime DESC")
    fun getAll(): Flow<List<DiaperRecord>>

    @Query("SELECT * FROM diapers ORDER BY recordTime DESC")
    suspend fun getAllOnce(): List<DiaperRecord>

    @Query("SELECT * FROM diapers WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<DiaperRecord>>

    @Query("SELECT * FROM diapers WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    suspend fun getByDateRangeOnce(startTime: Long, endTime: Long): List<DiaperRecord>

    @Query("SELECT COUNT(*) FROM diapers WHERE recordTime BETWEEN :startTime AND :endTime")
    suspend fun getCountByDateRange(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM diapers WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): DiaperRecord?

    @Query("SELECT * FROM diapers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DiaperRecord?

    @Query("SELECT * FROM diapers ORDER BY recordTime DESC LIMIT 1")
    suspend fun getLatest(): DiaperRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: DiaperRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<DiaperRecord>)

    @Update
    suspend fun update(record: DiaperRecord)

    @Delete
    suspend fun delete(record: DiaperRecord)

    @Query("DELETE FROM diapers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM diapers")
    suspend fun deleteAll()
}