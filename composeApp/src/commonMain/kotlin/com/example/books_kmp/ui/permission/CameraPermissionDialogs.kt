package com.example.books_kmp.ui.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_not_now
import bookskmp.composeapp.generated.resources.button_open_settings
import bookskmp.composeapp.generated.resources.button_retry
import bookskmp.composeapp.generated.resources.camera_permission_permanently_denied
import bookskmp.composeapp.generated.resources.camera_permission_rationale
import bookskmp.composeapp.generated.resources.camera_permission_title
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource

@Composable
fun CameraRationaleDialog(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.semantics { testTag = TestTags.CameraPermission.RationaleDialog },
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.camera_permission_title)) },
        text = { Text(stringResource(Res.string.camera_permission_rationale)) },
        confirmButton = {
            TextButton(
                modifier = Modifier.semantics { testTag = TestTags.CameraPermission.RetryButton },
                onClick = onRetry,
            ) {
                Text(stringResource(Res.string.button_retry))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.semantics { testTag = TestTags.CameraPermission.DismissButton },
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.button_not_now))
            }
        },
    )
}

@Composable
fun CameraSettingsDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.semantics { testTag = TestTags.CameraPermission.SettingsDialog },
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.camera_permission_title)) },
        text = { Text(stringResource(Res.string.camera_permission_permanently_denied)) },
        confirmButton = {
            TextButton(
                modifier = Modifier.semantics { testTag = TestTags.CameraPermission.OpenSettingsButton },
                onClick = onOpenSettings,
            ) {
                Text(stringResource(Res.string.button_open_settings))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.semantics { testTag = TestTags.CameraPermission.DismissButton },
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.button_not_now))
            }
        },
    )
}
