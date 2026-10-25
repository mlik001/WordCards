package com.example.wordcards.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.domain.model.ReviewResult
import com.example.wordcards.ui.AppViewModelProvider
import com.example.wordcards.ui.components.ConfirmDialog
import com.example.wordcards.ui.components.EmptyState
import com.example.wordcards.ui.theme.WordCardsTheme

/**
 * 复习页（有状态）。
 *
 * 四个阶段一个 when 到底，不在 UI 里判断「是不是既空又加载」——
 * 那种判断迟早会漏掉一个组合。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onExit: () -> Unit,
    viewModel: StudyViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }

    // 只跑一次；startSession 内部也做了幂等保护，防重组重复调用
    LaunchedEffect(Unit) { viewModel.startSession() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.study_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (state.phase == StudyPhase.STUDYING && state.answeredCount > 0) {
                                showExitConfirm = true
                            } else {
                                onExit()
                            }
                        }
                    ) { Text(stringResource(R.string.study_exit)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when (state.phase) {
                StudyPhase.LOADING -> BoxedLoading()

                StudyPhase.EMPTY -> EmptyState(
                    title = stringResource(R.string.study_empty_title),
                    description = stringResource(R.string.study_empty_desc),
                    action = {
                        OutlinedButton(onClick = onExit) {
                            Text(stringResource(R.string.study_finished_back))
                        }
                    }
                )

                StudyPhase.STUDYING -> StudyInProgress(
                    state = state,
                    onFlip = viewModel::onFlip,
                    onAnswer = viewModel::onAnswer
                )

                StudyPhase.FINISHED -> StudyFinished(
                    state = state,
                    onRestart = viewModel::restartSession,
                    onExit = onExit
                )
            }
        }
    }

    if (showExitConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.study_exit_confirm_title),
            message = stringResource(R.string.study_exit_confirm_message),
            confirmText = stringResource(R.string.study_exit),
            onConfirm = {
                showExitConfirm = false
                onExit()
            },
            onDismiss = { showExitConfirm = false }
        )
    }
}

@Composable
private fun BoxedLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.study_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StudyInProgress(
    state: StudyUiState,
    onFlip: () -> Unit,
    onAnswer: (ReviewResult) -> Unit
) {
    val card = state.card ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.study_progress, state.answeredCount, state.totalCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.study_remaining, state.remaining),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(8.dp))

    LinearProgressIndicator(
        progress = { state.progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        trackColor = MaterialTheme.colorScheme.primaryContainer
    )

    Spacer(Modifier.height(20.dp))

    FlashCard(
        card = card,
        isFlipped = state.isFlipped,
        onFlip = onFlip,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { onAnswer(ReviewResult.UNKNOWN) },
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            enabled = state.isFlipped && !state.isSubmitting,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.study_unknown))
        }

        Button(
            onClick = { onAnswer(ReviewResult.KNOWN) },
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            enabled = state.isFlipped && !state.isSubmitting,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.study_known))
        }
    }
}

@Composable
private fun StudyFinished(
    state: StudyUiState,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.study_finished_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.study_finished_summary, state.knownCount, state.unknownCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.requeuedCount > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.study_finished_requeued, state.requeuedCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.study_restart))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.study_finished_back))
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun StudyInProgressPreview() {
    WordCardsTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            StudyInProgress(
                state = StudyUiState(
                    phase = StudyPhase.STUDYING,
                    card = StudyCardUi(1, "abandon", "/əˈbændən/", "v. 放弃；抛弃", "He abandoned the plan.", 2),
                    totalCount = 20,
                    remaining = 17,
                    answeredCount = 3
                ),
                onFlip = {},
                onAnswer = {}
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun StudyFinishedPreview() {
    WordCardsTheme {
        StudyFinished(
            state = StudyUiState(
                phase = StudyPhase.FINISHED,
                totalCount = 20,
                answeredCount = 20,
                knownCount = 16,
                unknownCount = 4,
                requeuedCount = 3
            ),
            onRestart = {},
            onExit = {}
        )
    }
}
