package com.example.wordcards.domain.review

import com.example.wordcards.domain.model.ReviewOutcome
import com.example.wordcards.domain.model.ReviewResult

/**
 * 复习调度策略。
 *
 * 抽成接口的目的：MVP 用 Leitner，将来若要换 SM-2 / FSRS，
 * 只需新增一个实现并在 AppContainer 里换一行装配，ViewModel 与 DAO 零改动。
 */
interface ReviewScheduler {

    /**
     * 计算复习后的卡片状态。
     *
     * @param currentBox 当前盒子，允许传入越界值，实现方必须自行收敛
     * @param result     用户判断
     * @param today      本次会话冻结的当天（epochDay），整轮复习不变
     */
    fun schedule(currentBox: Int, result: ReviewResult, today: Long): ReviewOutcome

    /** 某个盒子对应的等待天数，供 UI 展示规则说明。 */
    fun intervalDaysFor(box: Int): Int

    companion object {
        /** 盒子上限：5 */
        const val MAX_BOX: Int = 5
    }
}
