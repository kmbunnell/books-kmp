package com.example.books_kmp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.books_kmp.ui.TestTags

/**
 * Tier 2 — Inline error presentation.
 *
 * Renders a short, field-adjacent validation message in the theme's error color.
 * Use directly below the affected form field. See `docs/error-presentation.md`.
 */
@Composable
fun InlineErrorText(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.testTag(TestTags.ErrorPresentation.InlineErrorText),
    )
}
