package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.FoodRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY recordTime DESC")
    fun getAll(): Flow<List<FoodRecord>>

    @Query("SELECT * FROM foods ORDER BY recordTime DESC")
    suspend fun getAllOnce(): List<FoodRecord>

    @Query("SELECT * FROM foods WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<FoodRecord>>

    @Query("SELECT * FROM foods WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    suspend fun getByDateRangeOnce(startTime: Long, endTime: Long): List<FoodRecord>

    @Query("SELECT COUNT(*) FROM foods WHERE recordTime BETWEEN :startTime AND :endTime")
    suspend fun getCountByDateRange(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM foods WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): FoodRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: FoodRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<FoodRecord>)

    @Update
    suspend fun update(record: FoodRecord)

    @Delete
    suspend fun delete(record: FoodRecord)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM foods")
    suspend fun deleteAll()
}