package com.example.books_kmp.domain.auth

import com.example.books_kmp.Result
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SignUpUseCaseTest {
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var useCase: SignUpUseCase

    @BeforeTest
    fun setUp() {
        fakeRepo = FakeAuthRepository()
        useCase = SignUpUseCase(fakeRepo)
    }

    @Test
    fun `returns Failure with EmptyEmail when email is blank`() =
        runTest {
            val result = useCase("", "password123", "password123")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.EmptyEmail, result.error)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `returns Failure with EmptyPassword when password is blank`() =
        runTest {
            val result = useCase("test@example.com", "", "")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.EmptyPassword, result.error)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `returns Failure with EmptyConfirmPassword when confirmPassword is blank`() =
        runTest {
            val result = useCase("test@example.com", "password123", "")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.EmptyConfirmPassword, result.error)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `returns Failure with PasswordMismatch when passwords do not match`() =
        runTest {
            val result = useCase("test@example.com", "password123", "different")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.PasswordMismatch, result.error)
            assertFalse(fakeRepo.signUpCalled)
        }

    @Test
    fun `returns Success when repository sign-up succeeds`() =
        runTest {
            val result = useCase("test@example.com", "password123", "password123")

            assertIs<Result.Success>(result)
            assertTrue(fakeRepo.signUpCalled)
        }

    @Test
    fun `returns Failure with EmailAlreadyInUse when repository returns EmailAlreadyInUse`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.EmailAlreadyInUse)

            val result = useCase("test@example.com", "password123", "password123")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.EmailAlreadyInUse, result.error)
        }

    @Test
    fun `returns Failure with WeakPassword when repository returns WeakPassword`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.WeakPassword)

            val result = useCase("test@example.com", "weak", "weak")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.WeakPassword, result.error)
        }

    @Test
    fun `returns Failure with InvalidEmail when repository returns InvalidEmail`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.InvalidEmail)

            val result = useCase("not-an-email", "password123", "password123")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.InvalidEmail, result.error)
        }

    @Test
    fun `returns Failure with SignUpFailed when repository returns NetworkError`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.NetworkError)

            val result = useCase("test@example.com", "password123", "password123")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.SignUpFailed, result.error)
        }

    @Test
    fun `returns Failure with SignUpFailed when repository returns Unknown`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.Unknown)

            val result = useCase("test@example.com", "password123", "password123")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.SignUpFailed, result.error)
        }

    @Test
    fun `validates email before password`() =
        runTest {
            val result = useCase("", "", "")

            assertIs<Result.Failure<SignUpError>>(result)
            assertEquals(SignUpError.EmptyEmail, result.error)
        }
}
