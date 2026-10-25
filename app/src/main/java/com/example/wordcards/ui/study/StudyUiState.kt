package com.example.wordcards.ui.study

/** 复习页的四个阶段。用枚举而不是 4 个布尔，让「既在加载又已完成」这种非法状态无法表达。 */
enum class StudyPhase { LOADING, EMPTY, STUDYING, FINISHED }

data class StudyCardUi(
    val id: Long,
    val word: String,
    val phonetic: String?,
    val definition: String,
    val example: String?,
    val box: Int
)

data class StudyUiState(
    val phase: StudyPhase = StudyPhase.LOADING,
    val card: StudyCardUi? = null,
    val isFlipped: Boolean = false,
    val isSubmitting: Boolean = false,
    /** 本轮总卡数（分母，会话开始时固定） */
    val totalCount: Int = 0,
    /** 队列里还剩多少张（含当前这张） */
    val remaining: Int = 0,
    /** 已经答过的**不重复**卡数，用作进度条的分子 */
    val answeredCount: Int = 0,
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val requeuedCount: Int = 0
) {
    val progress: Float
        get() = if (totalCount <= 0) 0f else (answeredCount.toFloat() / totalCount).coerceIn(0f, 1f)
}
