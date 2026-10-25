package com.example.wordcards.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.wordcards.WordCardsApplication
import com.example.wordcards.di.AppContainer
import com.example.wordcards.ui.addedit.AddEditViewModel
import com.example.wordcards.ui.list.WordListViewModel
import com.example.wordcards.ui.settings.SettingsViewModel
import com.example.wordcards.ui.stats.StatsViewModel
import com.example.wordcards.ui.study.StudyViewModel
import com.example.wordcards.ui.wordbook.WordBookViewModel

/**
 * 全应用唯一的 ViewModel 工厂。
 *
 * 手写 DI 的收口点：所有 ViewModel 的构造依赖在这里显式列出，
 * 一眼就能看完「谁依赖什么」。迁 Hilt 时这一整个对象会被 @HiltViewModel 取代。
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {

        initializer {
            val container = container()
            WordListViewModel(
                repository = container.wordRepository,
                backupService = container.backupService,
                clock = container.clock
            )
        }

        initializer {
            val container = container()
            AddEditViewModel(
                repository = container.wordRepository,
                clock = container.clock,
                savedStateHandle = createSavedStateHandle()
            )
        }

        initializer {
            val container = container()
            StudyViewModel(
                repository = container.wordRepository,
                prefsRepository = container.prefsRepository,
                clock = container.clock
            )
        }

        initializer {
            val container = container()
            StatsViewModel(repository = container.wordRepository, clock = container.clock)
        }

        initializer {
            val container = container()
            SettingsViewModel(
                prefsRepository = container.prefsRepository,
                reminderController = container.reminderController
            )
        }

        initializer {
            WordBookViewModel(service = container().wordBookService)
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as WordCardsApplication).container
