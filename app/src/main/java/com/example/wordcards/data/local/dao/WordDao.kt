package com.example.wordcards.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wordcards.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

/**
 * 单词表访问对象。
 *
 * 约定：
 * - 读列表返回 Flow，写操作用 suspend；不开 allowMainThreadQueries。
 * - 所有排序都带一个唯一列兜底（id），保证同一批数据每次顺序一致，
 *   否则调试和答辩演示时会遇到「顺序随机」的迷惑行为。
 */
@Dao
interface WordDao {

    /** 列表页：query 为空串返回全部，否则按单词/释义模糊匹配。 */
    @Query(
        """
        SELECT * FROM words
        WHERE :query = ''
           OR word LIKE '%' || :query || '%'
           OR definition LIKE '%' || :query || '%'
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun observeWords(query: String): Flow<List<Word>>

    /**
     * 到期队列：只取「复习过的词」。
     * 新词由 [getNewWords] 单独取，这样才能按每日新词上限控制数量。
     */
    @Query(
        """
        SELECT * FROM words
        WHERE firstReviewedEpochDay IS NOT NULL
          AND nextReviewEpochDay <= :today
        ORDER BY nextReviewEpochDay ASC, box ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getDueWords(today: Long, limit: Int): List<Word>

    /** 从未复习过的新词，按创建时间先进先出。 */
    @Query(
        """
        SELECT * FROM words
        WHERE firstReviewedEpochDay IS NULL
        ORDER BY createdAt ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getNewWords(limit: Int): List<Word>

    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE firstReviewedEpochDay IS NOT NULL AND nextReviewEpochDay <= :today
        """
    )
    fun observeDueCount(today: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE firstReviewedEpochDay IS NULL")
    fun observeNewWordCount(): Flow<Int>

    /** 今天已经「开过张」的新词数量，用于扣减每日新词额度。 */
    @Query("SELECT COUNT(*) FROM words WHERE firstReviewedEpochDay = :today")
    suspend fun countIntroducedOn(today: Long): Int

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): Word?

    @Query("SELECT * FROM words WHERE id = :id")
    fun observeById(id: Long): Flow<Word?>

    /** 导出用：一次性取全表。 */
    @Query("SELECT * FROM words ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<Word>

    @Query("SELECT COUNT(*) FROM words")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT box, COUNT(*) AS count FROM words GROUP BY box ORDER BY box ASC")
    fun observeBoxDistribution(): Flow<List<BoxCount>>

    /** 已达到指定盒子（含）以上的词数，用于「已掌握」指标。 */
    @Query("SELECT COUNT(*) FROM words WHERE box >= :minBox")
    fun observeCountWithMinBox(minBox: Int): Flow<Int>

    /** 唯一键冲突时返回 -1，由 Repository 转成业务错误。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<Word>): List<Long>

    @Update
    suspend fun update(word: Word)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 编辑时查重（排除自身）。 */
    @Query("SELECT COUNT(*) FROM words WHERE word = :word AND id != :excludeId")
    suspend fun countByWord(word: String, excludeId: Long): Int
}

/** 盒子分布投影。字段名必须与 SQL 的列别名一致。 */
data class BoxCount(
    val box: Int,
    val count: Int
)
