package com.example.wordcards.domain.stats

/**
 * 连续打卡天数计算。
 *
 * 规则与主流背单词 App 一致：
 * - 今天复习过 → 从今天往回数；
 * - 今天还没复习，但昨天复习过 → 从昨天往回数，连续记录**不断**（否则用户早上打开就掉签）；
 * - 最近一次复习早于昨天 → 连续为 0。
 *
 * 抽成纯函数的原因：这是统计里唯一有分支的逻辑，放 Repository 里就没法单测。
 */
object StreakCalculator {

    /**
     * @param activeDaysDesc 有复习记录的日期（epochDay），倒序，可重复
     * @param today          今天的 epochDay
     */
    fun streak(activeDaysDesc: List<Long>, today: Long): Int {
        if (activeDaysDesc.isEmpty()) return 0

        val days = activeDaysDesc.distinct().sortedDescending()
        val latest = days.first()

        val startFrom = when {
            latest == today -> today
            latest == today - 1 -> today - 1
            else -> return 0
        }

        var streak = 0
        var cursor = startFrom
        for (day in days) {
            when {
                day == cursor -> {
                    streak++
                    cursor--
                }
                // 已经数到断点，后面的日期更早，直接结束
                day < cursor -> return streak
                // day > cursor 只可能出现在起始判断之外，忽略
                else -> Unit
            }
        }
        return streak
    }
}
