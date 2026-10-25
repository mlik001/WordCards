package com.example.wordcards.ui.navigation

import com.example.wordcards.ui.addedit.AddEditViewModel

/**
 * 全部路由常量集中在这里。
 *
 * 用字符串路由而不是 type-safe 路由：课程设计阶段字符串更直观，
 * 出问题时能直接看懂 `word_edit?wordId=3` 是什么意思；
 * 进阶可以迁到 `@Serializable` 路由，迁移范围仅限本文件 + NavHost。
 */
object Routes {
    const val WORDS = "words"

    /** wordId 为查询参数，-1 表示新增 */
    const val WORD_EDIT = "word_edit?wordId={wordId}"

    const val STUDY = "study"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val WORDBOOKS = "wordbooks"

    const val ARG_WORD_ID = "wordId"

    fun wordEdit(wordId: Long = AddEditViewModel.NEW_WORD_ID): String = "word_edit?wordId=$wordId"
}
