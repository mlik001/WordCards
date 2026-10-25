package com.example.wordcards.util

import java.time.Clock
import java.time.LocalDate

/**
 * 全局唯一的「今天是哪天」入口。
 *
 * 为什么要包一层：复习算法依赖日期，而日期一旦直接读系统时间就无法单测。
 * 后续所有需要「今天」的地方都必须经这里，`Clock` 由 AppContainer 注入，
 * 测试时传 `Clock.fixed(...)` 即可模拟任意一天、跨天、跨时区。
 *
 * 注意：`java.time` 在 API 26+ 才原生可用，minSdk 24 依赖 core library desugaring，
 * 已在 app/build.gradle.kts 中开启，不要随手关掉。
 */
object DateProvider {

    /** 当天日期，以 epochDay 表示（1970-01-01 起的天数）。 */
    fun todayEpochDay(clock: Clock = Clock.systemDefaultZone()): Long =
        LocalDate.now(clock).toEpochDay()

    /** epochDay → LocalDate，用于格式化展示。 */
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** 从今天到目标日还有几天；负数表示已逾期。 */
    fun daysUntil(targetEpochDay: Long, todayEpochDay: Long): Long =
        targetEpochDay - todayEpochDay

    /** 是否已到期（含今天）。 */
    fun isDue(nextReviewEpochDay: Long, todayEpochDay: Long): Boolean =
        nextReviewEpochDay <= todayEpochDay
}
