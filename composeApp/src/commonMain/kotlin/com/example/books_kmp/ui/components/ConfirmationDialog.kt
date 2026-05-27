package com.example.books_kmp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.books_kmp.ui.TestTags

/**
 * Tier 3 — Destructive / consequential confirmation dialog.
 *
 * Generic reusable wrapper around Material 3 `AlertDialog`. Use for any action that
 * is destructive, irreversible, or otherwise warrants explicit confirmation.
 * See `docs/error-presentation.md`.
 *
 * @param title Short title (e.g. "Delete book?").
 * @param message Body explaining the consequence.
 * @param confirmLabel Label for the primary (destructive) action button.
 * @param dismissLabel Label for the cancel button.
 * @param onConfirm Invoked when the user taps the confirm button.
 * @param onDismiss Invoked for both the cancel button and `onDismissRequest`
 * (back press / scrim tap).
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.ErrorPresentation.ConfirmationDialogConfirmButton),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.ErrorPresentation.ConfirmationDialogDismissButton),
            ) {
                Text(dismissLabel)
            }
        },
        modifier = modifier.testTag(TestTags.ErrorPresentation.ConfirmationDialog),
    )
}
