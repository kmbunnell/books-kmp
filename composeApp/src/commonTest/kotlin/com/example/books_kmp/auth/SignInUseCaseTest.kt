package com.example.books_kmp.auth

import com.example.books_kmp.Result
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SignInUseCaseTest {
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var useCase: SignInUseCase

    @BeforeTest
    fun setUp() {
        fakeRepo = FakeAuthRepository()
        useCase = SignInUseCase(fakeRepo)
    }

    @Test
    fun `returns Failure with EmptyEmail when email is blank`() =
        runTest {
            val result = useCase("", "password123")

            assertIs<Result.Failure<SignInError>>(result)
            assertEquals(SignInError.EmptyEmail, result.error)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `returns Failure with EmptyPassword when password is blank`() =
        runTest {
            val result = useCase("test@example.com", "")

            assertIs<Result.Failure<SignInError>>(result)
            assertEquals(SignInError.EmptyPassword, result.error)
            assertFalse(fakeRepo.signInCalled)
        }

    @Test
    fun `returns Success when repository sign-in succeeds`() =
        runTest {
            val result = useCase("test@example.com", "password123")

            assertIs<Result.Success>(result)
            assertTrue(fakeRepo.signInCalled)
        }

    @Test
    fun `returns Failure with InvalidCredentials when repository throws`() =
        runTest {
            fakeRepo.signInException = Exception("Invalid credentials")

            val result = useCase("test@example.com", "wrong-password")

            assertIs<Result.Failure<SignInError>>(result)
            assertEquals(SignInError.InvalidCredentials, result.error)
            assertTrue(fakeRepo.signInCalled)
        }

    @Test
    fun `validates email before password`() =
        runTest {
            val result = useCase("", "")

            assertIs<Result.Failure<SignInError>>(result)
            assertEquals(SignInError.EmptyEmail, result.error)
        }
}
