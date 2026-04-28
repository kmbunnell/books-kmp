package com.example.books_kmp.ui.bookdetail

import android.app.Application
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.BookDetailError
import com.example.books_kmp.viewmodel.BookDetailIntent
import com.example.books_kmp.viewmodel.BookDetailUiState
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
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.tagChip("t1")).performClick()
        assertEquals(BookDetailIntent.ToggleTag("t1"), dispatched.last())
    }

    @Test
    fun `snackbar visible when tagToggleError non-null`() {
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(tagToggleError = BookDetailError.ToggleFailed),
                onIntent = {},
                onNavigateUp = {},
                onNavigateToTagManagement = {},
            )
        }
        composeTestRule.onNodeWithText("Something went wrong. Please try again.").assertIsDisplayed()
    }

    @Test
    fun `DismissTagToggleError dispatched after snackbar shown`() {
        val dispatched = mutableListOf<BookDetailIntent>()
        composeTestRule.setContent {
            BookDetailScreenContent(
                uiState = BookDetailUiState(tagToggleError = BookDetailError.ToggleFailed),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
                onNavigateToTagManagement = {},
            )
        }
        composeTestRule.mainClock.advanceTimeBy(5_000)
        composeTestRule.waitForIdle()
        assertTrue(dispatched.contains(BookDetailIntent.DismissTagToggleError))
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
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookDetail.RetryButton).performClick()
        assertTrue(dispatched.contains(BookDetailIntent.Reload))
    }
}
