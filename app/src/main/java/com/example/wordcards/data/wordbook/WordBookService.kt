package com.example.wordcards.data.wordbook

import com.example.wordcards.data.repository.ImportSummary
import com.example.wordcards.data.repository.WordRepository
import com.example.wordcards.util.DateProvider
import java.time.Clock

/**
 * 词书服务：把「一本内置词书」变成「用户库里的单词」。
 *
 * 两个关键点：
 * 1. **导入是幂等的**——重复点导入只会把已有词算成「跳过」，不会产生重复条目
 *    （底层靠 Repository 的 NOCASE 查重）。
 * 2. **已导入数按单词名求交集**，不是按数量差。用户可能自己先手动加过几个词，
 *    数量差会算错，交并集合不会。
 */
interface WordBookService {
    suspend fun listPacks(): List<WordBookSummary>

    suspend fun importPack(packId: String): ImportSummary
}

class DefaultWordBookService(
    private val catalog: WordBookCatalog,
    private val repository: WordRepository,
    private val clock: Clock
) : WordBookService {

    override suspend fun listPacks(): List<WordBookSummary> {
        val existing = repository.exportAll()
            .map { it.word.trim().lowercase() }
            .toSet()

        return catalog.loadAll().map { pack ->
            val imported = pack.words.count { it.word.trim().lowercase() in existing }
            WordBookSummary(pack = pack, importedCount = imported)
        }
    }

    override suspend fun importPack(packId: String): ImportSummary {
        val pack = catalog.load(packId) ?: return ImportSummary()
        return repository.importWords(pack.words, DateProvider.todayEpochDay(clock))
    }
}
