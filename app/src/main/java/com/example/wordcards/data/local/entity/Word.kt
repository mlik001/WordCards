package com.example.wordcards.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词实体。
 *
 * 几条关键设计：
 * - `word` 列声明 COLLATE NOCASE，唯一索引才会把 "Apple" 和 "apple" 视为冲突，
 *   否则用户很容易录进两条看起来一样的词。
 * - `nextReviewEpochDay` 用「天」而不是毫秒时间戳：Leitner 的语义是「哪一天复习」，
 *   整数比较既不会受时区/夏令时影响，也能让到期查询走 (nextReviewEpochDay, box) 复合索引。
 * - `firstReviewedEpochDay` 用来区分「新词」与「复习词」。用 `correctCount + wrongCount == 0`
 *   也能判断新词，但「今天已经引入了几个新词」需要按天统计，单独存一列比事后推导可靠得多。
 * - `deckId` 是词书功能的外键占位，MVP 不用，加词书时无需重建表。
 */
@Entity(
    tableName = "words",
    indices = [
        Index(value = ["word"], unique = true),
        Index(value = ["nextReviewEpochDay", "box"]),
        Index(value = ["box"]),
        Index(value = ["firstReviewedEpochDay"])
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val word: String,

    /** 释义（中文） */
    val definition: String,

    /** 音标，选填 */
    val phonetic: String? = null,

    /** 例句，选填 */
    val example: String? = null,

    /** Leitner 盒子 0..5，0 = 新词/刚遗忘 */
    val box: Int = 0,

    /** 下次复习日（LocalDate.toEpochDay()） */
    val nextReviewEpochDay: Long = 0L,

    val correctCount: Int = 0,
    val wrongCount: Int = 0,

    /** 创建时间毫秒，仅用于列表排序与「今天新增」展示 */
    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    /** 上次复习日，null 表示从未复习 */
    val lastReviewedEpochDay: Long? = null,

    /** 首次复习日，null 表示仍是新词 */
    val firstReviewedEpochDay: Long? = null,

    /** 词书外键占位，MVP 恒为 null */
    val deckId: Long? = null
) {
    /** 复习总次数，用于统计与「是否新词」的兜底判断。 */
    val reviewCount: Int get() = correctCount + wrongCount
}
