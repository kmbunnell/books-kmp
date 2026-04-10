package com.example.books_kmp.ui.library

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class LibraryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `sign out button is displayed`() {
        composeTestRule.setContent {
            LibraryScreenContent(onSignOut = {})
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SignOutButton).assertIsDisplayed()
    }

    @Test
    fun `clicking sign out button triggers onSignOut callback`() {
        var signOutCalled = false
        composeTestRule.setContent {
            LibraryScreenContent(onSignOut = { signOutCalled = true })
        }
        composeTestRule.onNodeWithTag(TestTags.Library.SignOutButton).performClick()
        assertTrue(signOutCalled)
    }
}
