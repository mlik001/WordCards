package com.example.wordcards.domain.review

/**
 * 会话队列组装：把「到期复习词」和「新词」按配额拼成一轮。
 *
 * 三条规则（顺序不能反）：
 * 1. 到期复习优先，欠账先还；
 * 2. 剩余名额才给新词；
 * 3. 新词还要再受「每日新词上限 - 今天已引入」约束。
 *
 * 用泛型而不是直接写 `List<Word>`：domain 层不应该反过来依赖 data 层的实体，
 * 保持「domain 是纯 Kotlin」这条边界，也让这个函数能拿任意占位类型做单测。
 */
object StudyQueueBuilder {

    fun <T> assemble(
        dueWords: List<T>,
        newWordCandidates: List<T>,
        sessionSize: Int,
        introducedToday: Int,
        dailyNewWordLimit: Int
    ): List<T> {
        if (sessionSize <= 0) return emptyList()

        val reviews = dueWords.take(sessionSize)
        val remainingSlots = (sessionSize - reviews.size).coerceAtLeast(0)
        val newQuota = (dailyNewWordLimit - introducedToday).coerceAtLeast(0)
        val newTake = minOf(remainingSlots, newQuota, newWordCandidates.size)

        return if (newTake <= 0) reviews else reviews + newWordCandidates.take(newTake)
    }
}
