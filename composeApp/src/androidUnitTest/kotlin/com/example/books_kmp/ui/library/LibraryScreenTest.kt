package com.example.books_kmp.ui.library

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.LibraryIntent
import com.example.books_kmp.viewmodel.LibraryUiState
import com.example.books_kmp.viewmodel.SortOrder
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LibraryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tag1 = Tag(id = "t1", name = "Fiction", isDefault = false)
    private val tag2 = Tag(id = "t2", name = "Sci-Fi", isDefault = false)

    private val book1 =
        Book(id = "b1", isbn = null, title = "Dune", authors = listOf("Frank Herbert"), coverImageUrl = null)
    private val book2 =
        Book(id = "b2", isbn = null, title = "Foundation", authors = listOf("Isaac Asimov"), coverImageUrl = null)

    @Test
    fun `sign out button is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {}, onSignOut = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SignOutButton).assertIsDisplayed()
    }

    @Test
    fun `clicking sign out button triggers onSignOut callback`() {
        var signOutCalled = false
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {}, onSignOut = { signOutCalled = true })
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SignOutButton).performClick()
        assertTrue(signOutCalled)
    }

    @Test
    fun `sign out button triggers sign out callback`() {
        var signOutCalled = false
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(),
                onIntent = {},
                onSignOut = { signOutCalled = true },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SignOutButton).performClick()
        assertTrue(signOutCalled)
    }

    @Test
    fun `Add Book FAB is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {}, onSignOut = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddBookFab).assertIsDisplayed()
    }

    @Test
    fun `clicking Add Book FAB triggers onNavigateToAddBook callback`() {
        var navigateCalled = false
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(),
                onIntent = {},
                onSignOut = {},
                onNavigateToAddBook = { navigateCalled = true },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddBookFab).performClick()
        assertTrue(navigateCalled)
    }

    @Test
    fun `manage tags button is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {}, onSignOut = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ManageTagsButton).assertIsDisplayed()
    }

    @Test
    fun `clicking manage tags button triggers onNavigateToTagManagement callback`() {
        var navigateCalled = false
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(),
                onIntent = {},
                onSignOut = {},
                onNavigateToTagManagement = { navigateCalled = true },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ManageTagsButton).performClick()
        assertTrue(navigateCalled)
    }

    @Test
    fun `filter bar chips shown for each tag`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(tags = listOf(tag1, tag2)),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.filterChip("t1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.filterChip("t2")).assertIsDisplayed()
    }

    @Test
    fun `active filter chip is selected`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(tags = listOf(tag1), activeFilterTagIds = setOf("t1")),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.filterChip("t1")).assertIsSelected()
    }

    @Test
    fun `inactive filter chip is not selected`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(tags = listOf(tag2), activeFilterTagIds = emptySet()),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.filterChip("t2")).assertIsNotSelected()
    }

    @Test
    fun `tapping chip dispatches ToggleFilter with correct tag id`() {
        val dispatched = mutableListOf<LibraryIntent>()
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(tags = listOf(tag1)),
                onIntent = { dispatched.add(it) },
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.filterChip("t1")).performClick()
        assertEquals(LibraryIntent.ToggleFilter("t1"), dispatched.last())
    }

    // --- New tests for SHELVD-78 ---

    @Test
    fun `grid is displayed when filteredBooks is non-empty`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(filteredBooks = listOf(book1)),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.BookGrid).assertIsDisplayed()
    }

    @Test
    fun `grid shows one cell per book in filteredBooks`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(filteredBooks = listOf(book1, book2)),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b2")).assertIsDisplayed()
    }

    @Test
    fun `each cell shows the book title`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(filteredBooks = listOf(book1)),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1"))
            .assertTextContains("Dune", substring = true)
    }

    @Test
    fun `each cell shows the first author`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(filteredBooks = listOf(book1)),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1"))
            .assertTextContains("Frank Herbert", substring = true)
    }

    @Test
    fun `tapping a book cell calls onNavigateToBookDetail with correct book id`() {
        var navigatedId: String? = null
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(filteredBooks = listOf(book1)),
                onIntent = {},
                onSignOut = {},
                onNavigateToBookDetail = { navigatedId = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1")).performClick()
        assertEquals("b1", navigatedId)
    }

    @Test
    fun `search bar is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {}, onSignOut = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).assertIsDisplayed()
    }

    @Test
    fun `search bar displays current searchQuery from state`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(searchQuery = "Dune"),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).assertTextContains("Dune")
    }

    @Test
    fun `typing in search bar dispatches ChangeSearchQuery intent`() {
        val dispatched = mutableListOf<LibraryIntent>()
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(searchQuery = ""),
                onIntent = { dispatched.add(it) },
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).performTextInput("Du")
        assertTrue(dispatched.any { it is LibraryIntent.ChangeSearchQuery })
    }

    @Test
    fun `sort button is displayed in top bar`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {}, onSignOut = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SortButton).assertIsDisplayed()
    }

    @Test
    fun `clicking sort button opens dropdown menu`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {}, onSignOut = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SortButton).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.SortMenuAuthorAsc).assertIsDisplayed()
    }

    @Test
    fun `selecting Author sort dispatches ChangeSortOrder AUTHOR_ASC`() {
        val dispatched = mutableListOf<LibraryIntent>()
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(),
                onIntent = { dispatched.add(it) },
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SortButton).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.SortMenuAuthorAsc).performClick()
        assertEquals(LibraryIntent.ChangeSortOrder(SortOrder.AUTHOR_ASC), dispatched.last())
    }

    @Test
    fun `grid is empty when filteredBooks is empty`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(filteredBooks = emptyList()),
                onIntent = {},
                onSignOut = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.BookGrid).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1")).assertDoesNotExist()
    }
}
