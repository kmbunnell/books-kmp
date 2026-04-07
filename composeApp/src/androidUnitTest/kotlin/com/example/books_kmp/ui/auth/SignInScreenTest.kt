package com.example.books_kmp.ui.auth

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.AuthEffect
import com.example.books_kmp.viewmodel.AuthError
import com.example.books_kmp.viewmodel.AuthUiState
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SignInScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows email and password fields`() {
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(),
                effects = MutableSharedFlow(),
                onSignIn = { _, _ -> },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignIn.EmailField).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SignIn.PasswordField).assertIsDisplayed()
    }

    @Test
    fun `shows email error from uiState`() {
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(emailError = AuthError.EmailRequired),
                effects = MutableSharedFlow(),
                onSignIn = { _, _ -> },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.onNodeWithText("Email is required").assertIsDisplayed()
    }

    @Test
    fun `shows password error from uiState`() {
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(passwordError = AuthError.PasswordRequired),
                effects = MutableSharedFlow(),
                onSignIn = { _, _ -> },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.onNodeWithText("Password is required").assertIsDisplayed()
    }

    @Test
    fun `shows loading indicator and disables button when isLoading is true`() {
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(isLoading = true),
                effects = MutableSharedFlow(),
                onSignIn = { _, _ -> },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignIn.LoadingIndicator).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SignIn.SignInButton).assertIsNotEnabled()
    }

    @Test
    fun `invokes onSignIn with email and password when fields are valid`() {
        var capturedEmail = ""
        var capturedPassword = ""
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(),
                effects = MutableSharedFlow(),
                onSignIn = { email, password ->
                    capturedEmail = email
                    capturedPassword = password
                },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignIn.EmailField).performTextInput("test@example.com")
        composeTestRule.onNodeWithTag(TestTags.SignIn.PasswordField).performTextInput("password123")
        composeTestRule.onNodeWithTag(TestTags.SignIn.SignInButton).performClick()
        assertEquals("test@example.com", capturedEmail)
        assertEquals("password123", capturedPassword)
    }

    @Test
    fun `shows snackbar when ShowError effect with InvalidCredentials is emitted`() {
        val effects = MutableSharedFlow<AuthEffect>(extraBufferCapacity = 1)
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(),
                effects = effects,
                onSignIn = { _, _ -> },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.waitForIdle()
        effects.tryEmit(AuthEffect.ShowError(AuthError.InvalidCredentials))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Invalid email or password").assertIsDisplayed()
    }

    @Test
    fun `shows snackbar when ShowError effect with SessionExpired is emitted`() {
        val effects = MutableSharedFlow<AuthEffect>(extraBufferCapacity = 1)
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(),
                effects = effects,
                onSignIn = { _, _ -> },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.waitForIdle()
        effects.tryEmit(AuthEffect.ShowError(AuthError.SessionExpired))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your session has expired. Please sign in again.").assertIsDisplayed()
    }

    @Test
    fun `sign up link is present`() {
        composeTestRule.setContent {
            SignInScreen(
                uiState = AuthUiState(),
                effects = MutableSharedFlow(),
                onSignIn = { _, _ -> },
                onNavigateToSignUp = {},
            )
        }
        composeTestRule.onNodeWithText("Sign Up", substring = true).assertIsDisplayed()
    }
}
