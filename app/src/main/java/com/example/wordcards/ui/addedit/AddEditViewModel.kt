package com.example.wordcards.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordcards.data.repository.WordRepository
import com.example.wordcards.domain.model.DraftField
import com.example.wordcards.domain.model.SaveResult
import com.example.wordcards.domain.model.WordDraft
import com.example.wordcards.ui.UiMessage
import com.example.wordcards.util.DateProvider
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 新增 / 编辑共用一个 ViewModel 和一个页面。
 *
 * 为什么不分两个页面：字段、校验、提交逻辑完全一致，分成两份就会出现
 * 「新增能拦截重复，编辑忘了拦截」这种不对称 bug。
 * 用 wordId == -1 区分模式，成本为零。
 */
class AddEditViewModel(
    private val repository: WordRepository,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 由导航参数注入；-1 表示新增。 */
    private val wordId: Long = savedStateHandle.get<Long>(ARG_WORD_ID) ?: NEW_WORD_ID

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (wordId == NEW_WORD_ID) {
            _uiState.update { it.copy(isLoading = false, isEditMode = false) }
        } else {
            loadExisting(wordId)
        }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            val word = repository.getWord(id)
            if (word == null) {
                _uiState.update { it.copy(isLoading = false, isEditMode = true, notFound = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEditMode = true,
                        word = word.word,
                        definition = word.definition,
                        phonetic = word.phonetic.orEmpty(),
                        example = word.example.orEmpty()
                    )
                }
            }
        }
    }

    fun onWordChange(value: String) {
        _uiState.update { it.copy(word = value, wordError = null) }
    }

    fun onDefinitionChange(value: String) {
        _uiState.update { it.copy(definition = value, definitionError = null) }
    }

    fun onPhoneticChange(value: String) {
        _uiState.update { it.copy(phonetic = value) }
    }

    fun onExampleChange(value: String) {
        _uiState.update { it.copy(example = value) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.saved) return

        // 先在前端给一次即时反馈，避免空内容也走一趟数据库
        val localError = when {
            state.word.isBlank() -> FieldError.REQUIRED
            state.definition.isBlank() -> FieldError.REQUIRED
            else -> null
        }
        if (localError != null) {
            _uiState.update {
                it.copy(
                    wordError = if (state.word.isBlank()) FieldError.REQUIRED else null,
                    definitionError = if (state.definition.isBlank()) FieldError.REQUIRED else null
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val draft = WordDraft(
                word = state.word,
                definition = state.definition,
                phonetic = state.phonetic,
                example = state.example
            )
            val today = DateProvider.todayEpochDay(clock)
            val result = if (wordId == NEW_WORD_ID) {
                repository.addWord(draft, today)
            } else {
                repository.updateWord(wordId, draft, today)
            }
            applyResult(result)
        }
    }

    private fun applyResult(result: SaveResult) {
        when (result) {
            is SaveResult.Success -> _uiState.update {
                it.copy(isSaving = false, saved = true)
            }

            SaveResult.DuplicateWord -> _uiState.update {
                it.copy(isSaving = false, wordError = FieldError.DUPLICATE)
            }

            is SaveResult.Invalid -> _uiState.update {
                it.copy(
                    isSaving = false,
                    wordError = if (result.field == DraftField.WORD) FieldError.REQUIRED else null,
                    definitionError = if (result.field == DraftField.DEFINITION) FieldError.REQUIRED else null
                )
            }

            SaveResult.NotFound -> _uiState.update {
                it.copy(isSaving = false, notFound = true)
            }
        }
    }

    fun onSavedHandled() {
        _uiState.update { it.copy(saved = false) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        const val ARG_WORD_ID = "wordId"
        const val NEW_WORD_ID = -1L
    }
}
