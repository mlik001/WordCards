package com.example.wordcards.ui.wordbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.data.wordbook.WordBookPack
import com.example.wordcards.data.wordbook.WordBookSummary
import com.example.wordcards.domain.model.WordDraft
import com.example.wordcards.ui.AppViewModelProvider
import com.example.wordcards.ui.components.asText
import com.example.wordcards.ui.theme.WordCardsTheme

/**
 * 词书页：列出内置词书，一键导入。
 *
 * 不做「选择部分单词导入」：几十个复选框对背单词这件事收益很低，
 * 用户真实需求是「给我一本能背的书」，导入后再自己删不想要的词更顺手。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordBookScreen(
    onBack: () -> Unit,
    viewModel: WordBookViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text(stringResource(R.string.wordbook_title), fontWeight = FontWeight.SemiBold) },
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
        WordBookContent(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onImport = viewModel::onImport
        )
    }
}

@Composable
private fun WordBookContent(
    state: WordBookUiState,
    modifier: Modifier = Modifier,
    onImport: (String) -> Unit = { _ -> }
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.wordbook_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(items = state.books, key = { it.pack.id }) { book ->
            WordBookCard(
                book = book,
                isImporting = state.importingId == book.pack.id,
                anyImporting = state.importingId != null,
                onImport = { onImport(book.pack.id) }
            )
        }

        if (!state.isLoading && state.books.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.wordbook_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.wordbook_online_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.wordbook_online_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WordBookCard(
    book: WordBookSummary,
    isImporting: Boolean,
    anyImporting: Boolean,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = book.pack.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = book.pack.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.wordbook_progress,
                        book.totalCount,
                        book.importedCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))

                when {
                    isImporting -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp).height(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.wordbook_action_importing),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    book.isFullyImported -> Text(
                        text = stringResource(R.string.wordbook_action_done),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> Button(
                        onClick = onImport,
                        enabled = !anyImporting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.wordbook_action_import,
                                book.totalCount - book.importedCount
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun WordBookPreview() {
    WordCardsTheme {
        WordBookContent(
            state = WordBookUiState(
                isLoading = false,
                books = listOf(
                    WordBookSummary(
                        pack = WordBookPack(
                            id = "cet6_high_freq",
                            name = "六级高频词",
                            description = "六级阅读与写作中最常出现的 384 词",
                            version = 1,
                            words = List(384) { WordDraft("word$it", "释义") }
                        ),
                        importedCount = 120
                    ),
                    WordBookSummary(
                        pack = WordBookPack(
                            id = "cet4_core",
                            name = "四级核心词",
                            description = "四级大纲高频 400 词",
                            version = 1,
                            words = List(400) { WordDraft("word$it", "释义") }
                        ),
                        importedCount = 400
                    )
                )
            )
        )
    }
}
