package com.example.wordcards.domain.model

/** 用户在复习页对一张卡片做出的判断。 */
enum class ReviewResult {
    /** 认识：盒子 +1，间隔按新盒子顺延。 */
    KNOWN,

    /** 不认识：盒子归零，当天仍到期。 */
    UNKNOWN
}
