package com.example.wordcards.domain.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 会话队列组装的规则测试。
 *
 * 用 Int 当占位类型，正好验证了「domain 不依赖 data 实体」这条边界是成立的。
 */
class StudyQueueBuilderTest {

    private val due = listOf(101, 102, 103, 104, 105)
    private val candidates = listOf(201, 202, 203, 204, 205)

    @Test
    fun `到期词优先 且不超过会话长度`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = due,
            newWordCandidates = candidates,
            sessionSize = 3,
            introducedToday = 0,
            dailyNewWordLimit = 10
        )
        assertEquals(listOf(101, 102, 103), queue)
    }

    @Test
    fun `名额被到期词占满时 不插入新词`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = due,
            newWordCandidates = candidates,
            sessionSize = 5,
            introducedToday = 0,
            dailyNewWordLimit = 10
        )
        assertEquals(due, queue)
    }

    @Test
    fun `剩余名额给新词`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = listOf(101, 102),
            newWordCandidates = candidates,
            sessionSize = 5,
            introducedToday = 0,
            dailyNewWordLimit = 10
        )
        assertEquals(listOf(101, 102, 201, 202, 203), queue)
    }

    @Test
    fun `今日新词额度已用完 只复习旧词`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = listOf(101),
            newWordCandidates = candidates,
            sessionSize = 10,
            introducedToday = 10,
            dailyNewWordLimit = 10
        )
        assertEquals(listOf(101), queue)
    }

    @Test
    fun `额度只剩两个 就只插两个新词`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = listOf(101),
            newWordCandidates = candidates,
            sessionSize = 10,
            introducedToday = 8,
            dailyNewWordLimit = 10
        )
        assertEquals(listOf(101, 201, 202), queue)
    }

    @Test
    fun `剩余名额与新词额度同时限制时 取较小者`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = listOf(101, 102),
            newWordCandidates = candidates,
            sessionSize = 6,
            introducedToday = 7,
            dailyNewWordLimit = 10
        )
        // 剩 4 个名额，但新词只剩 3 个额度 → 取 3
        assertEquals(listOf(101, 102, 201, 202, 203), queue)
    }

    @Test
    fun `每日新词上限为 0 时 只复习不学新词`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = listOf(101),
            newWordCandidates = candidates,
            sessionSize = 10,
            introducedToday = 0,
            dailyNewWordLimit = 0
        )
        assertEquals(listOf(101), queue)
    }

    @Test
    fun `候选新词不够时 不补空`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = emptyList(),
            newWordCandidates = listOf(201),
            sessionSize = 10,
            introducedToday = 0,
            dailyNewWordLimit = 10
        )
        assertEquals(listOf(201), queue)
    }

    @Test
    fun `两者都为空时返回空队列`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = emptyList<Int>(),
            newWordCandidates = emptyList<Int>(),
            sessionSize = 10,
            introducedToday = 0,
            dailyNewWordLimit = 10
        )
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `会话长度为 0 或负数时返回空`() {
        assertTrue(
            StudyQueueBuilder.assemble(due, candidates, 0, 0, 10).isEmpty()
        )
        assertTrue(
            StudyQueueBuilder.assemble(due, candidates, -5, 0, 10).isEmpty()
        )
    }

    @Test
    fun `已引入数量超过上限时不会算出负数名额`() {
        val queue = StudyQueueBuilder.assemble(
            dueWords = listOf(101),
            newWordCandidates = candidates,
            sessionSize = 5,
            introducedToday = 99,
            dailyNewWordLimit = 10
        )
        assertEquals(listOf(101), queue)
    }
}
