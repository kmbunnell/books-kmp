package com.example.books_kmp.ui.manualentry

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.books_kmp.ui.TestTags
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManualEntryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `blank title on submit shows title validation error`() {
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(titleError = ManualEntryError.TitleRequired),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = {},
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithText("Title is required").assertIsDisplayed()
    }

    @Test
    fun `blank author on submit shows author validation error`() {
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(authorError = ManualEntryError.AuthorRequired),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = {},
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithText("Author is required").assertIsDisplayed()
    }

    @Test
    fun `valid inputs — save button triggers SaveBook intent`() {
        var capturedIntent: ManualEntryIntent? = null
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = { capturedIntent = it },
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.TitleField).performTextInput("The Odyssey")
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.AuthorField).performTextInput("Homer")
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.SaveButton).performClick()
        assertEquals(ManualEntryIntent.SaveBook("The Odyssey", "Homer"), capturedIntent)
    }

    @Test
    fun `cancel button triggers Cancel intent`() {
        var capturedIntent: ManualEntryIntent? = null
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = { capturedIntent = it },
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.CancelButton).performClick()
        assertEquals(ManualEntryIntent.Cancel, capturedIntent)
    }

    @Test
    fun `save button disabled when both fields are empty`() {
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = {},
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.SaveButton).assertIsNotEnabled()
    }

    @Test
    fun `save button disabled when author is empty`() {
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = {},
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.TitleField).performTextInput("The Odyssey")
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.SaveButton).assertIsNotEnabled()
    }

    @Test
    fun `save button enabled when both fields have text`() {
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = {},
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.TitleField).performTextInput("The Odyssey")
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.AuthorField).performTextInput("Homer")
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.SaveButton).assertIsEnabled()
    }

    @Test
    fun `loading state — save button disabled and loading indicator visible`() {
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(isLoading = true),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = {},
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.LoadingIndicator).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.SaveButton).assertIsNotEnabled()
    }

    @Test
    fun `showDuplicateDialog — duplicate dialog is displayed`() {
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(showDuplicateDialog = true),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = {},
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.DuplicateDialog).assertIsDisplayed()
    }

    @Test
    fun `duplicate dialog add anyway button triggers AddAnyway intent`() {
        var capturedIntent: ManualEntryIntent? = null
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(showDuplicateDialog = true),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = { capturedIntent = it },
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.DuplicateDialogAddAnywayButton).performClick()
        assertEquals(ManualEntryIntent.AddAnyway, capturedIntent)
    }

    @Test
    fun `duplicate dialog cancel button triggers DismissDuplicateDialog intent`() {
        var capturedIntent: ManualEntryIntent? = null
        composeTestRule.setContent {
            ManualEntryScreenContent(
                uiState = ManualEntryUiState(showDuplicateDialog = true),
                effects = MutableSharedFlow<ManualEntryEffect>(),
                onIntent = { capturedIntent = it },
                onNavigateToLibrary = {},
                onNavigateBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ManualEntry.DuplicateDialogCancelButton).performClick()
        assertEquals(ManualEntryIntent.DismissDuplicateDialog, capturedIntent)
    }
}
