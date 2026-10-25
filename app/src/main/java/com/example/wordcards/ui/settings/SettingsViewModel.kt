package com.example.wordcards.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordcards.data.prefs.UserPrefs
import com.example.wordcards.data.prefs.UserPrefsRepository
import com.example.wordcards.data.reminder.ReminderController
import com.example.wordcards.ui.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val prefs: UserPrefs = UserPrefs(),
    val isLoading: Boolean = true,
    /** true 表示 UI 应该立刻发起通知权限申请 */
    val awaitingPermission: Boolean = false,
    val message: UiMessage? = null
)

/**
 * 设置页。
 *
 * 提醒开关不是「改一个布尔值」那么简单：Android 13+ 必须先拿到通知权限，
 * 因此这里维护了一个 `awaitingPermission` 中间态，由 UI 去弹系统对话框，
 * 回来再调 [onPermissionResult]。把权限请求留在 UI 层是因为它必须由 Activity 发起。
 */
class SettingsViewModel(
    private val prefsRepository: UserPrefsRepository,
    private val reminderController: ReminderController
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsRepository.prefs,
        local
    ) { prefs, localState ->
        SettingsUiState(
            prefs = prefs,
            isLoading = false,
            awaitingPermission = localState.awaitingPermission,
            message = localState.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onSessionSizeChange(size: Int) {
        viewModelScope.launch { prefsRepository.setSessionSize(size) }
    }

    fun onDailyNewWordLimitChange(limit: Int) {
        viewModelScope.launch { prefsRepository.setDailyNewWordLimit(limit) }
    }

    fun onReminderToggle(enabled: Boolean) {
        val prefs = uiState.value.prefs
        if (enabled && !reminderController.hasNotificationPermission()) {
            local.update { it.copy(awaitingPermission = true) }
            return
        }
        applyReminder(enabled, prefs.reminderHour, prefs.reminderMinute)
    }

    fun onPermissionResult(granted: Boolean) {
        val prefs = uiState.value.prefs
        local.update { it.copy(awaitingPermission = false) }
        if (granted) {
            applyReminder(enabled = true, hour = prefs.reminderHour, minute = prefs.reminderMinute)
        } else {
            viewModelScope.launch {
                prefsRepository.setReminder(false, prefs.reminderHour, prefs.reminderMinute)
            }
            local.update { it.copy(message = UiMessage.PermissionDenied) }
        }
    }

    fun onReminderTimeChange(hour: Int, minute: Int) {
        val prefs = uiState.value.prefs
        applyReminder(prefs.reminderEnabled, hour, minute)
    }

    fun onMessageShown() {
        local.update { it.copy(message = null) }
    }

    private fun applyReminder(enabled: Boolean, hour: Int, minute: Int) {
        reminderController.apply(enabled, hour, minute)
        viewModelScope.launch { prefsRepository.setReminder(enabled, hour, minute) }
    }

    private data class LocalState(
        val awaitingPermission: Boolean = false,
        val message: UiMessage? = null
    )
}
