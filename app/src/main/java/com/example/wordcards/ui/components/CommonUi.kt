package com.example.wordcards.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordcards.R
import com.example.wordcards.ui.UiMessage

/**
 * 空态。列表页、搜索无结果、复习队列为空都复用这一个，
 * 文案由调用方给，避免三个页面各写一套版式。
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Column(modifier = Modifier.padding(top = 8.dp)) { action() }
        }
    }
}

/** 删除等不可逆操作的二次确认。 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.action_delete),
    dismissText: String = stringResource(R.string.action_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    )
}

/** UiMessage → 本地化文案。ViewModel 只描述事件，翻译在 UI 层完成。 */
@Composable
fun UiMessage.asText(): String = when (this) {
    is UiMessage.WordDeleted -> stringResource(R.string.msg_word_deleted, word)
    is UiMessage.Exported -> stringResource(R.string.msg_exported, count)
    is UiMessage.Imported -> stringResource(R.string.msg_imported, inserted, skipped)
    is UiMessage.BookImported ->
        stringResource(R.string.msg_book_imported, bookName, inserted, skipped)
    is UiMessage.Failed -> stringResource(
        R.string.msg_failed,
        reason ?: stringResource(R.string.msg_unknown_error)
    )
    UiMessage.PermissionDenied -> stringResource(R.string.settings_permission_denied)
}
