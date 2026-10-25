package com.example.wordcards.data.wordbook

import android.content.Context
import com.example.wordcards.domain.model.WordDraft
import org.json.JSONObject

/**
 * 词书目录：扫描 `assets/wordbooks/` 下的所有 JSON。
 *
 * 新增词书只要往 assets 里丢一个同格式的 JSON，无需改代码——
 * 这也是将来「在线下载词书」的接入点：下载完写到本地目录，把 [loadAll] 的来源
 * 从 AssetManager 换成 File 即可，上层完全不用动。
 */
class WordBookCatalog(private val context: Context) {

    fun loadAll(): List<WordBookPack> {
        val names = runCatching {
            context.assets.list(ASSET_DIR).orEmpty()
                .filter { it.endsWith(".json") }
                .sorted()
        }.getOrDefault(emptyList())

        return names.mapNotNull { fileName -> runCatching { parse(read(fileName)) }.getOrNull() }
    }

    fun load(id: String): WordBookPack? = loadAll().firstOrNull { it.id == id }

    private fun read(fileName: String): String =
        context.assets.open("$ASSET_DIR/$fileName").bufferedReader().use { it.readText() }

    /**
     * 解析失败返回异常，由调用方 runCatching 兜住：
     * 一本词书写坏了不应该让整个词书页崩掉，跳过它继续展示其他词书。
     */
    private fun parse(json: String): WordBookPack {
        val root = JSONObject(json)
        val id = root.getString("id")
        val array = root.optJSONArray("words") ?: error("词书 $id 缺少 words 数组")

        val words = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val word = item.optString("word").trim()
                val definition = item.optString("definition").trim()
                if (word.isEmpty() || definition.isEmpty()) continue
                add(
                    WordDraft(
                        word = word,
                        definition = definition,
                        phonetic = item.optString("phonetic").trim().ifEmpty { null },
                        example = item.optString("example").trim().ifEmpty { null }
                    )
                )
            }
        }

        require(words.isNotEmpty()) { "词书 $id 没有有效词条" }

        return WordBookPack(
            id = id,
            name = root.optString("name").ifEmpty { id },
            description = root.optString("description"),
            version = root.optInt("version", 1),
            words = words
        )
    }

    companion object {
        private const val ASSET_DIR = "wordbooks"
    }
}
