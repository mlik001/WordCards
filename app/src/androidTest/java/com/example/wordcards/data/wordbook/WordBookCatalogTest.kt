package com.example.wordcards.data.wordbook

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 词书资源解析测试。
 *
 * 词书是手工维护的 JSON，最容易出的是「漏了逗号/中英文引号/词条重复」这类低级错误，
 * 而它们在真机上表现为「词书页空白」，很难查。这里直接把 assets 里的每本书都解析一遍。
 */
@RunWith(AndroidJUnit4::class)
class WordBookCatalogTest {

    private lateinit var catalog: WordBookCatalog

    @Before
    fun setUp() {
        catalog = WordBookCatalog(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun 至少内置一本词书() {
        val packs = catalog.loadAll()
        assertTrue("assets/wordbooks 下没有可解析的词书", packs.isNotEmpty())
    }

    @Test
    fun 六级高频词可解析且词条数量正常() {
        val pack = catalog.load("cet6_high_freq")
        assertNotNull(pack)
        pack!!

        assertEquals("六级高频词", pack.name)
        assertTrue("词条太少，可能解析出错：${pack.totalCount}", pack.totalCount >= 300)
    }

    @Test
    fun 所有词条都有单词与释义() {
        val pack = catalog.load("cet6_high_freq")!!

        val broken = pack.words.filter { it.word.isBlank() || it.definition.isBlank() }
        assertTrue("存在空单词或空释义：$broken", broken.isEmpty())
    }

    @Test
    fun 词书内部没有重复单词() {
        val pack = catalog.load("cet6_high_freq")!!

        val duplicated = pack.words
            .groupBy { it.word.trim().lowercase() }
            .filterValues { it.size > 1 }
            .keys

        assertTrue("存在重复单词：$duplicated", duplicated.isEmpty())
    }

    @Test
    fun 每本词书的id与文件名无关且不重复() {
        val ids = catalog.loadAll().map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }
}
