package com.example.wordcards.domain.model

/**
 * 一次复习的计算结果。
 *
 * 只描述「复习后这张卡应该是什么状态」，不包含 id / 计数 / 时间戳，
 * 这样算法本身不依赖实体与数据库，可以直接在 JVM 单测里跑。
 *
 * @param box                 复习后的盒子（0..ReviewScheduler.MAX_BOX）
 * @param nextReviewEpochDay  下次复习日（LocalDate.toEpochDay()）
 * @param intervalDays        本次距下次复习的天数，供 UI 显示「3 天后再见」
 */
data class ReviewOutcome(
    val box: Int,
    val nextReviewEpochDay: Long,
    val intervalDays: Int
)
