package com.example.books_kmp.viewmodel

import app.cash.turbine.test
import com.example.books_kmp.auth.AuthSessionState
import com.example.books_kmp.auth.FakeAuthRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is NotAuthenticated when repository emits not authenticated`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo)

            val state = viewModel.uiState.value
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.userId)
        }

    @Test
    fun `state transitions to Authenticated after successful sign-in intent`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", "password123"))
            fakeRepo.sessionFlow.emit(AuthSessionState.Authenticated("user-123"))

            val state = viewModel.uiState.value
            assertTrue(state.isAuthenticated)
            assertEquals("user-123", state.userId)
        }

    @Test
    fun `state transitions to NotAuthenticated after sign-out intent`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.Authenticated("user-123"))
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.onIntent(AuthIntent.SignOut)
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)

            val state = viewModel.uiState.value
            assertFalse(state.isAuthenticated)
            assertNull(state.userId)
        }

    @Test
    fun `ShowError effect emitted when sign-in throws`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            fakeRepo.signInResult = Result.failure(Exception("Invalid credentials"))
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.effects.test {
                viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", "wrong-password"))
                val effect = awaitItem()
                assertIs<AuthEffect.ShowError>(effect)
                assertIs<AuthError.SignInFailed>(effect.error)
                assertEquals("Invalid credentials", effect.error.cause)
            }
        }

    @Test
    fun `ShowError effect emitted when session status emits Error`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.effects.test {
                fakeRepo.sessionFlow.emit(AuthSessionState.Error("Session expired"))
                val effect = awaitItem()
                assertIs<AuthEffect.ShowError>(effect)
                assertIs<AuthError.SessionError>(effect.error)
                assertEquals("Session expired", effect.error.cause)
            }
        }

    @Test
    fun `sign in with blank email sets email error and does not call repository`() =
        runTest {
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.onIntent(AuthIntent.SignInWithEmail("", "password123"))

            assertEquals(AuthError.EmailRequired, viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `sign in with blank password sets password error and does not call repository`() =
        runTest {
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", ""))

            assertNull(viewModel.uiState.value.emailError)
            assertEquals(AuthError.PasswordRequired, viewModel.uiState.value.passwordError)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `sign in with valid fields calls repository and clears validation errors`() =
        runTest {
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", "password123"))

            assertNull(viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertTrue(fakeRepo.signInCalled)
        }
}
