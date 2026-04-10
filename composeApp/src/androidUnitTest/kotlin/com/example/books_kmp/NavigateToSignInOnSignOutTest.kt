package com.example.books_kmp

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NavigateToSignInOnSignOutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `onSignedOut fires when not authenticated and not loading`() {
        var fired = false
        composeTestRule.setContent {
            NavigateToSignInOnSignOut(
                isAuthenticated = false,
                isLoading = false,
                onSignedOut = { fired = true },
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(fired)
    }

    @Test
    fun `onSignedOut does not fire when still loading`() {
        var fired = false
        composeTestRule.setContent {
            NavigateToSignInOnSignOut(
                isAuthenticated = false,
                isLoading = true,
                onSignedOut = { fired = true },
            )
        }
        composeTestRule.waitForIdle()
        assertFalse(fired)
    }

    @Test
    fun `onSignedOut does not fire when authenticated`() {
        var fired = false
        composeTestRule.setContent {
            NavigateToSignInOnSignOut(
                isAuthenticated = true,
                isLoading = false,
                onSignedOut = { fired = true },
            )
        }
        composeTestRule.waitForIdle()
        assertFalse(fired)
    }
}
