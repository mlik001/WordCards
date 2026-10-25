package com.example.wordcards.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wordcards.data.local.AppDatabase
import com.example.wordcards.data.local.entity.Word
import com.example.wordcards.domain.model.DraftField
import com.example.wordcards.domain.model.ReviewResult
import com.example.wordcards.domain.model.SaveResult
import com.example.wordcards.domain.model.WordDraft
import com.example.wordcards.domain.review.LeitnerScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Repository 的事务与队列规则测试。
 *
 * 这一层是「业务规则真正落地」的地方，最容易出错的是：
 * - 提交复习时词表更新成功、流水没写（事务缺失）；
 * - 队列把新词一次性全抽出来（没按 quota 扣减）。
 * 两个都在这里用真实数据库验证。
 */
@RunWith(AndroidJUnit4::class)
class DefaultWordRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultWordRepository

    private val today = 20_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = DefaultWordRepository(
            database = db,
            wordDao = db.wordDao(),
            reviewLogDao = db.reviewLogDao(),
            scheduler = LeitnerScheduler()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---------- 新增 / 编辑 ----------

    @Test
    fun 新增单词会初始化为今天到期的新词() = runTest {
        val result = repository.addWord(WordDraft("abandon", "v. 放弃"), today)
        assertTrue(result is SaveResult.Success)

        val word = repository.getWord((result as SaveResult.Success).id)!!
        assertEquals(0, word.box)
        assertEquals(today, word.nextReviewEpochDay)
        assertNull(word.firstReviewedEpochDay)
        assertEquals("abandon", word.word)
    }

    @Test
    fun 重复单词返回业务错误而不是抛异常() = runTest {
        repository.addWord(WordDraft("apple", "苹果"), today)
        val duplicated = repository.addWord(WordDraft("Apple", "苹果"), today)
        assertEquals(SaveResult.DuplicateWord, duplicated)
    }

    @Test
    fun 空字段返回对应字段的校验错误() = runTest {
        val emptyWord = repository.addWord(WordDraft("   ", "苹果"), today)
        assertEquals(SaveResult.Invalid(DraftField.WORD), emptyWord)

        val emptyDefinition = repository.addWord(WordDraft("apple", "  "), today)
        assertEquals(SaveResult.Invalid(DraftField.DEFINITION), emptyDefinition)
    }

    @Test
    fun 编辑不改变复习进度() = runTest {
        val id = (repository.addWord(WordDraft("apple", "苹果"), today) as SaveResult.Success).id
        repository.submitReview(id, ReviewResult.KNOWN, today)

        repository.updateWord(id, WordDraft("apple", "n. 苹果", "/ˈæpl/"), today)

        val word = repository.getWord(id)!!
        assertEquals("n. 苹果", word.definition)
        assertEquals(1, word.box)
        assertEquals(1, word.correctCount)
    }

    // ---------- 提交复习 ----------

    @Test
    fun 认识后盒子进一格并写一条流水() = runTest {
        val id = (repository.addWord(WordDraft("abandon", "v. 放弃"), today) as SaveResult.Success).id

        val updated = repository.submitReview(id, ReviewResult.KNOWN, today)!!

        assertEquals(1, updated.box)
        assertEquals(today + 1, updated.nextReviewEpochDay)
        assertEquals(1, updated.correctCount)
        assertEquals(0, updated.wrongCount)
        assertEquals(today, updated.firstReviewedEpochDay)
        assertEquals(1, db.reviewLogDao().observeTotalReviewCount().first())
    }

    @Test
    fun 不认识会归零且当天仍然到期() = runTest {
        val id = (repository.addWord(WordDraft("abandon", "v. 放弃"), today) as SaveResult.Success).id
        // 先推到 3 号盒
        repeat(3) { repository.submitReview(id, ReviewResult.KNOWN, today) }
        assertEquals(3, repository.getWord(id)!!.box)

        val updated = repository.submitReview(id, ReviewResult.UNKNOWN, today)!!

        assertEquals(0, updated.box)
        assertEquals(today, updated.nextReviewEpochDay)
        assertEquals(1, updated.wrongCount)
        assertEquals(3, updated.correctCount)
    }

    @Test
    fun 删除后再提交复习返回空() = runTest {
        val id = (repository.addWord(WordDraft("gone", "消失"), today) as SaveResult.Success).id
        repository.deleteWord(id)

        assertNull(repository.submitReview(id, ReviewResult.KNOWN, today))
    }

    // ---------- 队列规则 ----------

    @Test
    fun 队列先排到期词再按额度补新词() = runTest {
        // 2 个「以前就复习过、今天到期」的词（直接写库，模拟历史数据）
        repeat(2) { index ->
            db.wordDao().insert(
                Word(
                    word = "reviewed$index",
                    definition = "旧词",
                    box = 2,
                    nextReviewEpochDay = today - 1,
                    firstReviewedEpochDay = today - 3,
                    lastReviewedEpochDay = today - 3
                )
            )
        }
        // 5 个从未学过的词
        repeat(5) { index ->
            repository.addWord(WordDraft("fresh$index", "新词"), today)
        }

        val queue = repository.loadStudyQueue(today, sessionSize = 4, dailyNewWordLimit = 1)

        assertEquals(3, queue.size) // 2 个到期 + 1 个新词（额度只允许 1 个）
        assertTrue(queue.take(2).all { it.firstReviewedEpochDay != null })
        assertEquals(1, queue.count { it.firstReviewedEpochDay == null })
    }

    @Test
    fun 今日新词额度用完后只复习旧词() = runTest {
        db.wordDao().insert(
            Word(
                word = "old",
                definition = "旧词",
                box = 2,
                nextReviewEpochDay = today - 1,
                firstReviewedEpochDay = today - 5,
                lastReviewedEpochDay = today - 5
            )
        )
        // 今天已经学过的新词：占用 1 个额度
        db.wordDao().insert(
            Word(
                word = "introduced-today",
                definition = "新词",
                box = 1,
                nextReviewEpochDay = today + 1,
                firstReviewedEpochDay = today,
                lastReviewedEpochDay = today
            )
        )
        repository.addWord(WordDraft("fresh", "还没学的新词"), today)

        val queue = repository.loadStudyQueue(today, sessionSize = 10, dailyNewWordLimit = 1)

        assertEquals(listOf("old"), queue.map { it.word })
    }

    @Test
    fun 今天首次复习的词会占用当日新词额度() = runTest {
        val id = (repository.addWord(WordDraft("first", "新词"), today) as SaveResult.Success).id
        // 首次复习 → firstReviewedEpochDay = today → 吃掉 1 个额度
        repository.submitReview(id, ReviewResult.KNOWN, today)
        repository.addWord(WordDraft("second", "另一个新词"), today)

        val queue = repository.loadStudyQueue(today, sessionSize = 10, dailyNewWordLimit = 1)

        // 额度已用完：队列里只有「到期」语义的词，不会再多引入新词
        assertTrue(queue.none { it.word == "second" })
    }

    @Test
    fun 没有可学的词时队列为空() = runTest {
        val queue = repository.loadStudyQueue(today, sessionSize = 10, dailyNewWordLimit = 10)
        assertTrue(queue.isEmpty())
    }

    // ---------- 统计 ----------

    @Test
    fun 统计数字与流水一致() = runTest {
        val id = (repository.addWord(WordDraft("abandon", "v. 放弃"), today) as SaveResult.Success).id
        repository.submitReview(id, ReviewResult.KNOWN, today)
        repository.submitReview(id, ReviewResult.KNOWN, today)
        repository.addWord(WordDraft("fresh", "新词"), today)

        val stats = repository.observeStats(today).first()

        assertEquals(2, stats.reviewedToday)
        assertEquals(1, stats.streakDays)
        assertEquals(2, stats.totalWords)
        assertEquals(1, stats.newWordCount)
        // 两次认识 → box 2
        assertEquals(1, stats.boxDistribution.firstOrNull { it.box == 2 }?.count)
    }

    // ---------- 导入 ----------

    @Test
    fun 导入会跳过重复与非法条目() = runTest {
        repository.addWord(WordDraft("apple", "苹果"), today)

        val summary = repository.importWords(
            listOf(
                WordDraft("apple", "苹果"),   // 重复
                WordDraft("banana", "香蕉"),  // 新增
                WordDraft("", "空单词")        // 非法
            ),
            today
        )

        assertEquals(1, summary.inserted)
        assertEquals(1, summary.duplicated)
        assertEquals(1, summary.invalid)
        assertEquals(2, repository.observeWords("").first().size)
    }
}
