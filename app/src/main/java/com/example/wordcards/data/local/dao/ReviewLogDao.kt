package com.example.wordcards.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.wordcards.data.local.entity.ReviewLog
import kotlinx.coroutines.flow.Flow

/** 复习流水访问对象。只写不常读，读都是聚合统计。 */
@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLog)

    @Insert
    suspend fun insertAll(logs: List<ReviewLog>)

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAtEpochDay = :day")
    fun observeReviewedCountOn(day: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_logs")
    fun observeTotalReviewCount(): Flow<Int>

    /**
     * 有复习记录的日期（倒序，去重）。
     * 只取最近 N 天，够算连续打卡即可，避免历史数据变多后每次全表扫。
     */
    @Query(
        """
        SELECT DISTINCT reviewedAtEpochDay FROM review_logs
        ORDER BY reviewedAtEpochDay DESC
        LIMIT :limit
        """
    )
    fun observeActiveDays(limit: Int): Flow<List<Long>>
}
