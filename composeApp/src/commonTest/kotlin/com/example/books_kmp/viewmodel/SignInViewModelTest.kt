package com.example.books_kmp.viewmodel

import app.cash.turbine.test
import com.example.books_kmp.Result
import com.example.books_kmp.auth.AuthRepositoryError
import com.example.books_kmp.auth.FakeAuthRepository
import com.example.books_kmp.auth.SignInError
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
class SignInViewModelTest {
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
    fun `ShowError effect with InvalidCredentials emitted when sign-in fails`() =
        runTest {
            fakeRepo.signInResult = Result.Failure(AuthRepositoryError.InvalidCredentials)
            val viewModel = SignInViewModel(signInUseCase)

            viewModel.effects.test {
                viewModel.onIntent(SignInIntent.SignIn("test@example.com", "wrong-password"))
                val effect = awaitItem()
                assertIs<SignInEffect.ShowError>(effect)
                assertIs<SignInError.InvalidCredentials>(effect.error)
            }
        }

    @Test
    fun `sign in with blank email sets email error and does not call repository`() =
        runTest {
            val viewModel = SignInViewModel(signInUseCase)

            viewModel.onIntent(SignInIntent.SignIn("", "password123"))

            assertEquals(SignInError.EmptyEmail, viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `sign in with blank password sets password error and does not call repository`() =
        runTest {
            val viewModel = SignInViewModel(signInUseCase)

            viewModel.onIntent(SignInIntent.SignIn("test@example.com", ""))

            assertNull(viewModel.uiState.value.emailError)
            assertEquals(SignInError.EmptyPassword, viewModel.uiState.value.passwordError)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `sign in with valid fields calls repository and clears validation errors`() =
        runTest {
            val viewModel = SignInViewModel(signInUseCase)

            viewModel.onIntent(SignInIntent.SignIn("test@example.com", "password123"))

            assertNull(viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertTrue(fakeRepo.signInCalled)
        }

    @Test
    fun `isLoading is false after successful sign-in`() =
        runTest {
            val viewModel = SignInViewModel(signInUseCase)

            viewModel.onIntent(SignInIntent.SignIn("test@example.com", "password123"))

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `ShowError effect with SignInFailed emitted when repository returns NetworkError`() =
        runTest {
            fakeRepo.signInResult = Result.Failure(AuthRepositoryError.NetworkError)
            val viewModel = SignInViewModel(signInUseCase)

            viewModel.effects.test {
                viewModel.onIntent(SignInIntent.SignIn("test@example.com", "password123"))
                val effect = awaitItem()
                assertIs<SignInEffect.ShowError>(effect)
                assertIs<SignInError.SignInFailed>(effect.error)
            }
        }

    @Test
    fun `isLoading is false after sign-in fails`() =
        runTest {
            fakeRepo.signInResult = Result.Failure(AuthRepositoryError.InvalidCredentials)
            val viewModel = SignInViewModel(signInUseCase)

            viewModel.effects.test {
                viewModel.onIntent(SignInIntent.SignIn("test@example.com", "password"))
                awaitItem() // consume the effect
            }

            assertFalse(viewModel.uiState.value.isLoading)
        }
}
