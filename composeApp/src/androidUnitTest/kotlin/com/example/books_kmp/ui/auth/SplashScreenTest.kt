package com.example.books_kmp.ui.auth

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.AuthUiState
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SplashScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows loading indicator when auth state is Loading`() {
        composeTestRule.setContent {
            SplashScreen(
                uiState = AuthUiState(isLoading = true),
                onAuthenticated = {},
                onNotAuthenticated = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Splash.LoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun `calls onAuthenticated when state is Authenticated`() {
        var callbackFired = false
        composeTestRule.setContent {
            SplashScreen(
                uiState = AuthUiState(isAuthenticated = true),
                onAuthenticated = { callbackFired = true },
                onNotAuthenticated = {},
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(callbackFired)
    }

    @Test
    fun `calls onNotAuthenticated when state is NotAuthenticated`() {
        var callbackFired = false
        composeTestRule.setContent {
            SplashScreen(
                uiState = AuthUiState(isLoading = false, isAuthenticated = false),
                onAuthenticated = {},
                onNotAuthenticated = { callbackFired = true },
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(callbackFired)
    }
}
