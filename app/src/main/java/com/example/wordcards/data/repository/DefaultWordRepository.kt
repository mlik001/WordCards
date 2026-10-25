package com.example.wordcards.data.repository

import androidx.room.withTransaction
import com.example.wordcards.data.local.AppDatabase
import com.example.wordcards.data.local.dao.ReviewLogDao
import com.example.wordcards.data.local.dao.WordDao
import com.example.wordcards.data.local.entity.ReviewLog
import com.example.wordcards.data.local.entity.Word
import com.example.wordcards.domain.model.BoxBucket
import com.example.wordcards.domain.model.DraftField
import com.example.wordcards.domain.model.ReviewResult
import com.example.wordcards.domain.model.SaveResult
import com.example.wordcards.domain.model.StudyStats
import com.example.wordcards.domain.model.WordDraft
import com.example.wordcards.domain.review.ReviewScheduler
import com.example.wordcards.domain.review.StudyQueueBuilder
import com.example.wordcards.domain.stats.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 默认实现：Room + LeitnerScheduler。
 *
 * 这一层承担三件 UI 不该管、DAO 也管不了的事：
 * 1. 输入清洗与唯一性校验（业务规则，不是 SQL）；
 * 2. 事务边界（更新词表 + 写流水必须同生共死）；
 * 3. 把多条 Flow 聚合成一个统计对象，避免 UI 拿到「半更新」的数据。
 */
class DefaultWordRepository(
    private val database: AppDatabase,
    private val wordDao: WordDao,
    private val reviewLogDao: ReviewLogDao,
    private val scheduler: ReviewScheduler
) : WordRepository {

    override fun observeWords(query: String): Flow<List<Word>> = wordDao.observeWords(query)

    override fun observeWord(id: Long): Flow<Word?> = wordDao.observeById(id)

    override fun observeDueCount(today: Long): Flow<Int> = wordDao.observeDueCount(today)

    override fun observeNewWordCount(): Flow<Int> = wordDao.observeNewWordCount()

    override fun observeStats(today: Long): Flow<StudyStats> {
        val wordPart = combine(
            wordDao.observeTotalCount(),
            wordDao.observeDueCount(today),
            wordDao.observeNewWordCount(),
            wordDao.observeCountWithMinBox(MASTERED_BOX),
            wordDao.observeBoxDistribution()
        ) { total, due, newCount, mastered, distribution ->
            WordPart(total, due, newCount, mastered, distribution)
        }

        val activityPart = combine(
            reviewLogDao.observeReviewedCountOn(today),
            reviewLogDao.observeActiveDays(STREAK_LOOKBACK_DAYS)
        ) { reviewedToday, activeDays ->
            ActivityPart(reviewedToday, StreakCalculator.streak(activeDays, today))
        }

        return combine(wordPart, activityPart) { words, activity ->
            StudyStats(
                totalWords = words.total,
                dueCount = words.due,
                newWordCount = words.newWordCount,
                masteredCount = words.mastered,
                reviewedToday = activity.reviewedToday,
                streakDays = activity.streak,
                boxDistribution = words.distribution.map { BoxBucket(it.box, it.count) }
            )
        }
    }

    override suspend fun getWord(id: Long): Word? = wordDao.getById(id)

    override suspend fun addWord(draft: WordDraft, today: Long): SaveResult {
        val clean = draft.normalized()
        validate(clean)?.let { return it }
        if (wordDao.countByWord(clean.word, excludeId = -1L) > 0) return SaveResult.DuplicateWord

        val now = System.currentTimeMillis()
        val id = wordDao.insert(
            Word(
                word = clean.word,
                definition = clean.definition,
                phonetic = clean.phonetic,
                example = clean.example,
                box = 0,
                // 新词从「今天」开始到期，而不是 0：否则 epochDay 0（1970 年）会算成逾期 2 万年
                nextReviewEpochDay = today,
                createdAt = now,
                updatedAt = now
            )
        )
        return if (id == -1L) SaveResult.DuplicateWord else SaveResult.Success(id)
    }

    override suspend fun updateWord(id: Long, draft: WordDraft, today: Long): SaveResult {
        val existing = wordDao.getById(id) ?: return SaveResult.NotFound
        val clean = draft.normalized()
        validate(clean)?.let { return it }
        if (wordDao.countByWord(clean.word, excludeId = id) > 0) return SaveResult.DuplicateWord

        wordDao.update(
            existing.copy(
                word = clean.word,
                definition = clean.definition,
                phonetic = clean.phonetic,
                example = clean.example,
                updatedAt = System.currentTimeMillis()
            )
        )
        return SaveResult.Success(id)
    }

    override suspend fun deleteWord(id: Long) {
        // review_logs 上配了 CASCADE，关联流水会一起删掉
        wordDao.deleteById(id)
    }

    override suspend fun loadStudyQueue(
        today: Long,
        sessionSize: Int,
        dailyNewWordLimit: Int
    ): List<Word> {
        val due = wordDao.getDueWords(today, sessionSize)
        val newCandidates = wordDao.getNewWords(sessionSize)
        val introducedToday = wordDao.countIntroducedOn(today)
        return StudyQueueBuilder.assemble(
            dueWords = due,
            newWordCandidates = newCandidates,
            sessionSize = sessionSize,
            introducedToday = introducedToday,
            dailyNewWordLimit = dailyNewWordLimit
        )
    }

    override suspend fun submitReview(
        wordId: Long,
        result: ReviewResult,
        today: Long,
        nowMillis: Long
    ): Word? = database.withTransaction {
        val current = wordDao.getById(wordId) ?: return@withTransaction null
        val outcome = scheduler.schedule(current.box, result, today)

        val updated = current.copy(
            box = outcome.box,
            nextReviewEpochDay = outcome.nextReviewEpochDay,
            correctCount = current.correctCount + if (result == ReviewResult.KNOWN) 1 else 0,
            wrongCount = current.wrongCount + if (result == ReviewResult.UNKNOWN) 1 else 0,
            lastReviewedEpochDay = today,
            firstReviewedEpochDay = current.firstReviewedEpochDay ?: today,
            updatedAt = nowMillis
        )

        wordDao.update(updated)
        reviewLogDao.insert(
            ReviewLog(
                wordId = wordId,
                reviewedAtEpochDay = today,
                reviewedAtMillis = nowMillis,
                known = result == ReviewResult.KNOWN,
                boxBefore = current.box,
                boxAfter = outcome.box
            )
        )
        updated
    }

    override suspend fun exportAll(): List<Word> = wordDao.getAll()

    override suspend fun importWords(drafts: List<WordDraft>, today: Long): ImportSummary =
        database.withTransaction {
            var inserted = 0
            var duplicated = 0
            var invalid = 0
            val now = System.currentTimeMillis()

            drafts.forEach { draft ->
                val clean = draft.normalized()
                when {
                    validate(clean) != null -> invalid++
                    wordDao.countByWord(clean.word, excludeId = -1L) > 0 -> duplicated++
                    else -> {
                        val id = wordDao.insert(
                            Word(
                                word = clean.word,
                                definition = clean.definition,
                                phonetic = clean.phonetic,
                                example = clean.example,
                                nextReviewEpochDay = today,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                        if (id == -1L) duplicated++ else inserted++
                    }
                }
            }
            ImportSummary(inserted = inserted, duplicated = duplicated, invalid = invalid)
        }

    private fun validate(draft: WordDraft): SaveResult.Invalid? = when {
        draft.word.isEmpty() -> SaveResult.Invalid(DraftField.WORD)
        draft.definition.isEmpty() -> SaveResult.Invalid(DraftField.DEFINITION)
        else -> null
    }

    private data class WordPart(
        val total: Int,
        val due: Int,
        val newWordCount: Int,
        val mastered: Int,
        val distribution: List<com.example.wordcards.data.local.dao.BoxCount>
    )

    private data class ActivityPart(val reviewedToday: Int, val streak: Int)

    companion object {
        /** box >= 4 视为「已掌握」，与统计页的文案口径保持一致。 */
        const val MASTERED_BOX: Int = 4

        /** 连续打卡最多回溯 120 天，够用且不随历史增长变慢。 */
        const val STREAK_LOOKBACK_DAYS: Int = 120
    }
}
