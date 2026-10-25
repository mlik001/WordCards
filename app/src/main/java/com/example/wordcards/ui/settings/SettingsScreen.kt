package com.example.wordcards.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.data.prefs.UserPrefs
import com.example.wordcards.ui.AppViewModelProvider
import com.example.wordcards.ui.components.asText
import com.example.wordcards.ui.theme.WordCardsTheme
import kotlin.math.roundToInt

/**
 * 设置页。
 *
 * 两个实现细节：
 * 1. Slider 用「本地值 + onValueChangeFinished 提交」而不是 onValueChange 直接写库，
 *    否则拖动一次会触发几十次 DataStore 写入。
 * 2. 通知权限必须由 Activity 发起，所以 VM 只给出 `awaitingPermission` 标记，
 *    实际申请在 Composable 里通过 ActivityResult API 完成。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onPermissionResult(granted) }

    LaunchedEffect(state.awaitingPermission) {
        if (state.awaitingPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val messageText = state.message?.asText()
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        SettingsContent(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onSessionSizeChange = viewModel::onSessionSizeChange,
            onDailyNewWordLimitChange = viewModel::onDailyNewWordLimitChange,
            onReminderToggle = viewModel::onReminderToggle,
            onReminderTimeChange = viewModel::onReminderTimeChange
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onSessionSizeChange: (Int) -> Unit = { _ -> },
    onDailyNewWordLimitChange: (Int) -> Unit = { _ -> },
    onReminderToggle: (Boolean) -> Unit = { _ -> },
    onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> }
) {
    val prefs = state.prefs

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsCard(title = stringResource(R.string.settings_session_size)) {
            IntSlider(
                value = prefs.sessionSize,
                valueRange = 5..50,
                step = 5,
                label = stringResource(R.string.settings_session_size_value, prefs.sessionSize),
                onValueChangeFinished = onSessionSizeChange
            )
        }

        SettingsCard(title = stringResource(R.string.settings_daily_new)) {
            IntSlider(
                value = prefs.dailyNewWordLimit,
                valueRange = 0..30,
                step = 5,
                label = stringResource(R.string.settings_daily_new_value, prefs.dailyNewWordLimit),
                onValueChangeFinished = onDailyNewWordLimitChange
            )
            Text(
                text = stringResource(R.string.settings_daily_new_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsCard(title = stringResource(R.string.settings_reminder)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_reminder_switch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = prefs.reminderEnabled,
                    onCheckedChange = onReminderToggle
                )
            }

            if (prefs.reminderEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_reminder_time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTime(prefs.reminderHour, prefs.reminderMinute),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                TimePickerRow(
                    hour = prefs.reminderHour,
                    minute = prefs.reminderMinute,
                    onChange = onReminderTimeChange
                )

                Text(
                    text = stringResource(R.string.settings_reminder_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun IntSlider(
    value: Int,
    valueRange: IntRange,
    step: Int,
    label: String,
    onValueChangeFinished: (Int) -> Unit
) {
    var local by remember(value) { mutableIntStateOf(value) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = local.toFloat(),
            onValueChange = { raw ->
                val snapped = ((raw / step).roundToInt() * step)
                    .coerceIn(valueRange.first, valueRange.last)
                local = snapped
            },
            onValueChangeFinished = { onValueChangeFinished(local) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first) / step - 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun TimePickerRow(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    var localHour by remember(hour) { mutableIntStateOf(hour) }

    Column {
        Slider(
            value = localHour.toFloat(),
            onValueChange = { localHour = it.roundToInt().coerceIn(0, 23) },
            onValueChangeFinished = { onChange(localHour, minute) },
            valueRange = 0f..23f,
            steps = 22,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 30).forEach { candidate ->
                FilterChip(
                    selected = minute == candidate,
                    onClick = { onChange(localHour, candidate) },
                    label = { Text(":%02d".format(candidate)) }
                )
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun SettingsPreview() {
    WordCardsTheme {
        SettingsContent(
            state = SettingsUiState(
                isLoading = false,
                prefs = UserPrefs(
                    sessionSize = 20,
                    dailyNewWordLimit = 10,
                    reminderEnabled = true,
                    reminderHour = 8,
                    reminderMinute = 30
                )
            )
        )
    }
}
