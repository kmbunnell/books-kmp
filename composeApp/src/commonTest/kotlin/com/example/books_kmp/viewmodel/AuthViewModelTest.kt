package com.example.books_kmp.viewmodel

import app.cash.turbine.test
import com.example.books_kmp.auth.AuthSessionState
import com.example.books_kmp.auth.FakeAuthRepository
import com.example.books_kmp.auth.SignInUseCase
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
    private lateinit var signInUseCase: SignInUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        signInUseCase = SignInUseCase(fakeRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is NotAuthenticated when repository emits not authenticated`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            val state = viewModel.uiState.value
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.userId)
        }

    @Test
    fun `state transitions to Authenticated after successful sign-in intent`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

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
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.onIntent(AuthIntent.SignOut)
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)

            val state = viewModel.uiState.value
            assertFalse(state.isAuthenticated)
            assertNull(state.userId)
        }

    @Test
    fun `ShowError effect with InvalidCredentials emitted when sign-in fails`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            fakeRepo.signInException = Exception("Invalid credentials")
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.effects.test {
                viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", "wrong-password"))
                val effect = awaitItem()
                assertIs<AuthEffect.ShowError>(effect)
                assertIs<AuthError.InvalidCredentials>(effect.error)
            }
        }

    @Test
    fun `ShowError effect with SessionExpired emitted when session status emits Error`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.effects.test {
                fakeRepo.sessionFlow.emit(AuthSessionState.Error)
                val effect = awaitItem()
                assertIs<AuthEffect.ShowError>(effect)
                assertIs<AuthError.SessionExpired>(effect.error)
            }
        }

    @Test
    fun `sign in with blank email sets email error and does not call repository`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.onIntent(AuthIntent.SignInWithEmail("", "password123"))

            assertEquals(AuthError.EmailRequired, viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `sign in with blank password sets password error and does not call repository`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", ""))

            assertNull(viewModel.uiState.value.emailError)
            assertEquals(AuthError.PasswordRequired, viewModel.uiState.value.passwordError)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `sign in with valid fields calls repository and clears validation errors`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", "password123"))

            assertNull(viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertTrue(fakeRepo.signInCalled)
        }

    @Test
    fun `isLoading is true while sign-in is in progress`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", "password123"))

            // After successful sign-in, isLoading stays true until session flow updates
            assertTrue(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `isLoading is false after sign-in fails`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            fakeRepo.signInException = Exception("fail")
            val viewModel = AuthViewModel(fakeRepo, signInUseCase)

            viewModel.effects.test {
                viewModel.onIntent(AuthIntent.SignInWithEmail("test@example.com", "password"))
                awaitItem() // consume the effect
            }

            assertFalse(viewModel.uiState.value.isLoading)
        }
}
