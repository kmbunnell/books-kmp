package com.example.books_kmp.ui.library

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.testing.TEST_INSTANT
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
class LibraryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tag1 = Tag(id = "t1", name = "Fiction", isDefault = false, updatedAt = TEST_INSTANT)
    private val tag2 = Tag(id = "t2", name = "Sci-Fi", isDefault = false, updatedAt = TEST_INSTANT)

    private val book1 =
        Book(
            id = "b1",
            isbn = null,
            title = "Dune",
            authors = listOf("Frank Herbert"),
            coverImageUrl = null,
            updatedAt = TEST_INSTANT,
        )
    private val book2 =
        Book(
            id = "b2",
            isbn = null,
            title = "Foundation",
            authors = listOf("Isaac Asimov"),
            coverImageUrl = null,
            updatedAt = TEST_INSTANT,
        )

    @Test
    fun `profile button is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ProfileButton).assertIsDisplayed()
    }

    @Test
    fun `tapping profile button opens profile sheet`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ProfileButton).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.ProfileSheet).assertIsDisplayed()
    }

    @Test
    fun `clicking sign out in profile sheet dispatches SignOut intent`() {
        var capturedIntent: LibraryIntent? = null
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = { capturedIntent = it })
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ProfileButton).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.ProfileSheetSignOut).performClick()
        assertEquals(LibraryIntent.SignOut, capturedIntent)
    }

    @Test
    fun `Add Book FAB is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddBookFab).assertIsDisplayed()
    }

    @Test
    fun `clicking Add Book FAB dispatches NavigateToAddBook intent`() {
        var dispatchedIntent: LibraryIntent? = null
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(),
                onIntent = { dispatchedIntent = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddBookFab).performClick()
        assertEquals(LibraryIntent.NavigateToAddBook, dispatchedIntent)
    }

    @Test
    fun `manage tags button is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
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
                onNavigateToTagManagement = { navigateCalled = true },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ManageTagsButton).performClick()
        assertTrue(navigateCalled)
    }

    // --- Filter button tests ---

    @Test
    fun `filter button is shown in TopAppBar`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterButton).assertIsDisplayed()
    }

    @Test
    fun `no badge shown when activeFilterTagIds is empty`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(activeFilterTagIds = emptySet()),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterBadge).assertDoesNotExist()
    }

    @Test
    fun `badge shown when activeFilterTagIds is non-empty`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(activeFilterTagIds = setOf("t1")),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterBadge).assertIsDisplayed()
    }

    @Test
    fun `tapping filter button opens TagFilterBottomSheet`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterButton).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.FilterSheet).assertIsDisplayed()
    }

    // --- Error state tests ---

    @Test
    fun `when error is set error message is shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(error = LibraryError.LoadFailed),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.LibraryError).assertIsDisplayed()
    }

    @Test
    fun `when error is set retry button is shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(error = LibraryError.LoadFailed),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.RetryButton).assertIsDisplayed()
    }

    @Test
    fun `when error is set book grid is not visible`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(error = LibraryError.LoadFailed),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.BookGrid).assertDoesNotExist()
    }

    @Test
    fun `when error is set search bar is not visible`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(error = LibraryError.LoadFailed),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).assertDoesNotExist()
    }

    @Test
    fun `when error is set filter button is not visible`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(error = LibraryError.LoadFailed),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterButton).assertDoesNotExist()
    }

    @Test
    fun `tapping retry dispatches LibraryIntent Refresh`() {
        val dispatched = mutableListOf<LibraryIntent>()
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(error = LibraryError.LoadFailed),
                onIntent = { dispatched.add(it) },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.RetryButton).performClick()
        assertTrue(dispatched.contains(LibraryIntent.Refresh))
    }

    // --- Reload error banner tests (error set with data present) ---

    @Test
    fun `when error and books non-empty reload error banner is shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState(
                        error = LibraryError.LoadFailed,
                        books = listOf(book1),
                        filteredBooks = listOf(book1),
                    ),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ReloadErrorBanner).assertIsDisplayed()
    }

    @Test
    fun `when error and books non-empty book grid is still shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState(
                        error = LibraryError.LoadFailed,
                        books = listOf(book1),
                        filteredBooks = listOf(book1),
                    ),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.BookGrid).assertIsDisplayed()
    }

    @Test
    fun `when error and books non-empty full-screen error is NOT shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState(
                        error = LibraryError.LoadFailed,
                        books = listOf(book1),
                        filteredBooks = listOf(book1),
                    ),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.LibraryError).assertDoesNotExist()
    }

    @Test
    fun `tapping retry in banner dispatches Refresh intent`() {
        val dispatched = mutableListOf<LibraryIntent>()
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState(
                        error = LibraryError.LoadFailed,
                        books = listOf(book1),
                        filteredBooks = listOf(book1),
                    ),
                onIntent = { dispatched.add(it) },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ReloadErrorBannerRetry).performClick()
        assertTrue(dispatched.contains(LibraryIntent.Refresh))
    }

    @Test
    fun `tapping dismiss in banner dispatches DismissError intent`() {
        val dispatched = mutableListOf<LibraryIntent>()
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState(
                        error = LibraryError.LoadFailed,
                        books = listOf(book1),
                        filteredBooks = listOf(book1),
                    ),
                onIntent = { dispatched.add(it) },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ReloadErrorBannerDismiss).performClick()
        assertTrue(dispatched.contains(LibraryIntent.DismissError))
    }

    @Test
    fun `when error and books non-empty but filteredBooks empty, reload error banner is shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState(
                        error = LibraryError.LoadFailed,
                        books = listOf(book1),
                        filteredBooks = emptyList(),
                    ),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.ReloadErrorBanner).assertIsDisplayed()
    }

    // --- Empty-library state tests ---

    @Test
    fun `empty-library message shown when books empty and not loading or failed`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = emptyList(), isLoading = false),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.EmptyLibrary).assertIsDisplayed()
    }

    @Test
    fun `add first book button shown in empty-library state`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = emptyList(), isLoading = false),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddFirstBookButton).assertIsDisplayed()
    }

    @Test
    fun `tapping Add First Book dispatches NavigateToAddBook intent`() {
        var dispatchedIntent: LibraryIntent? = null
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = emptyList(), isLoading = false),
                onIntent = { dispatchedIntent = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddFirstBookButton).performClick()
        assertEquals(LibraryIntent.NavigateToAddBook, dispatchedIntent)
    }

    // --- Empty-filter state tests ---

    @Test
    fun `empty-filter message shown when books non-empty but filteredBooks empty`() {
        val state =
            LibraryUiState(
                books = listOf(book1),
                filteredBooks = emptyList(),
                isLoading = false,
            )
        composeTestRule.setContent {
            LibraryScreenContent(uiState = state, onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.EmptyFilter).assertIsDisplayed()
    }

    @Test
    fun `add first book button NOT shown in empty-filter state`() {
        val state =
            LibraryUiState(
                books = listOf(book1),
                filteredBooks = emptyList(),
                isLoading = false,
            )
        composeTestRule.setContent {
            LibraryScreenContent(uiState = state, onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddFirstBookButton).assertDoesNotExist()
    }

    @Test
    fun `search bar shown in empty-filter state`() {
        val state =
            LibraryUiState(
                books = listOf(book1),
                filteredBooks = emptyList(),
                isLoading = false,
            )
        composeTestRule.setContent {
            LibraryScreenContent(uiState = state, onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).assertIsDisplayed()
    }

    // --- Grid/search tests ---

    @Test
    fun `grid is displayed when filteredBooks is non-empty`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = listOf(book1)),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.BookGrid).assertIsDisplayed()
    }

    @Test
    fun `grid shows one cell per book in filteredBooks`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1, book2), filteredBooks = listOf(book1, book2)),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b2")).assertIsDisplayed()
    }

    @Test
    fun `each cell shows the book title`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = listOf(book1)),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1"))
            .assertTextContains("Dune", substring = true)
    }

    @Test
    fun `each cell shows the first author`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = listOf(book1)),
                onIntent = {},
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
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = listOf(book1)),
                onIntent = {},
                onNavigateToBookDetail = { navigatedId = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1")).performClick()
        assertEquals("b1", navigatedId)
    }

    @Test
    fun `search bar is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = listOf(book1)),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).assertIsDisplayed()
    }

    @Test
    fun `search bar displays current searchQuery from state`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = listOf(book1), searchQuery = "Dune"),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).assertTextContains("Dune")
    }

    @Test
    fun `typing in search bar dispatches ChangeSearchQuery intent`() {
        val dispatched = mutableListOf<LibraryIntent>()
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = listOf(book1), searchQuery = ""),
                onIntent = { dispatched.add(it) },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SearchBar).performTextInput("Du")
        assertTrue(dispatched.any { it is LibraryIntent.ChangeSearchQuery })
    }

    @Test
    fun `sort button is displayed in top bar`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SortButton).assertIsDisplayed()
    }

    @Test
    fun `clicking sort button opens dropdown menu`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(), onIntent = {})
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
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SortButton).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.SortMenuAuthorAsc).performClick()
        assertEquals(LibraryIntent.ChangeSortOrder(SortOrder.AUTHOR_ASC), dispatched.last())
    }

    // --- Loading state tests ---

    @Test
    fun `loading indicator shown when isLoading true and books empty`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(isLoading = true, books = emptyList()),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.LoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun `loading indicator not shown when books non-empty even if isLoading true`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState(
                        isLoading = true,
                        books = listOf(book1),
                        filteredBooks = listOf(book1),
                    ),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.LoadingIndicator).assertDoesNotExist()
    }

    // --- Speed dial / recommendations tests ---

    @Test
    fun `when isPremium false only single Add Book FAB is shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(isPremium = false), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.AddBookFab).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialFab).assertDoesNotExist()
    }

    @Test
    fun `when isPremium true speed dial toggle FAB is shown`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(isPremium = true), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialFab).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.AddBookFab).assertDoesNotExist()
    }

    @Test
    fun `tapping speed dial toggle when collapsed expands speed dial options`() {
        composeTestRule.setContent {
            LibraryScreenContent(uiState = LibraryUiState(isPremium = true), onIntent = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialFab).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialAddBook).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialGetRecommendations).assertIsDisplayed()
    }

    @Test
    fun `tapping Add Book in speed dial dispatches NavigateToAddBook intent`() {
        var dispatchedIntent: LibraryIntent? = null
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(isPremium = true),
                onIntent = { dispatchedIntent = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialFab).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialAddBook).performClick()
        assertEquals(LibraryIntent.NavigateToAddBook, dispatchedIntent)
    }

    @Test
    fun `tapping Get Recommendations in speed dial dispatches RequestRecommendations`() {
        var dispatchedIntent: LibraryIntent? = null
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(isPremium = true),
                onIntent = { dispatchedIntent = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialFab).performClick()
        composeTestRule.onNodeWithTag(TestTags.Library.SpeedDialGetRecommendations).performClick()
        assertEquals(LibraryIntent.RequestRecommendations, dispatchedIntent)
    }

    @Test
    fun `quality warning dialog shown when showQualityWarning is true in state`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(showQualityWarning = true),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialog).assertIsDisplayed()
    }

    @Test
    fun `confirming quality warning dialog dispatches ConfirmRecommendations`() {
        var dispatchedIntent: LibraryIntent? = null
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(showQualityWarning = true),
                onIntent = { dispatchedIntent = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogConfirmButton).performClick()
        assertEquals(LibraryIntent.ConfirmRecommendations, dispatchedIntent)
    }

    @Test
    fun `dismissing quality warning dialog dispatches DismissQualityWarning intent`() {
        var dispatchedIntent: LibraryIntent? = null
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(showQualityWarning = true),
                onIntent = { dispatchedIntent = it },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.ErrorPresentation.ConfirmationDialogDismissButton).performClick()
        assertEquals(LibraryIntent.DismissQualityWarning, dispatchedIntent)
    }

    @Test
    fun `book grid not shown when filteredBooks is empty but books non-empty`() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState(books = listOf(book1), filteredBooks = emptyList()),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.BookGrid).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Library.bookItem("b1")).assertDoesNotExist()
    }
}
