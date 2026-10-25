package com.example.wordcards.ui.list

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.ui.AppViewModelProvider
import com.example.wordcards.ui.components.ConfirmDialog
import com.example.wordcards.ui.components.EmptyState
import com.example.wordcards.ui.components.asText
import com.example.wordcards.ui.theme.BoxLearning
import com.example.wordcards.ui.theme.BoxMastered
import com.example.wordcards.ui.theme.BoxNew
import com.example.wordcards.ui.theme.WordCardsTheme

/**
 * 单词列表页（有状态）。
 *
 * 这里只做三件 UI 层才该做的事：发系统级请求（SAF 选文件、Snackbar）、
 * 收集状态、把用户动作翻译成 ViewModel 事件。判断与取数都在下层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    onAddWord: () -> Unit,
    onEditWord: (Long) -> Unit,
    onStartStudy: () -> Unit,
    onOpenWordBooks: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: WordListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF：不需要申请存储权限，用户自己选文件位置
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::onExport) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onImport) }

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
                title = { Text(stringResource(R.string.list_title), fontWeight = FontWeight.SemiBold) },
                actions = {
                    MoreMenu(
                        onOpenWordBooks = onOpenWordBooks,
                        onOpenStats = onOpenStats,
                        onOpenSettings = onOpenSettings,
                        onExport = { exportLauncher.launch("wordcards-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json")) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddWord,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.list_add_word), fontWeight = FontWeight.Medium)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        WordListContent(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onQueryChange = viewModel::onQueryChange,
            onStartStudy = onStartStudy,
            onEditWord = onEditWord,
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onDeleteDismiss = viewModel::onDeleteDismiss
        )
    }
}

/** 无状态版本：只吃状态 + 回调，因此可以脱离 ViewModel 做 @Preview。 */
@Composable
private fun WordListContent(
    state: WordListUiState,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = { _ -> },
    onStartStudy: () -> Unit = {},
    onEditWord: (Long) -> Unit = { _ -> },
    onDeleteClick: (WordListItem) -> Unit = { _ -> },
    onDeleteConfirm: () -> Unit = {},
    onDeleteDismiss: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StudyBanner(
                dueCount = state.dueCount,
                newWordCount = state.newWordCount,
                enabled = state.hasStudyWork,
                onStartStudy = onStartStudy
            )
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.list_search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (state.words.isNotEmpty()) {
            item {
                Text(
                    text = if (state.isSearching) {
                        stringResource(R.string.list_search_count, state.totalCount)
                    } else {
                        stringResource(R.string.list_header_count, state.totalCount)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            items(items = state.words, key = { it.id }) { item ->
                WordRow(
                    item = item,
                    onClick = { onEditWord(item.id) },
                    onDelete = { onDeleteClick(item) }
                )
            }
        }

        if (!state.isLoading && state.words.isEmpty()) {
            item {
                if (state.isSearching) {
                    EmptyState(
                        title = stringResource(R.string.list_search_empty_title),
                        description = stringResource(R.string.list_search_empty_desc)
                    )
                } else {
                    EmptyState(
                        title = stringResource(R.string.list_empty_title),
                        description = stringResource(R.string.list_empty_desc)
                    )
                }
            }
        }
    }

    state.pendingDelete?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.list_delete_title),
            message = stringResource(R.string.list_delete_message, target.word),
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss
        )
    }
}

@Composable
private fun StudyBanner(
    dueCount: Int,
    newWordCount: Int,
    enabled: Boolean,
    onStartStudy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled) {
                        stringResource(R.string.list_banner_due, dueCount) +
                            " · " + stringResource(R.string.list_banner_new, newWordCount)
                    } else {
                        stringResource(R.string.list_nothing_to_study)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (enabled) R.string.list_banner_hint else R.string.list_banner_hint_idle
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onStartStudy, enabled = enabled) {
                Text(stringResource(R.string.list_start_study))
            }
        }
    }
}

@Composable
private fun WordRow(
    item: WordListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        item.phonetic?.let { phonetic ->
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = phonetic,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    BoxBadge(item.box)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = dueLabel(item.daysUntilDue),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.daysUntilDue <= 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxBadge(box: Int) {
    val color = boxColor(box)
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = stringResource(R.string.study_box_label, box),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun dueLabel(daysUntilDue: Long): String = when {
    daysUntilDue < 0 -> stringResource(R.string.due_overdue, (-daysUntilDue).toInt())
    daysUntilDue == 0L -> stringResource(R.string.due_today)
    daysUntilDue == 1L -> stringResource(R.string.due_tomorrow)
    else -> stringResource(R.string.due_in_days, daysUntilDue.toInt())
}

private fun boxColor(box: Int): Color = when {
    box <= 0 -> BoxNew
    box >= 4 -> BoxMastered
    else -> BoxLearning
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenu(
    onOpenWordBooks: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.action_more), color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_menu_wordbooks)) },
                onClick = {
                    expanded = false
                    onOpenWordBooks()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_menu_stats)) },
                onClick = {
                    expanded = false
                    onOpenStats()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_menu_settings)) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_menu_export)) },
                onClick = {
                    expanded = false
                    onExport()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_menu_import)) },
                onClick = {
                    expanded = false
                    onImport()
                }
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun WordListContentPreview() {
    WordCardsTheme {
        WordListContent(
            state = WordListUiState(
                isLoading = false,
                dueCount = 4,
                newWordCount = 6,
                totalCount = 3,
                words = listOf(
                    WordListItem(1, "abandon", "/əˈbændən/", "v. 放弃；抛弃", 0, 0),
                    WordListItem(2, "benefit", "/ˈbenɪfɪt/", "n. 利益 v. 有益于", 3, 4),
                    WordListItem(3, "diligent", "/ˈdɪlɪdʒənt/", "adj. 勤奋的", 5, 12)
                )
            )
        )
    }
}
