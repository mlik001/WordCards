package com.example.wordcards.data.wordbook

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wordcards.data.local.AppDatabase
import com.example.wordcards.data.repository.DefaultWordRepository
import com.example.wordcards.domain.review.LeitnerScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 词书导入的端到端测试：真实 assets + 真实 Room。
 *
 * 核心要验证的是**幂等**：用户很可能会连点两次导入，
 * 第二次必须全部算「跳过」而不是把库塞成两倍。
 */
@RunWith(AndroidJUnit4::class)
class WordBookServiceTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultWordRepository
    private lateinit var service: DefaultWordBookService

    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-25T08:00:00Z"), ZoneId.of("Asia/Shanghai"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = DefaultWordRepository(
            database = db,
            wordDao = db.wordDao(),
            reviewLogDao = db.reviewLogDao(),
            scheduler = LeitnerScheduler()
        )
        service = DefaultWordBookService(
            catalog = WordBookCatalog(context),
            repository = repository,
            clock = clock
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun 首次进入词书页时已导入数为零() = runTest {
        val books = service.listPacks()
        val cet6 = books.first { it.pack.id == "cet6_high_freq" }

        assertEquals(0, cet6.importedCount)
        assertTrue(!cet6.isFullyImported)
    }

    @Test
    fun 导入词书会把所有单词写入数据库() = runTest {
        val summary = service.importPack("cet6_high_freq")

        val total = WordBookCatalog(ApplicationProvider.getApplicationContext())
            .load("cet6_high_freq")!!.totalCount
        assertEquals(total, summary.inserted)
        assertEquals(0, summary.duplicated)
        assertEquals(total, repository.observeWords("").first().size)
    }

    @Test
    fun 重复导入是幂等的() = runTest {
        service.importPack("cet6_high_freq")
        val second = service.importPack("cet6_high_freq")

        assertEquals(0, second.inserted)
        assertTrue(second.duplicated > 0)

        // 库里不能出现重复条目
        val words = repository.observeWords("").first().map { it.word.lowercase() }
        assertEquals(words.size, words.distinct().size)
    }

    @Test
    fun 导入后进度满格且按钮应置灰() = runTest {
        service.importPack("cet6_high_freq")

        val cet6 = service.listPacks().first { it.pack.id == "cet6_high_freq" }
        assertEquals(cet6.totalCount, cet6.importedCount)
        assertTrue(cet6.isFullyImported)
    }

    @Test
    fun 用户已手动添加的词不会被重复导入() = runTest {
        // 用户先自己加了 abandon
        repository.addWord(com.example.wordcards.domain.model.WordDraft("abandon", "v. 放弃"), 20_000L)

        val summary = service.importPack("cet6_high_freq")

        // abandon 已存在 → 计入跳过，不新增
        assertTrue(summary.duplicated >= 1)
        assertEquals(1, repository.observeWords("abandon").first().size)
    }

    @Test
    fun 导入的词都是新词且当天到期() = runTest {
        service.importPack("cet6_high_freq")

        val today = com.example.wordcards.util.DateProvider.todayEpochDay(clock)
        val words = repository.observeWords("").first()
        assertTrue(words.all { it.box == 0 })
        assertTrue(words.all { it.firstReviewedEpochDay == null })
        assertTrue(words.all { it.nextReviewEpochDay == today })
    }
}
