package com.example.wordcards.ui.addedit

import com.example.wordcards.ui.UiMessage

/** 字段级错误，UI 负责翻译成文案。 */
enum class FieldError { REQUIRED, DUPLICATE }

data class AddEditUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val word: String = "",
    val definition: String = "",
    val phonetic: String = "",
    val example: String = "",
    val wordError: FieldError? = null,
    val definitionError: FieldError? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val notFound: Boolean = false,
    val message: UiMessage? = null
) {
    /** 保存按钮的可用性只跟「是否在提交」有关，不跟内容是否为空有关——
     *  允许用户点一下、由校验给出明确错误，比按钮静默灰掉更好懂。 */
    val canSubmit: Boolean get() = !isSaving && !saved
}
