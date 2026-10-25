package com.example.wordcards.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 复习流水。
 *
 * 为什么 MVP 就要这张表：`Word` 上的 `correctCount / wrongCount` 只能回答「一共答对几次」，
 * 回答不了「今天复习了多少」「连续打卡几天」「这周的学习曲线」。等做统计页时再补表，
 * 就要写 Room 迁移；而现在多一条 insert 的成本几乎为零。
 *
 * `known` 用 Boolean 而不是枚举：Room 存枚举需要 TypeConverter，
 * 而这个字段只有两种取值，Boolean 语义清晰且零配置。
 */
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reviewedAtEpochDay"]),
        Index(value = ["wordId"])
    ]
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val wordId: Long,

    /** 复习发生在哪一天（epochDay），按日聚合用 */
    val reviewedAtEpochDay: Long,

    /** 精确毫秒时间戳，用于将来做「一天内的时间分布」 */
    val reviewedAtMillis: Long,

    /** true = 认识，false = 不认识 */
    val known: Boolean,

    val boxBefore: Int,
    val boxAfter: Int
)
