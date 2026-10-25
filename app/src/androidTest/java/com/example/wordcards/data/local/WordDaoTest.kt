package com.example.wordcards.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wordcards.data.local.dao.WordDao
import com.example.wordcards.data.local.entity.ReviewLog
import com.example.wordcards.data.local.entity.Word
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
 * WordDao 的 instrumented 测试。
 *
 * 为什么必须跑在设备上：Room 的实现、SQLite 的 COLLATE NOCASE、外键级联都是平台行为，
 * JVM 单测里的 sqlite-jdbc 并不等价。这里用 inMemoryDatabaseBuilder，
 * 每个用例一个干净库，互不影响。
 */
@RunWith(AndroidJUnit4::class)
class WordDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WordDao

    private val today = 20_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.wordDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---------- 写入与唯一性 ----------

    @Test
    fun 插入后能读回() = runTest {
        val id = dao.insert(word("abandon"))
        assertTrue(id > 0)
        assertEquals("abandon", dao.getById(id)?.word)
    }

    @Test
    fun 大小写不同的重复单词会被唯一索引拦下() = runTest {
        val first = dao.insert(word("apple"))
        val second = dao.insert(word("Apple"))

        assertTrue(first > 0)
        assertEquals(-1L, second)
        assertEquals(1, dao.observeWords("").first().size)
    }

    @Test
    fun 编辑查重会排除自身() = runTest {
        val id = dao.insert(word("apple"))
        assertEquals(0, dao.countByWord("APPLE", excludeId = id))
        assertEquals(1, dao.countByWord("Apple", excludeId = -1L))
    }

    // ---------- 到期队列 ----------

    @Test
    fun 到期队列只含复习过的词且按逾期与盒子排序() = runTest {
        dao.insert(word("fresh"))                                             // 新词，不该出现
        dao.insert(word("due-today", box = 1, next = today, firstReviewed = today - 5))
        dao.insert(word("overdue-box2", box = 2, next = today - 3, firstReviewed = today - 9))
        dao.insert(word("overdue-box0", box = 0, next = today - 3, firstReviewed = today - 4))
        dao.insert(word("future", box = 3, next = today + 1, firstReviewed = today - 1))

        val due = dao.getDueWords(today, 10).map { it.word }

        // 逾期最久优先；同一天按盒子升序
        assertEquals(listOf("overdue-box0", "overdue-box2", "due-today"), due)
    }

    @Test
    fun 到期队列受limit限制() = runTest {
        repeat(5) { index ->
            dao.insert(word("w$index", box = 1, next = today - index, firstReviewed = today))
        }
        assertEquals(2, dao.getDueWords(today, 2).size)
    }

    @Test
    fun 新词队列只取未复习的词并按创建时间先进先出() = runTest {
        dao.insert(word("created-first", createdAt = 1))
        dao.insert(word("created-later", createdAt = 2))
        dao.insert(word("already-reviewed", firstReviewed = today, createdAt = 0))

        assertEquals(
            listOf("created-first", "created-later"),
            dao.getNewWords(10).map { it.word }
        )
    }

    @Test
    fun 今日已引入新词数按首次复习日统计() = runTest {
        dao.insert(word("a", firstReviewed = today))
        dao.insert(word("b", firstReviewed = today))
        dao.insert(word("c", firstReviewed = today - 1))

        assertEquals(2, dao.countIntroducedOn(today))
        assertEquals(0, dao.countIntroducedOn(today + 1))
    }

    @Test
    fun 到期数与新词数分别统计() = runTest {
        dao.insert(word("due", next = today - 1, firstReviewed = today - 3))
        dao.insert(word("fresh1"))
        dao.insert(word("fresh2"))

        assertEquals(1, dao.observeDueCount(today).first())
        assertEquals(2, dao.observeNewWordCount().first())
    }

    // ---------- 搜索与统计 ----------

    @Test
    fun 搜索同时匹配单词与释义() = runTest {
        dao.insert(word("benefit", definition = "n. 好处"))
        dao.insert(word("abandon", definition = "v. 放弃"))

        assertEquals(listOf("benefit"), dao.observeWords("ben").first().map { it.word })
        assertEquals(1, dao.observeWords("放弃").first().size)
        assertEquals(2, dao.observeWords("").first().size)
    }

    @Test
    fun 盒子分布按盒子分组() = runTest {
        dao.insert(word("a", box = 0))
        dao.insert(word("b", box = 0))
        dao.insert(word("c", box = 3))

        val distribution = dao.observeBoxDistribution().first().associate { it.box to it.count }

        assertEquals(2, distribution[0] ?: 0)
        assertEquals(1, distribution[3] ?: 0)
        assertNull(distribution[5])
    }

    @Test
    fun 已掌握统计按盒子阈值累计() = runTest {
        dao.insert(word("a", box = 3))
        dao.insert(word("b", box = 4))
        dao.insert(word("c", box = 5))

        assertEquals(2, dao.observeCountWithMinBox(4).first())
    }

    // ---------- 删除与外键 ----------

    @Test
    fun 删除单词会级联删除复习流水() = runTest {
        val id = dao.insert(word("gone", firstReviewed = today))
        db.reviewLogDao().insert(
            ReviewLog(
                wordId = id,
                reviewedAtEpochDay = today,
                reviewedAtMillis = 0L,
                known = true,
                boxBefore = 0,
                boxAfter = 1
            )
        )
        assertEquals(1, db.reviewLogDao().observeTotalReviewCount().first())

        dao.deleteById(id)

        assertNull(dao.getById(id))
        assertEquals(0, db.reviewLogDao().observeTotalReviewCount().first())
    }

    private fun word(
        name: String,
        definition: String = "释义-$name",
        box: Int = 0,
        next: Long = today,
        firstReviewed: Long? = null,
        createdAt: Long = 0L
    ) = Word(
        word = name,
        definition = definition,
        box = box,
        nextReviewEpochDay = next,
        firstReviewedEpochDay = firstReviewed,
        lastReviewedEpochDay = firstReviewed,
        createdAt = createdAt
    )
}
