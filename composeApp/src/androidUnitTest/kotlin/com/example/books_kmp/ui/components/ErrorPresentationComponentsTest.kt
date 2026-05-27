package com.example.books_kmp.ui.components

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.books_kmp.ui.TestTags
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ErrorPresentationComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Tier 1 — Snackbar

    @Test
    fun `snackbar renders message text when shown`() {
        val message = "Something went wrong"
        composeTestRule.setContent {
            val hostState = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                hostState.showSnackbar(message)
            }
            AppSnackbarHost(hostState = hostState)
        }
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.SnackbarHost).assertIsDisplayed()
    }

    // Tier 2 — Inline error

    @Test
    fun `InlineErrorText displays message`() {
        val message = "Email is required"
        composeTestRule.setContent {
            InlineErrorText(message = message)
        }
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.InlineErrorText).assertIsDisplayed()
    }

    // Tier 3 — Confirmation Dialog

    @Test
    fun `ConfirmationDialog displays title and message`() {
        val title = "Delete book?"
        val message = "This action cannot be undone."
        composeTestRule.setContent {
            ConfirmationDialog(
                title = title,
                message = message,
                confirmLabel = "Delete",
                dismissLabel = "Cancel",
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialog).assertIsDisplayed()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `ConfirmationDialog confirm button fires onConfirm callback`() {
        var confirmed = false
        composeTestRule.setContent {
            ConfirmationDialog(
                title = "Title",
                message = "Message",
                confirmLabel = "Delete",
                dismissLabel = "Cancel",
                onConfirm = { confirmed = true },
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogConfirmButton).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun `ConfirmationDialog dismiss button fires onDismiss callback`() {
        var dismissed = false
        composeTestRule.setContent {
            ConfirmationDialog(
                title = "Title",
                message = "Message",
                confirmLabel = "Delete",
                dismissLabel = "Cancel",
                onConfirm = {},
                onDismiss = { dismissed = true },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogDismissButton).performClick()
        assertTrue(dismissed)
    }
}
