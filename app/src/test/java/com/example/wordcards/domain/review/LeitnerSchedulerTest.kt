package com.example.wordcards.domain.review

import com.example.wordcards.domain.model.ReviewResult
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LeitnerScheduler 的行为契约测试。
 *
 * 这类纯函数测试是项目里性价比最高的一块：不依赖 Android、不依赖数据库，
 * 跑一次不到一秒，却能挡住「复习节奏算错」这类最难靠手点发现的 bug。
 */
class LeitnerSchedulerTest {

    private val scheduler = LeitnerScheduler()

    /** 固定一个基准日：2026-10-25。所有断言都基于它推算，避免测试依赖「今天」。 */
    private val today: Long = LocalDate.of(2026, 10, 25).toEpochDay()

    // ---------- 认识：盒子前进 ----------

    @Test
    fun `新词认识后进入 1 号盒 明天复习`() {
        val outcome = scheduler.schedule(currentBox = 0, result = ReviewResult.KNOWN, today = today)

        assertEquals(1, outcome.box)
        assertEquals(1, outcome.intervalDays)
        assertEquals(today + 1, outcome.nextReviewEpochDay)
    }

    @Test
    fun `3 号盒认识后进入 4 号盒 间隔 7 天`() {
        val outcome = scheduler.schedule(currentBox = 3, result = ReviewResult.KNOWN, today = today)

        assertEquals(4, outcome.box)
        assertEquals(7, outcome.intervalDays)
        assertEquals(today + 7, outcome.nextReviewEpochDay)
    }

    @Test
    fun `5 号盒认识后仍是 5 号盒 间隔不继续增长`() {
        val outcome = scheduler.schedule(currentBox = 5, result = ReviewResult.KNOWN, today = today)

        assertEquals(ReviewScheduler.MAX_BOX, outcome.box)
        assertEquals(15, outcome.intervalDays)
        assertEquals(today + 15, outcome.nextReviewEpochDay)
    }

    // ---------- 不认识：归零 ----------

    @Test
    fun `不认识时盒子归零 且当天重新到期`() {
        val outcome = scheduler.schedule(currentBox = 4, result = ReviewResult.UNKNOWN, today = today)

        assertEquals(0, outcome.box)
        assertEquals(0, outcome.intervalDays)
        assertEquals(today, outcome.nextReviewEpochDay)
    }

    @Test
    fun `不认识不会把日期推到未来`() {
        val outcome = scheduler.schedule(currentBox = 0, result = ReviewResult.UNKNOWN, today = today)

        assertTrue(outcome.nextReviewEpochDay <= today)
        assertEquals(0, outcome.box)
    }

    // ---------- 越界自愈 ----------

    @Test
    fun `盒子超出上限时按上限处理`() {
        val outcome = scheduler.schedule(currentBox = 7, result = ReviewResult.KNOWN, today = today)

        assertEquals(ReviewScheduler.MAX_BOX, outcome.box)
    }

    @Test
    fun `盒子为负数时按 0 号盒处理`() {
        val outcome = scheduler.schedule(currentBox = -3, result = ReviewResult.KNOWN, today = today)

        assertEquals(1, outcome.box)
        assertEquals(today + 1, outcome.nextReviewEpochDay)
    }

    // ---------- 间隔表 ----------

    @Test
    fun `默认间隔表与设计文档一致`() {
        assertEquals(listOf(0, 1, 2, 4, 7, 15), LeitnerScheduler.DEFAULT_INTERVAL_DAYS)
    }

    @Test
    fun `intervalDaysFor 对越界入参同样收敛`() {
        assertEquals(15, scheduler.intervalDaysFor(99))
        assertEquals(0, scheduler.intervalDaysFor(-1))
    }

    @Test
    fun `间隔表可以注入 用于将来换节奏`() {
        val fastScheduler = LeitnerScheduler(intArrayOf(0, 1, 1, 1, 1, 1))
        val outcome = fastScheduler.schedule(currentBox = 4, result = ReviewResult.KNOWN, today = today)

        assertEquals(5, outcome.box)
        assertEquals(1, outcome.intervalDays)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `间隔表长度不足时构造失败`() {
        LeitnerScheduler(intArrayOf(0, 1, 2))
    }

    // ---------- 连续推进 ----------

    @Test
    fun `连续 5 次认识 复习日期按 1-2-4-7-15 累加`() {
        val expectedIntervals = listOf(1, 2, 4, 7, 15)
        var box = 0
        var cursor = today

        expectedIntervals.forEach { expectedInterval ->
            val outcome = scheduler.schedule(currentBox = box, result = ReviewResult.KNOWN, today = cursor)
            assertEquals(expectedInterval, outcome.intervalDays)
            box = outcome.box
            cursor = outcome.nextReviewEpochDay
        }

        assertEquals(5, box)
        // 今天是 2026-10-25，累加 1+2+4+7+15 = 29 天后进入最终复习日
        assertEquals(LocalDate.of(2026, 11, 23).toEpochDay(), cursor)
    }

    @Test
    fun `学到一半忘记 之后重新从 1 号盒开始`() {
        // 一路推到 3 号盒
        var box = 0
        repeat(3) {
            box = scheduler.schedule(box, ReviewResult.KNOWN, today).box
        }
        assertEquals(3, box)

        // 忘记 → 归零
        val forgot = scheduler.schedule(box, ReviewResult.UNKNOWN, today)
        assertEquals(0, forgot.box)
        assertEquals(today, forgot.nextReviewEpochDay)

        // 再认识一次只到 1 号盒，不会跳回 3 号盒
        val recovered = scheduler.schedule(forgot.box, ReviewResult.KNOWN, today)
        assertEquals(1, recovered.box)
        assertEquals(today + 1, recovered.nextReviewEpochDay)
    }
}
