package com.example.books_kmp.ui.addbook

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.ui.TestTags
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AddBookScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val emptyEffects = MutableSharedFlow<AddBookEffect>()

    @Test
    fun `navigate up button is displayed`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.NavigateUpButton).assertIsDisplayed()
    }

    @Test
    fun `tapping navigate up button triggers onNavigateUp`() {
        var navigatedUp = false
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = { navigatedUp = true },
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.NavigateUpButton).performClick()
        assertTrue(navigatedUp)
    }

    @Test
    fun `lookup mode toggle is displayed`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookupModeToggle).assertIsDisplayed()
    }

    @Test
    fun `ISBN field is displayed in ISBN mode`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.IsbnField).assertIsDisplayed()
    }

    @Test
    fun `title field is displayed in title mode`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(lookupMode = LookupMode.Title),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.TitleField).assertIsDisplayed()
    }

    @Test
    fun `ISBN field is not present in title mode`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(lookupMode = LookupMode.Title),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.IsbnField).assertDoesNotExist()
    }

    @Test
    fun `tapping title mode button dispatches SetLookupMode Title intent`() {
        var capturedIntent: AddBookIntent? = null
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.TitleModeButton).performClick()
        assertEquals(AddBookIntent.SetLookupMode(LookupMode.Title), capturedIntent)
    }

    @Test
    fun `title results are displayed when titleResults is non-empty`() {
        val book = BookLookupData(null, "The Iliad", listOf("Homer"), null)
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(lookupMode = LookupMode.Title, titleResults = listOf(book)),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.titleResultItem(0)).assertIsDisplayed()
    }

    @Test
    fun `tapping title result dispatches SelectTitleResult intent`() {
        var capturedIntent: AddBookIntent? = null
        val book = BookLookupData(null, "The Iliad", listOf("Homer"), null)
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(lookupMode = LookupMode.Title, titleResults = listOf(book)),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.titleResultItem(0)).performClick()
        assertEquals(AddBookIntent.SelectTitleResult(book), capturedIntent)
    }

    @Test
    fun `Look Up button is displayed`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).assertIsDisplayed()
    }

    @Test
    fun `Look Up button is disabled when ISBN is empty`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).assertIsNotEnabled()
    }

    @Test
    fun `Look Up button is enabled when ISBN is non-empty`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(isbn = "9780140449136"),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).assertIsEnabled()
    }

    @Test
    fun `Look Up button is disabled when title query is empty in title mode`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(lookupMode = LookupMode.Title, titleQuery = ""),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).assertIsNotEnabled()
    }

    @Test
    fun `Look Up button is enabled when title query is non-empty in title mode`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(lookupMode = LookupMode.Title, titleQuery = "Iliad"),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).assertIsEnabled()
    }

    @Test
    fun `typing in ISBN field dispatches IsbnChanged intent`() {
        val intents = mutableListOf<AddBookIntent>()
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = { intents += it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.IsbnField).performClick()
        composeTestRule.onNodeWithTag(TestTags.AddBook.IsbnField)
            .performTextInput("9780140449136")
        assertTrue(intents.any { it is AddBookIntent.IsbnChanged })
    }

    @Test
    fun `ISBN field does not dispatch IsbnChanged when input exceeds 13 characters`() {
        val intents = mutableListOf<AddBookIntent>()
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(isbn = "9780140449136"),
                effects = emptyEffects,
                onIntent = { intents += it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.IsbnField).performTextInput("X")
        assertTrue(intents.none { it is AddBookIntent.IsbnChanged })
    }

    @Test
    fun `tapping Look Up button dispatches LookupIsbn intent with the current isbn state`() {
        var capturedIntent: AddBookIntent? = null
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(isbn = "9780140449136"),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).performClick()
        assertEquals(AddBookIntent.LookupIsbn("9780140449136"), capturedIntent)
    }

    @Test
    fun `tapping Look Up button in title mode dispatches LookupByTitle intent`() {
        var capturedIntent: AddBookIntent? = null
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(lookupMode = LookupMode.Title, titleQuery = "The Iliad"),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).performClick()
        assertEquals(AddBookIntent.LookupByTitle("The Iliad"), capturedIntent)
    }

    @Test
    fun `loading indicator is visible when isLoading is true`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(isLoading = true),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun `Look Up button is disabled when isLoading is true`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(isbn = "9780140449136", isLoading = true),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.LookUpButton).assertIsNotEnabled()
    }

    @Test
    fun `book preview section is not visible in initial state`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.BookPreviewTitle).assertDoesNotExist()
    }

    @Test
    fun `book preview shows title and authors when foundBook is set`() {
        val book = BookLookupData("isbn", "The Iliad", listOf("Homer"), null)
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(foundBook = book),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.BookPreviewTitle).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.AddBook.BookPreviewAuthors).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Add button is displayed when foundBook is set`() {
        val book = BookLookupData("isbn", "The Iliad", listOf("Homer"), null)
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(foundBook = book),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.AddButton).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Cancel button is displayed when foundBook is set`() {
        val book = BookLookupData("isbn", "The Iliad", listOf("Homer"), null)
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(foundBook = book),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.CancelButton).assertIsDisplayed()
    }

    @Test
    fun `tapping Add dispatches ConfirmBook intent`() {
        var capturedIntent: AddBookIntent? = null
        val book = BookLookupData("isbn", "The Iliad", listOf("Homer"), null)
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(foundBook = book),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.AddButton).performScrollTo().performClick()
        assertEquals(AddBookIntent.ConfirmBook, capturedIntent)
    }

    @Test
    fun `tapping Cancel dispatches CancelBookPreview intent`() {
        var capturedIntent: AddBookIntent? = null
        val book = BookLookupData("isbn", "The Iliad", listOf("Homer"), null)
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(foundBook = book),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.CancelButton).performClick()
        assertEquals(AddBookIntent.CancelBookPreview, capturedIntent)
    }

    @Test
    fun `duplicate dialog is shown when showDuplicateDialog is true`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(showDuplicateDialog = true),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.DuplicateDialog).assertIsDisplayed()
    }

    @Test
    fun `Cancel button in duplicate dialog dispatches DismissDuplicateDialog intent`() {
        var capturedIntent: AddBookIntent? = null
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(showDuplicateDialog = true),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.DuplicateDialogCancelButton).performClick()
        assertEquals(AddBookIntent.DismissDuplicateDialog, capturedIntent)
    }

    @Test
    fun `duplicate dialog shows Add Anyway button`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(showDuplicateDialog = true),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.DuplicateDialogAddAnywayButton).assertIsDisplayed()
    }

    @Test
    fun `Add Anyway button dispatches AddAnyway intent`() {
        var capturedIntent: AddBookIntent? = null
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(showDuplicateDialog = true),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.DuplicateDialogAddAnywayButton).performClick()
        assertEquals(AddBookIntent.AddAnyway, capturedIntent)
    }

    @Test
    fun `network error banner is shown for NetworkError state`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.NetworkError),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.ErrorBanner).assertIsDisplayed()
    }

    @Test
    fun `retry button dispatches Retry intent`() {
        var capturedIntent: AddBookIntent? = null
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.NetworkError),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.RetryButton).performClick()
        assertEquals(AddBookIntent.Retry, capturedIntent)
    }

    @Test
    fun `not found error message is shown for NotFound error state`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.NotFound),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.ErrorBanner).assertIsDisplayed()
    }

    @Test
    fun `enter manually button is displayed for NotFound error state`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.NotFound),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.EnterManuallyButton).assertIsDisplayed()
    }

    @Test
    fun `tapping enter manually button dispatches EnterManually intent`() {
        var capturedIntent: AddBookIntent? = null
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.NotFound),
                effects = emptyEffects,
                onIntent = { capturedIntent = it },
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.EnterManuallyButton).performClick()
        assertEquals(AddBookIntent.EnterManually, capturedIntent)
    }

    @Test
    fun `NotFound error from title lookup shows error banner`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.NotFound),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.ErrorBanner).assertIsDisplayed()
    }

    @Test
    fun `NotFound error from title lookup shows enter manually button`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.NotFound),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.EnterManuallyButton).assertIsDisplayed()
    }

    @Test
    fun `rate limited banner is shown for RateLimited state`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.RateLimited),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithText("Too many requests. Please try again later.").assertIsDisplayed()
    }

    @Test
    fun `ScanUnknownError shows error banner and retry button`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.ScanUnknownError),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.ErrorBanner).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.AddBook.RetryButton).assertIsDisplayed()
    }

    @Test
    fun `ScanCameraPermissionDenied shows error banner and no retry button`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.ScanCameraPermissionDenied),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.ErrorBanner).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.AddBook.RetryButton).assertDoesNotExist()
    }

    @Test
    fun `ScanHardwareUnavailable shows error banner and no retry button`() {
        composeTestRule.setContent {
            AddBookScreenContent(
                uiState = AddBookUiState(error = AddBookScreenError.ScanHardwareUnavailable),
                effects = emptyEffects,
                onIntent = {},
                onNavigateUp = {},
                onNavigateToManualEntry = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.AddBook.ErrorBanner).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.AddBook.RetryButton).assertDoesNotExist()
    }
}
