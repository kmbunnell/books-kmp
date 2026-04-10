package com.example.books_kmp.ui.auth

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.example.books_kmp.domain.auth.SignUpError
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.SignUpEffect
import com.example.books_kmp.viewmodel.SignUpUiState
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SignUpScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows email password and confirm password fields`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignUp.EmailField).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SignUp.PasswordField).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SignUp.ConfirmPasswordField).assertIsDisplayed()
    }

    @Test
    fun `shows email error from uiState`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(emailError = SignUpError.EmptyEmail),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Email is required").assertIsDisplayed()
    }

    @Test
    fun `shows password error from uiState`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(passwordError = SignUpError.EmptyPassword),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Password is required").assertIsDisplayed()
    }

    @Test
    fun `shows confirm password mismatch error from uiState`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(confirmPasswordError = SignUpError.PasswordMismatch),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }

    @Test
    fun `shows loading indicator and disables button when isLoading is true`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(isLoading = true),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignUp.LoadingIndicator).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SignUp.CreateAccountButton).assertIsNotEnabled()
    }

    @Test
    fun `invokes onSignUp with correct args when fields are filled`() {
        var capturedEmail = ""
        var capturedPassword = ""
        var capturedConfirm = ""
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { email, password, confirm ->
                    capturedEmail = email
                    capturedPassword = password
                    capturedConfirm = confirm
                },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignUp.EmailField).performTextInput("test@example.com")
        composeTestRule.onNodeWithTag(TestTags.SignUp.PasswordField).performTextInput("password123")
        composeTestRule.onNodeWithTag(TestTags.SignUp.ConfirmPasswordField).performTextInput("password123")
        composeTestRule.onNodeWithTag(TestTags.SignUp.CreateAccountButton).performClick()
        assertEquals("test@example.com", capturedEmail)
        assertEquals("password123", capturedPassword)
        assertEquals("password123", capturedConfirm)
    }

    @Test
    fun `shows snackbar when ShowError effect is emitted`() {
        val effects = MutableSharedFlow<SignUpEffect>(extraBufferCapacity = 1)
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = effects,
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.waitForIdle()
        effects.tryEmit(SignUpEffect.ShowError(SignUpError.SignUpFailed))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sign up failed. Please try again.").assertIsDisplayed()
    }

    @Test
    fun `shows confirm password required error when EmptyConfirmPassword`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(confirmPasswordError = SignUpError.EmptyConfirmPassword),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Please confirm your password").assertIsDisplayed()
    }

    @Test
    fun `shows email already in use snackbar when EmailAlreadyInUse effect is emitted`() {
        val effects = MutableSharedFlow<SignUpEffect>(extraBufferCapacity = 1)
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = effects,
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.waitForIdle()
        effects.tryEmit(SignUpEffect.ShowError(SignUpError.EmailAlreadyInUse))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("An account with that email already exists").assertIsDisplayed()
    }

    @Test
    fun `shows weak password error from uiState`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(passwordError = SignUpError.WeakPassword),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Password does not meet the minimum requirements").assertIsDisplayed()
    }

    @Test
    fun `shows invalid email format error from uiState`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(emailError = SignUpError.InvalidEmail),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Please enter a valid email address").assertIsDisplayed()
    }

    @Test
    fun `password toggle buttons are displayed`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignUp.PasswordToggle).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SignUp.ConfirmPasswordToggle).assertIsDisplayed()
    }

    @Test
    fun `password toggle shows and hides password`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignUp.PasswordToggle).performClick()
        // Password field revealed — its icon now says "Hide password"
        composeTestRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }

    @Test
    fun `confirm password toggle shows and hides password`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        // Click only the confirm password toggle
        composeTestRule.onNodeWithTag(TestTags.SignUp.ConfirmPasswordToggle).performClick()
        // Now one "Hide password" (confirm) and one "Show password" (password field unchanged)
        composeTestRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Show password").assertIsDisplayed()
    }

    @Test
    fun `password toggles are independent`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        // Click only the password field toggle
        composeTestRule.onNodeWithTag(TestTags.SignUp.PasswordToggle).performClick()
        // One "Hide password" (password field), one "Show password" (confirm field unchanged)
        composeTestRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Show password").assertIsDisplayed()
    }

    @Test
    fun `shows password requirement hint`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun `pressing IME next on email moves focus to password`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.SignUp.EmailField).performImeAction()
        composeTestRule.onNodeWithTag(TestTags.SignUp.PasswordField).assertIsFocused()
    }

    @Test
    fun `sign in link is present`() {
        composeTestRule.setContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                effects = MutableSharedFlow(),
                onSignUp = { _, _, _ -> },
                onNavigateToSignIn = {},
            )
        }
        composeTestRule.onNodeWithText("Sign In", substring = true).assertIsDisplayed()
    }
}
