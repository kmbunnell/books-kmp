package com.example.books_kmp.domain.auth

import com.example.books_kmp.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableSharedFlow<AuthSessionState>(replay = 1)
    var signInResult: Result<AuthRepositoryError> = Result.Success
    var signUpResult: Result<AuthRepositoryError> = Result.Success
    var signOutException: Exception? = null
    var signInCalled = false
    var signUpCalled = false

    override val sessionStatus: Flow<AuthSessionState> = sessionFlow

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<AuthRepositoryError> {
        signUpCalled = true
        return signUpResult
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthRepositoryError> {
        signInCalled = true
        return signInResult
    }

    override suspend fun signOut() {
        signOutException?.let { throw it }
    }
}
