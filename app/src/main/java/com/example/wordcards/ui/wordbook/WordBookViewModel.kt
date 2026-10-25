package com.example.wordcards.ui.wordbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordcards.data.wordbook.WordBookService
import com.example.wordcards.data.wordbook.WordBookSummary
import com.example.wordcards.ui.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordBookUiState(
    val isLoading: Boolean = true,
    val books: List<WordBookSummary> = emptyList(),
    /** 正在导入的词书 id，非空时禁用其他导入按钮，避免并发写库 */
    val importingId: String? = null,
    val message: UiMessage? = null
)

class WordBookViewModel(
    private val service: WordBookService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordBookUiState())
    val uiState: StateFlow<WordBookUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    /** 每次进入或导入完都重新算一遍「已导入 N 词」，数字要跟列表页对得上。 */
    fun reload() {
        viewModelScope.launch {
            val books = runCatching { service.listPacks() }.getOrDefault(emptyList())
            _uiState.update { it.copy(isLoading = false, books = books) }
        }
    }

    fun onImport(packId: String) {
        val state = _uiState.value
        if (state.importingId != null) return
        val book = state.books.firstOrNull { it.pack.id == packId } ?: return
        if (book.isFullyImported) return

        _uiState.update { it.copy(importingId = packId) }
        viewModelScope.launch {
            runCatching { service.importPack(packId) }
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            importingId = null,
                            message = UiMessage.BookImported(
                                bookName = book.pack.name,
                                inserted = summary.inserted,
                                skipped = summary.duplicated
                            )
                        )
                    }
                    reload()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(importingId = null, message = UiMessage.Failed(error.message))
                    }
                }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
