package com.example.books_kmp.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableSharedFlow<AuthSessionState>(replay = 1)
    var signInException: Exception? = null
    var signUpException: Exception? = null
    var signOutException: Exception? = null
    var signInCalled = false

    override val sessionStatus: Flow<AuthSessionState> = sessionFlow

    override suspend fun signUp(
        email: String,
        password: String,
    ) {
        signUpException?.let { throw it }
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ) {
        signInCalled = true
        signInException?.let { throw it }
    }

    override suspend fun signOut() {
        signOutException?.let { throw it }
    }
}
