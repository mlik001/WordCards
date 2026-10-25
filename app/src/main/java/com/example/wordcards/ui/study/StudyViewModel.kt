package com.example.wordcards.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordcards.data.local.entity.Word
import com.example.wordcards.data.prefs.UserPrefsRepository
import com.example.wordcards.data.repository.WordRepository
import com.example.wordcards.domain.model.ReviewResult
import com.example.wordcards.util.DateProvider
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 复习会话。
 *
 * 三个关键设计：
 * 1. **快照队列**：会话开始一次性把到期词读进内存队列，答题过程中**不再查库**。
 *    否则每答一张，`nextReview` 变化会让到期集合重排，用户会看到当前卡片"跳变"。
 * 2. **答错的卡回到队尾**：DB 侧已经写成「今天到期」，所以即使中途退出也不会丢；
 *    内存里再插回队尾，是为了让本轮体验连贯。每张卡每轮最多重现 2 次，防止死循环。
 * 3. **today 冻结**：整轮用同一个 today，跨零点的极端场景不会把队列算乱。
 */
class StudyViewModel(
    private val repository: WordRepository,
    private val prefsRepository: UserPrefsRepository,
    clock: Clock
) : ViewModel() {

    private val today: Long = DateProvider.todayEpochDay(clock)

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var sessionStarted = false
    private val queue = ArrayDeque<Word>()
    private val answeredIds = mutableSetOf<Long>()
    private val requeueCount = mutableMapOf<Long, Int>()
    private var working: Word? = null

    /** 幂等：Compose 重组或进程恢复可能重复调用，不能每次都重建会话。 */
    fun startSession() {
        if (sessionStarted) return
        sessionStarted = true
        _uiState.update { it.copy(phase = StudyPhase.LOADING) }

        viewModelScope.launch {
            val prefs = prefsRepository.prefs.first()
            val words = repository.loadStudyQueue(
                today = today,
                sessionSize = prefs.sessionSize,
                dailyNewWordLimit = prefs.dailyNewWordLimit
            )

            queue.clear()
            queue.addAll(words)
            working = queue.removeFirstOrNull()

            _uiState.update {
                it.copy(
                    phase = if (words.isEmpty()) StudyPhase.EMPTY else StudyPhase.STUDYING,
                    card = working?.toCard(),
                    isFlipped = false,
                    totalCount = words.size,
                    remaining = queue.size + if (working != null) 1 else 0
                )
            }
        }
    }

    fun onFlip() {
        val state = _uiState.value
        if (state.phase != StudyPhase.STUDYING || state.isSubmitting) return
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun onAnswer(result: ReviewResult) {
        val state = _uiState.value
        val current = working ?: return
        // 必须先翻面再作答：否则用户可能在没看到释义的情况下乱点
        if (state.phase != StudyPhase.STUDYING || !state.isFlipped || state.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val updated = runCatching {
                repository.submitReview(current.id, result, today)
            }.getOrNull()

            answeredIds.add(current.id)

            val requeued = if (result == ReviewResult.UNKNOWN && updated != null) {
                val times = requeueCount[current.id] ?: 0
                if (times < MAX_REQUEUE_PER_WORD) {
                    requeueCount[current.id] = times + 1
                    queue.addLast(updated)
                    true
                } else {
                    false
                }
            } else {
                false
            }

            working = queue.removeFirstOrNull()

            _uiState.update { s ->
                s.copy(
                    phase = if (working == null) StudyPhase.FINISHED else StudyPhase.STUDYING,
                    card = working?.toCard(),
                    isFlipped = false,
                    isSubmitting = false,
                    answeredCount = answeredIds.size,
                    knownCount = s.knownCount + if (result == ReviewResult.KNOWN) 1 else 0,
                    unknownCount = s.unknownCount + if (result == ReviewResult.UNKNOWN) 1 else 0,
                    requeuedCount = s.requeuedCount + if (requeued) 1 else 0,
                    remaining = queue.size + if (working != null) 1 else 0
                )
            }
        }
    }

    /** 完成页的「再来一轮」：重置会话状态后重新取队列（此时刚答完的词已经排到明天了）。 */
    fun restartSession() {
        sessionStarted = false
        queue.clear()
        answeredIds.clear()
        requeueCount.clear()
        working = null
        _uiState.value = StudyUiState()
        startSession()
    }

    private fun Word.toCard() = StudyCardUi(
        id = id,
        word = word,
        phonetic = phonetic,
        definition = definition,
        example = example,
        box = box
    )

    companion object {
        /** 同一张卡在本轮最多重现次数。不设上限的话，全错的用户会陷入死循环。 */
        const val MAX_REQUEUE_PER_WORD = 2
    }
}
