package com.example.wordcards.ui.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordcards.data.backup.WordBackupService
import com.example.wordcards.data.local.entity.Word
import com.example.wordcards.data.repository.WordRepository
import com.example.wordcards.ui.UiMessage
import com.example.wordcards.util.DateProvider
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 列表页状态。
 *
 * 数据流：搜索词 → flatMapLatest(查库) → 与「到期数 / 新词数 / 本地交互状态」组合成一个 UiState。
 * 用 flatMapLatest 而不是自己维护列表：搜索词一变，旧查询自动取消，
 * 不会出现「先输入 a 后输入 ab，结果却是 a 的」这种乱序。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WordListViewModel(
    private val repository: WordRepository,
    private val backupService: WordBackupService,
    clock: Clock
) : ViewModel() {

    /**
     * 列表页生命周期内固定的「今天」。
     * ViewModel 存活期间跨零点的概率极低，为它加一层刷新反而会让日期文案在滚动时突然跳动。
     */
    private val today: Long = DateProvider.todayEpochDay(clock)

    private val query = MutableStateFlow("")
    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<WordListUiState> = combine(
        query.flatMapLatest { repository.observeWords(it) },
        repository.observeDueCount(today),
        repository.observeNewWordCount(),
        query,
        local
    ) { words, dueCount, newWordCount, currentQuery, localState ->
        WordListUiState(
            isLoading = false,
            query = currentQuery,
            words = words.map { it.toItem(today) },
            totalCount = words.size,
            dueCount = dueCount,
            newWordCount = newWordCount,
            pendingDelete = localState.pendingDelete,
            message = localState.message,
            isBusy = localState.isBusy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WordListUiState()
    )

    // ---------- 搜索 ----------

    fun onQueryChange(value: String) {
        query.value = value
    }

    // ---------- 删除 ----------

    fun onDeleteClick(item: WordListItem) {
        local.update { it.copy(pendingDelete = item) }
    }

    fun onDeleteDismiss() {
        local.update { it.copy(pendingDelete = null) }
    }

    fun onDeleteConfirm() {
        val target = local.value.pendingDelete ?: return
        local.update { it.copy(pendingDelete = null, isBusy = true) }
        viewModelScope.launch {
            repository.deleteWord(target.id)
            local.update {
                it.copy(isBusy = false, message = UiMessage.WordDeleted(target.word))
            }
        }
    }

    // ---------- 导入导出 ----------

    fun onExport(uri: Uri) {
        local.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            runCatching { backupService.exportTo(uri) }
                .onSuccess { count ->
                    local.update { it.copy(isBusy = false, message = UiMessage.Exported(count)) }
                }
                .onFailure { error ->
                    local.update { it.copy(isBusy = false, message = UiMessage.Failed(error.message)) }
                }
        }
    }

    fun onImport(uri: Uri) {
        local.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            runCatching { backupService.importFrom(uri) }
                .onSuccess { summary ->
                    local.update {
                        it.copy(
                            isBusy = false,
                            message = UiMessage.Imported(summary.inserted, summary.duplicated)
                        )
                    }
                }
                .onFailure { error ->
                    local.update { it.copy(isBusy = false, message = UiMessage.Failed(error.message)) }
                }
        }
    }

    fun onMessageShown() {
        local.update { it.copy(message = null) }
    }

    private fun Word.toItem(today: Long) = WordListItem(
        id = id,
        word = word,
        phonetic = phonetic,
        definition = definition,
        box = box,
        daysUntilDue = nextReviewEpochDay - today
    )

    private data class LocalState(
        val pendingDelete: WordListItem? = null,
        val message: UiMessage? = null,
        val isBusy: Boolean = false
    )
}
