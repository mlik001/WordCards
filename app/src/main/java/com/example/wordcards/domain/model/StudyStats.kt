package com.example.wordcards.domain.model

/** 一个盒子里有多少词，用于统计页的柱状分布。 */
data class BoxBucket(val box: Int, val count: Int)

/**
 * 统计页需要的全部数字，一次算好再交给 UI。
 *
 * 为什么不让 UI 自己从几条 Flow 里各取所需：那样会出现「总数已更新、分布还没更新」的
 * 中间态，画面上就是几个数字对不上。聚合成一个对象保证同帧渲染。
 */
data class StudyStats(
    val totalWords: Int = 0,
    val dueCount: Int = 0,
    val newWordCount: Int = 0,
    val masteredCount: Int = 0,
    val reviewedToday: Int = 0,
    val streakDays: Int = 0,
    val boxDistribution: List<BoxBucket> = emptyList()
)
