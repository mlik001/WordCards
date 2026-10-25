package com.example.wordcards.data.repository

import com.example.wordcards.data.local.entity.Word
import com.example.wordcards.domain.model.ReviewResult
import com.example.wordcards.domain.model.SaveResult
import com.example.wordcards.domain.model.StudyStats
import com.example.wordcards.domain.model.WordDraft
import kotlinx.coroutines.flow.Flow

/**
 * 单词数据的唯一入口。
 *
 * 定义为 interface 是为了两件事：
 * 1. 换实现（将来接云端同步）不动 ViewModel；
 * 2. 写 ViewModel 测试时可以塞一个假实现，不必启动 Room。
 */
interface WordRepository {

    // ---------- 读 ----------

    fun observeWords(query: String): Flow<List<Word>>

    fun observeWord(id: Long): Flow<Word?>

    fun observeDueCount(today: Long): Flow<Int>

    fun observeNewWordCount(): Flow<Int>

    fun observeStats(today: Long): Flow<StudyStats>

    suspend fun getWord(id: Long): Word?

    // ---------- 写 ----------

    suspend fun addWord(draft: WordDraft, today: Long): SaveResult

    suspend fun updateWord(id: Long, draft: WordDraft, today: Long): SaveResult

    suspend fun deleteWord(id: Long)

    // ---------- 复习 ----------

    /**
     * 开一轮会话：到期词优先，新词按「每日新词上限」和会话长度扣减。
     * 返回的是快照，调用方在内存里消费，不要边答边重查。
     */
    suspend fun loadStudyQueue(
        today: Long,
        sessionSize: Int,
        dailyNewWordLimit: Int
    ): List<Word>

    /**
     * 提交一次复习：算新状态 → 更新 word → 写 review_log，整体在一个事务里。
     * 词已被删除时返回 null，调用方跳过即可。
     */
    suspend fun submitReview(
        wordId: Long,
        result: ReviewResult,
        today: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Word?

    // ---------- 导入导出 ----------

    suspend fun exportAll(): List<Word>

    suspend fun importWords(drafts: List<WordDraft>, today: Long): ImportSummary
}

/** 导入结果，用来给用户一句「新增 12 个，跳过 3 个重复」。 */
data class ImportSummary(
    val inserted: Int = 0,
    val duplicated: Int = 0,
    val invalid: Int = 0
)
