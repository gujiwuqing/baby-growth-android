package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.SupplementRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements ORDER BY recordTime DESC")
    fun getAll(): Flow<List<SupplementRecord>>

    @Query("SELECT * FROM supplements ORDER BY recordTime DESC")
    suspend fun getAllOnce(): List<SupplementRecord>

    @Query("SELECT * FROM supplements WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<SupplementRecord>>

    @Query("SELECT * FROM supplements WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    suspend fun getByDateRangeOnce(startTime: Long, endTime: Long): List<SupplementRecord>

    @Query("SELECT COUNT(*) FROM supplements WHERE recordTime BETWEEN :startTime AND :endTime")
    suspend fun getCountByDateRange(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM supplements WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): SupplementRecord?

    @Query("SELECT * FROM supplements ORDER BY recordTime DESC LIMIT 1")
    suspend fun getLatest(): SupplementRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: SupplementRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<SupplementRecord>)

    @Update
    suspend fun update(record: SupplementRecord)

    @Delete
    suspend fun delete(record: SupplementRecord)

    @Query("DELETE FROM supplements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM supplements")
    suspend fun deleteAll()
}