package com.example.wordcards.domain.review

import com.example.wordcards.domain.model.ReviewOutcome
import com.example.wordcards.domain.model.ReviewResult

/**
 * Leitner 盒子算法（0~5 盒）。
 *
 * 规则：
 * - 认识   → box + 1（封顶 5），下次复习 = 今天 + intervals[新 box]
 * - 不认识 → box 归零，下次复习 = 今天（当天仍到期，允许本轮重现）
 *
 * 三条刻意的设计：
 * 1. 不读系统时间。`today` 由调用方传入并在整轮会话中冻结，
 *    否则凌晨复习会出现「每答一张，到期集合就变一次」。
 * 2. 不认识是「归零」而非「减一」。Leitner 的假设是遗忘即记忆强度不可信，
 *    减一还需要额外处理下界，多一个分支就多一处可能出错的边界。
 * 3. 越界输入一律 coerce，不抛异常。DAO 里的历史数据若被外部改坏，
 *    算法层要能自愈，而不是让整个复习流程崩掉。
 *
 * @param intervals 索引即盒子编号，值为「认识后需等待的天数」，
 *                  长度必须 ≥ [ReviewScheduler.MAX_BOX] + 1。
 */
class LeitnerScheduler(
    private val intervals: IntArray = DEFAULT_INTERVALS.copyOf()
) : ReviewScheduler {

    init {
        require(intervals.size > ReviewScheduler.MAX_BOX) {
            "intervals 至少需要 ${ReviewScheduler.MAX_BOX + 1} 项，当前 ${intervals.size} 项"
        }
        require(intervals.all { it >= 0 }) { "intervals 不允许负数" }
    }

    override fun schedule(
        currentBox: Int,
        result: ReviewResult,
        today: Long
    ): ReviewOutcome {
        val safeBox = currentBox.coerceIn(0, ReviewScheduler.MAX_BOX)
        return when (result) {
            ReviewResult.KNOWN -> {
                val newBox = (safeBox + 1).coerceAtMost(ReviewScheduler.MAX_BOX)
                val interval = intervals[newBox]
                ReviewOutcome(
                    box = newBox,
                    nextReviewEpochDay = today + interval,
                    intervalDays = interval
                )
            }

            ReviewResult.UNKNOWN -> ReviewOutcome(
                box = 0,
                nextReviewEpochDay = today,
                intervalDays = 0
            )
        }
    }

    override fun intervalDaysFor(box: Int): Int =
        intervals[box.coerceIn(0, ReviewScheduler.MAX_BOX)]

    companion object {
        /** 各盒子「认识后」的等待天数：0 天 / 1 天 / 2 天 / 4 天 / 7 天 / 15 天。 */
        val DEFAULT_INTERVALS: IntArray = intArrayOf(0, 1, 2, 4, 7, 15)

        /** 供 UI 与文档使用的只读视图，避免把可变数组暴露出去。 */
        val DEFAULT_INTERVAL_DAYS: List<Int> = DEFAULT_INTERVALS.toList()
    }
}
