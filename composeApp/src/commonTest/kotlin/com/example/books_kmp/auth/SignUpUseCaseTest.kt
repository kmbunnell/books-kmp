package com.example.books_kmp.auth

import com.example.books_kmp.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SignUpUseCaseTest {
    private val fakeRepo = FakeAuthRepository()
    private val useCase = SignUpUseCase(fakeRepo)

    @Test
    fun `returns EmptyEmail failure when email is blank`() =
        runTest {
            val result = useCase("", "password123", "password123")
            assertEquals(Result.Failure(SignUpError.EmptyEmail), result)
        }

    @Test
    fun `returns EmptyPassword failure when password is blank`() =
        runTest {
            val result = useCase("test@example.com", "", "")
            assertEquals(Result.Failure(SignUpError.EmptyPassword), result)
        }

    @Test
    fun `returns PasswordMismatch failure when passwords do not match`() =
        runTest {
            val result = useCase("test@example.com", "password123", "different")
            assertEquals(Result.Failure(SignUpError.PasswordMismatch), result)
        }

    @Test
    fun `returns EmptyConfirmPassword failure when confirm password is blank`() =
        runTest {
            val result = useCase("test@example.com", "password123", "")
            assertEquals(Result.Failure(SignUpError.EmptyConfirmPassword), result)
        }

    @Test
    fun `returns Success when credentials are valid and repository succeeds`() =
        runTest {
            val result = useCase("test@example.com", "password123", "password123")
            assertEquals(Result.Success, result)
        }

    @Test
    fun `returns SignUpFailed when repository returns Unknown`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.Unknown)
            val result = useCase("test@example.com", "password123", "password123")
            assertEquals(Result.Failure(SignUpError.SignUpFailed), result)
        }

    @Test
    fun `returns EmailAlreadyInUse when repository returns EmailAlreadyInUse`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.EmailAlreadyInUse)
            val result = useCase("test@example.com", "password123", "password123")
            assertEquals(Result.Failure(SignUpError.EmailAlreadyInUse), result)
        }

    @Test
    fun `returns WeakPassword when repository returns WeakPassword`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.WeakPassword)
            val result = useCase("test@example.com", "weak", "weak")
            assertEquals(Result.Failure(SignUpError.WeakPassword), result)
        }

    @Test
    fun `returns InvalidEmail when repository returns InvalidEmail`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.InvalidEmail)
            val result = useCase("not-an-email", "password123", "password123")
            assertEquals(Result.Failure(SignUpError.InvalidEmail), result)
        }

    @Test
    fun `returns SignUpFailed when repository returns NetworkError`() =
        runTest {
            fakeRepo.signUpResult = Result.Failure(AuthRepositoryError.NetworkError)
            val result = useCase("test@example.com", "password123", "password123")
            assertEquals(Result.Failure(SignUpError.SignUpFailed), result)
        }
}
