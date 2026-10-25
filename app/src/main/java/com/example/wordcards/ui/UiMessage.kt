package com.example.wordcards.ui

/**
 * 一次性的 UI 消息。
 *
 * 为什么不直接在 ViewModel 里拼中文：文案属于 UI 层资源（要支持多语言、要能改），
 * ViewModel 只产出「发生了什么」，由 Composable 用 stringResource 翻译成文字。
 */
sealed interface UiMessage {

    data class WordDeleted(val word: String) : UiMessage

    data class Exported(val count: Int) : UiMessage

    data class Imported(val inserted: Int, val skipped: Int) : UiMessage

    /** 导入一本内置词书的结果 */
    data class BookImported(val bookName: String, val inserted: Int, val skipped: Int) : UiMessage

    data class Failed(val reason: String?) : UiMessage

    /** 用户拒绝了通知权限，提醒无法生效。 */
    data object PermissionDenied : UiMessage
}
