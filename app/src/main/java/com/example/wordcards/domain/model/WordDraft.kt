package com.example.wordcards.domain.model

/** 校验失败时指出是哪个字段的问题，UI 才能把错误显示在对应输入框下面。 */
enum class DraftField { WORD, DEFINITION }

/** 表单载荷：UI 只提交用户真正填的内容，id / 计数 / 日期由数据层负责。 */
data class WordDraft(
    val word: String,
    val definition: String,
    val phonetic: String? = null,
    val example: String? = null
) {
    /** 统一清洗：去首尾空格，空串转 null。 */
    fun normalized(): WordDraft = copy(
        word = word.trim(),
        definition = definition.trim(),
        phonetic = phonetic?.trim()?.takeIf { it.isNotEmpty() },
        example = example?.trim()?.takeIf { it.isNotEmpty() }
    )
}
