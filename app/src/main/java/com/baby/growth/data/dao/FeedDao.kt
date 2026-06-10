package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.FeedRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds ORDER BY recordTime DESC")
    fun getAll(): Flow<List<FeedRecord>>

    @Query("SELECT * FROM feeds ORDER BY recordTime DESC")
    suspend fun getAllOnce(): List<FeedRecord>

    @Query("SELECT * FROM feeds WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<FeedRecord>>

    @Query("SELECT * FROM feeds WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    suspend fun getByDateRangeOnce(startTime: Long, endTime: Long): List<FeedRecord>

    @Query("SELECT COUNT(*) FROM feeds WHERE recordTime BETWEEN :startTime AND :endTime")
    suspend fun getCountByDateRange(startTime: Long, endTime: Long): Int

    @Query("SELECT SUM(amount) FROM feeds WHERE recordTime BETWEEN :startTime AND :endTime")
    suspend fun getTotalAmountByDateRange(startTime: Long, endTime: Long): Int?

    @Query("SELECT * FROM feeds WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): FeedRecord?

    @Query("SELECT * FROM feeds WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FeedRecord?

    @Query("SELECT * FROM feeds ORDER BY recordTime DESC LIMIT 1")
    suspend fun getLatest(): FeedRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: FeedRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<FeedRecord>)

    @Update
    suspend fun update(record: FeedRecord)

    @Delete
    suspend fun delete(record: FeedRecord)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM feeds")
    suspend fun deleteAll()
}