package com.example.books_kmp.ui.library

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.LibraryIntent
import com.example.books_kmp.viewmodel.LibraryUiState
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
}
