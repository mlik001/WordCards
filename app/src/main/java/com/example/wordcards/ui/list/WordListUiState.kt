package com.example.wordcards.ui.list

import com.example.wordcards.ui.UiMessage

/** 列表项展示模型：只带 UI 需要的东西，不带 createdAt 之类的内部字段。 */
data class WordListItem(
    val id: Long,
    val word: String,
    val phonetic: String?,
    val definition: String,
    val box: Int,
    /** 距下次复习还有几天，负数表示已逾期；具体文案由 UI 层翻译 */
    val daysUntilDue: Long
)

data class WordListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val words: List<WordListItem> = emptyList(),
    val totalCount: Int = 0,
    val dueCount: Int = 0,
    val newWordCount: Int = 0,
    val pendingDelete: WordListItem? = null,
    val message: UiMessage? = null,
    val isBusy: Boolean = false
) {
    val hasStudyWork: Boolean get() = dueCount + newWordCount > 0

    val isSearching: Boolean get() = query.isNotBlank()
}
