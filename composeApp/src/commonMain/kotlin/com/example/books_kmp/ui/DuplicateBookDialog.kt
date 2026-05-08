package com.example.books_kmp.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_add_anyway
import bookskmp.composeapp.generated.resources.button_cancel
import bookskmp.composeapp.generated.resources.error_duplicate_message
import bookskmp.composeapp.generated.resources.error_duplicate_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun DuplicateBookDialog(
    onAddAnyway: () -> Unit,
    onDismiss: () -> Unit,
    dialogTag: String,
    addAnywayTag: String,
    cancelTag: String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.error_duplicate_title)) },
        text = { Text(stringResource(Res.string.error_duplicate_message)) },
        confirmButton = {
            Button(
                onClick = onAddAnyway,
                modifier = Modifier.testTag(addAnywayTag),
            ) {
                Text(stringResource(Res.string.button_add_anyway))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(cancelTag),
            ) {
                Text(stringResource(Res.string.button_cancel))
            }
        },
        modifier = Modifier.testTag(dialogTag),
    )
}
