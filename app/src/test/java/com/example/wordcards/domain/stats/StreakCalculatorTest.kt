package com.example.wordcards.domain.stats

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 连续打卡计算的行为契约。
 *
 * 这块逻辑的坑全在「今天还没学」这个分支上：多数实现会把它算成断签，
 * 用户早上打开 App 看到连续记录归零，会直接影响留存。
 */
class StreakCalculatorTest {

    private val today: Long = LocalDate.of(2026, 10, 25).toEpochDay()
    private fun day(offset: Long): Long = today + offset

    @Test
    fun `没有任何记录时连续为 0`() {
        assertEquals(0, StreakCalculator.streak(emptyList(), today))
    }

    @Test
    fun `只有今天复习过 连续为 1`() {
        assertEquals(1, StreakCalculator.streak(listOf(day(0)), today))
    }

    @Test
    fun `今天和昨天都复习 连续为 2`() {
        assertEquals(2, StreakCalculator.streak(listOf(day(0), day(-1)), today))
    }

    @Test
    fun `今天还没开始 昨天复习过 连续不断`() {
        assertEquals(1, StreakCalculator.streak(listOf(day(-1)), today))
    }

    @Test
    fun `最近一次是前天 连续归零`() {
        assertEquals(0, StreakCalculator.streak(listOf(day(-2)), today))
    }

    @Test
    fun `连续五天 从今天往回全部命中`() {
        val days = listOf(0L, -1L, -2L, -3L, -4L).map(::day)
        assertEquals(5, StreakCalculator.streak(days, today))
    }

    @Test
    fun `中间断一天 只算到断点为止`() {
        // 今天、昨天，然后空一天，再往前三天
        val days = listOf(day(0), day(-1), day(-3), day(-4), day(-5))
        assertEquals(2, StreakCalculator.streak(days, today))
    }

    @Test
    fun `重复日期不会重复计数`() {
        val days = listOf(day(0), day(0), day(-1), day(-1))
        assertEquals(2, StreakCalculator.streak(days, today))
    }

    @Test
    fun `乱序输入也能正确计算`() {
        val days = listOf(day(-2), day(0), day(-1))
        assertEquals(3, StreakCalculator.streak(days, today))
    }

    @Test
    fun `今天没学但昨天和前天一整段连续 从昨天开始数`() {
        val days = listOf(day(-1), day(-2), day(-3))
        assertEquals(3, StreakCalculator.streak(days, today))
    }

    @Test
    fun `跨月连续也能正确累加`() {
        val base = LocalDate.of(2026, 11, 2).toEpochDay()
        val days = listOf(base, base - 1, base - 2)
        assertEquals(3, StreakCalculator.streak(days, base))
    }
}
