package com.example.wordcards.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.ui.AppViewModelProvider
import com.example.wordcards.ui.components.EmptyState
import com.example.wordcards.ui.theme.WordCardsTheme

/**
 * 新增 / 编辑页（有状态）。
 *
 * 「保存成功」用 `state.saved` 这个一次性标记驱动导航，而不是在点击回调里直接
 * `popBackStack()`：后者在配置变更（旋转）时可能丢失，而状态不会。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    onDone: () -> Unit,
    viewModel: AddEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onSavedHandled()
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEditMode) R.string.edit_title_edit else R.string.edit_title_new
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.action_back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AddEditContent(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onWordChange = viewModel::onWordChange,
            onDefinitionChange = viewModel::onDefinitionChange,
            onPhoneticChange = viewModel::onPhoneticChange,
            onExampleChange = viewModel::onExampleChange,
            onSave = viewModel::onSave,
            onBack = onDone
        )
    }
}

@Composable
private fun AddEditContent(
    state: AddEditUiState,
    modifier: Modifier = Modifier,
    onWordChange: (String) -> Unit = { _ -> },
    onDefinitionChange: (String) -> Unit = { _ -> },
    onPhoneticChange: (String) -> Unit = { _ -> },
    onExampleChange: (String) -> Unit = { _ -> },
    onSave: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    if (state.notFound) {
        EmptyState(
            title = stringResource(R.string.edit_not_found),
            description = stringResource(R.string.list_search_empty_desc),
            modifier = modifier,
            action = {
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            }
        )
        return
    }

    val wordErrorText = when (state.wordError) {
        FieldError.REQUIRED -> stringResource(R.string.edit_error_required)
        FieldError.DUPLICATE -> stringResource(R.string.edit_error_duplicate)
        null -> null
    }
    val definitionErrorText = if (state.definitionError == FieldError.REQUIRED) {
        stringResource(R.string.edit_error_required)
    } else {
        null
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = state.word,
            onValueChange = onWordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_field_word)) },
            singleLine = true,
            isError = wordErrorText != null,
            supportingText = wordErrorText?.let { message -> { Text(message) } },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = state.definition,
            onValueChange = onDefinitionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_field_definition)) },
            minLines = 2,
            isError = definitionErrorText != null,
            supportingText = definitionErrorText?.let { message -> { Text(message) } },
            shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
            value = state.phonetic,
            onValueChange = onPhoneticChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_field_phonetic)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
            value = state.example,
            onValueChange = onExampleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_field_example)) },
            minLines = 2,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = state.canSubmit,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.action_save),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun AddEditContentPreview() {
    WordCardsTheme {
        AddEditContent(
            state = AddEditUiState(
                isLoading = false,
                isEditMode = true,
                word = "abandon",
                definition = "v. 放弃；抛弃",
                phonetic = "/əˈbændən/",
                example = "He abandoned the plan."
            )
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun AddEditErrorPreview() {
    WordCardsTheme {
        AddEditContent(
            state = AddEditUiState(
                isLoading = false,
                word = "abandon",
                wordError = FieldError.DUPLICATE
            )
        )
    }
}
