package com.example.books_kmp.ui.components

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.books_kmp.ui.TestTags

/**
 * Tier 1 — Transient error / status presentation.
 *
 * Standardised `SnackbarHost` wrapper for use in a Material 3 `Scaffold`. Pair with
 * a `SnackbarHostState` and call `hostState.showSnackbar(message)` from a
 * `LaunchedEffect` driven by a `SharedFlow<XxxEffect>`. See `docs/error-presentation.md`.
 *
 * Also use for operation-level success confirmations (e.g. "Book deleted"). Do not
 * introduce a separate success-toast pattern.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.testTag(TestTags.ErrorPresentation.SnackbarHost),
    )
}
