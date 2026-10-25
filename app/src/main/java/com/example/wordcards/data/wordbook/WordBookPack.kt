package com.example.wordcards.data.wordbook

import com.example.wordcards.domain.model.WordDraft

/**
 * 一本内置词书。
 *
 * 数据来自 assets/wordbooks 目录下的 JSON，随 APK 发布，离线可用。
 * 之所以用「词书」而不是直接把词塞进数据库：用户可能同时想学六级和考研，
 * 需要自己决定导哪一本、什么时候导。
 */
data class WordBookPack(
    val id: String,
    val name: String,
    val description: String,
    val version: Int,
    val words: List<WordDraft>
) {
    val totalCount: Int get() = words.size
}

/** 词书 + 当前已导入进度。 */
data class WordBookSummary(
    val pack: WordBookPack,
    val importedCount: Int
) {
    val totalCount: Int get() = pack.totalCount

    val isFullyImported: Boolean get() = importedCount >= totalCount
}
