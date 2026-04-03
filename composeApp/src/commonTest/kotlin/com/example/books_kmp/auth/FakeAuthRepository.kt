package com.example.books_kmp.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableSharedFlow<AuthSessionState>(replay = 1)
    var signInResult: Result<Unit> = Result.success(Unit)
    var signUpResult: Result<Unit> = Result.success(Unit)
    var signOutResult: Result<Unit> = Result.success(Unit)

    override val sessionStatus: Flow<AuthSessionState> = sessionFlow

    override suspend fun signUp(
        email: String,
        password: String
    ) {
        signUpResult.getOrThrow()
    }

    override suspend fun signIn(
        email: String,
        password: String
    ) {
        signInResult.getOrThrow()
    }

    override suspend fun signOut() {
        signOutResult.getOrThrow()
    }
}
