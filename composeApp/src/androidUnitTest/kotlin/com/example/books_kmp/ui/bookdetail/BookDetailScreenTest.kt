package com.example.books_kmp.ui.bookdetail

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.ui.TestTags
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BookDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tag1 = Tag(id = "t1", name = "Fiction", isDefault = false)
    private val tag2 = Tag(id = "t2", name = "Sci-Fi", isDefault = false)

    @Test
    fun `all tags shown as FilterChips`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(allTags = listOf(tag1, tag2)),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.tagChip("t1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BookDetail.tagChip("t2")).assertIsDisplayed()
    }

    @Test
    fun `applied tag chip is selected`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState =
                    BookDetailUiState(
                        allTags = listOf(tag1),
                        appliedTagIds = setOf("t1"),
                    ),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.tagChip("t1")).assertIsSelected()
    }

    @Test
    fun `unapplied tag chip is not selected`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState =
                    BookDetailUiState(
                        allTags = listOf(tag2),
                        appliedTagIds = setOf("t1"),
                    ),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.tagChip("t2")).assertIsNotSelected()
    }

    @Test
    fun `in-flight chip is disabled`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState =
                    BookDetailUiState(
                        allTags = listOf(tag1),
                        inFlightTagIds = setOf("t1"),
                    ),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.tagChip("t1")).assertIsNotEnabled()
    }

    @Test
    fun `tapping chip dispatches ToggleTag with correct id`() {
        val dispatched = mutableListOf<BookDetailIntent>()
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(allTags = listOf(tag1)),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.tagChip("t1")).performClick()
        assertEquals(BookDetailIntent.ToggleTag("t1"), dispatched.last())
    }

    @Test
    fun `manage tags button click invokes navigation callback`() {
        var navigated = false
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = { navigated = true },
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.ManageTagsButton).performClick()
        assertTrue(navigated)
    }

    @Test
    fun `loading state shows spinner and hides content`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(isLoading = true),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.LoadingIndicator).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BookDetail.ManageTagsButton).assertDoesNotExist()
    }

    @Test
    fun `loadFailed state shows error message and retry button`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(loadFailed = true),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.LoadFailedMessage).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BookDetail.RetryButton).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BookDetail.ManageTagsButton).assertDoesNotExist()
    }

    @Test
    fun `retry button dispatches Reload intent`() {
        val dispatched = mutableListOf<BookDetailIntent>()
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(loadFailed = true),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.RetryButton).performClick()
        assertTrue(dispatched.contains(BookDetailIntent.Reload))
    }

    @Test
    fun `top app bar always shows Tag Book title`() {
        val book = Book(id = "b1", isbn = null, title = "Dune", authors = listOf("Frank Herbert"), coverImageUrl = null)
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(book = book),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithText("Tag Book").assertIsDisplayed()
    }

    @Test
    fun `cover image placeholder is shown in content state`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(allTags = listOf(tag1)),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.CoverImage).assertIsDisplayed()
    }

    @Test
    fun `tags section label shown in content state`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(allTags = listOf(tag1)),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.TagsSectionLabel).assertIsDisplayed()
    }

    @Test
    fun `tag section renders below cover image in content state`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(allTags = listOf(tag1)),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.CoverImage).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BookDetail.ManageTagsButton).assertIsDisplayed()
    }

    // --- Delete book ---

    @Test
    fun `delete button is shown in toolbar`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.DeleteButton).assertIsDisplayed()
    }

    @Test
    fun `tapping delete button dispatches DeleteBook intent`() {
        val dispatched = mutableListOf<BookDetailIntent>()
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.DeleteButton).performClick()
        assertTrue(dispatched.contains(BookDetailIntent.DeleteBook))
    }

    @Test
    fun `confirmation dialog shown when showDeleteConfirm is true`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(showDeleteConfirm = true),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogConfirmButton).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogDismissButton).assertIsDisplayed()
    }

    @Test
    fun `confirm delete button dispatches ConfirmDelete`() {
        val dispatched = mutableListOf<BookDetailIntent>()
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(showDeleteConfirm = true),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogConfirmButton).performClick()
        assertTrue(dispatched.contains(BookDetailIntent.ConfirmDelete))
    }

    @Test
    fun `cancel delete button dispatches DismissDelete`() {
        val dispatched = mutableListOf<BookDetailIntent>()
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(showDeleteConfirm = true),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
                onNavigateToTagManagement = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogDismissButton).performClick()
        assertTrue(dispatched.contains(BookDetailIntent.DismissDelete))
    }
}
