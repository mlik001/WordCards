package com.example.wordcards.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordcards.data.repository.WordRepository
import com.example.wordcards.domain.model.StudyStats
import com.example.wordcards.util.DateProvider
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 统计页只有一个只读数据源，因此直接暴露 `StateFlow<StudyStats>`，
 * 不再包一层 UiState——加壳只会多一层字段搬运。
 */
class StatsViewModel(
    repository: WordRepository,
    clock: Clock
) : ViewModel() {

    private val today = DateProvider.todayEpochDay(clock)

    val uiState: StateFlow<StudyStats> = repository.observeStats(today)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudyStats()
        )
}
