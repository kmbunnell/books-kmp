package com.example.books_kmp.viewmodel

import app.cash.turbine.test
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.auth.AuthRepositoryError
import com.example.books_kmp.domain.auth.FakeAuthRepository
import com.example.books_kmp.domain.auth.SignUpError
import com.example.books_kmp.domain.auth.SignUpUseCase
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
class SignUpViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var signUpUseCase: SignUpUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        signUpUseCase = SignUpUseCase(fakeRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sign up with blank email sets email error and does not call repository`() =
        runTest {
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("", "password123", "password123"))

            assertEquals(SignUpError.EmptyEmail, viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertNull(viewModel.uiState.value.confirmPasswordError)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `sign up with blank password sets password error and does not call repository`() =
        runTest {
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "", ""))

            assertNull(viewModel.uiState.value.emailError)
            assertEquals(SignUpError.EmptyPassword, viewModel.uiState.value.passwordError)
            assertNull(viewModel.uiState.value.confirmPasswordError)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `sign up with blank confirm password sets EmptyConfirmPassword error and does not call repository`() =
        runTest {
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", ""))

            assertNull(viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertEquals(SignUpError.EmptyConfirmPassword, viewModel.uiState.value.confirmPasswordError)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `sign up with mismatched passwords sets PasswordMismatch error and does not call repository`() =
        runTest {
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", "different"))

            assertNull(viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertEquals(SignUpError.PasswordMismatch, viewModel.uiState.value.confirmPasswordError)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `sign up with valid fields calls repository and clears all errors`() =
        runTest {
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", "password123"))

            assertNull(viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertNull(viewModel.uiState.value.confirmPasswordError)
            assertTrue(fakeRepo.signUpCalled)
        }

    @Test
    fun `WeakPassword sets password field error when repository returns WeakPassword`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.WeakPassword)
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "weak", "weak"))

            assertEquals(SignUpError.WeakPassword, viewModel.uiState.value.passwordError)
            assertNull(viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.confirmPasswordError)
        }

    @Test
    fun `InvalidEmail sets email field error when repository returns InvalidEmail`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.InvalidEmail)
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("not-an-email", "password123", "password123"))

            assertEquals(SignUpError.InvalidEmail, viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.passwordError)
            assertNull(viewModel.uiState.value.confirmPasswordError)
        }

    @Test
    fun `ShowError effect with SignUpFailed emitted when repository returns NetworkError`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.NetworkError)
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.effects.test {
                viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", "password123"))
                val effect = awaitItem()
                assertIs<SignUpEffect.ShowError>(effect)
                assertIs<SignUpError.SignUpFailed>(effect.error)
            }
        }

    @Test
    fun `ShowError effect with SignUpFailed emitted when repository returns Unknown`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.Unknown)
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.effects.test {
                viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", "password123"))
                val effect = awaitItem()
                assertIs<SignUpEffect.ShowError>(effect)
                assertIs<SignUpError.SignUpFailed>(effect.error)
            }
        }

    @Test
    fun `ShowError effect with EmailAlreadyInUse emitted when repository returns EmailAlreadyInUse`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.EmailAlreadyInUse)
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.effects.test {
                viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", "password123"))
                val effect = awaitItem()
                assertIs<SignUpEffect.ShowError>(effect)
                assertIs<SignUpError.EmailAlreadyInUse>(effect.error)
            }
        }

    @Test
    fun `isLoading is false after successful sign up`() =
        runTest {
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", "password123"))

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `isLoading is false after sign up fails`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.Unknown)
            val viewModel = SignUpViewModel(signUpUseCase)

            viewModel.effects.test {
                viewModel.onIntent(SignUpIntent.SignUp("test@example.com", "password123", "password123"))
                awaitItem() // consume the effect
            }

            assertFalse(viewModel.uiState.value.isLoading)
        }
}
