package com.example.books_kmp.auth

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.auth.AuthRepository
import com.example.books_kmp.domain.auth.AuthRepositoryError
import com.example.books_kmp.domain.auth.AuthSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableSharedFlow<AuthSessionState>(replay = 1)
    var signInResult: Result<Unit, AuthRepositoryError> = Result.Success(Unit)
    var signUpResult: Result<Unit, AuthRepositoryError> = Result.Success(Unit)
    var signOutException: Exception? = null
    var signInCalled = false
    var signUpCalled = false

    override val sessionStatus: Flow<AuthSessionState> = sessionFlow

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError> {
        signUpCalled = true
        return signUpResult
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError> {
        signInCalled = true
        return signInResult
    }

    override suspend fun signOut() {
        signOutException?.let { throw it }
    }
}
