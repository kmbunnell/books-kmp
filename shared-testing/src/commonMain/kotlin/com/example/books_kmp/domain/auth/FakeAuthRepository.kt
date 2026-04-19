package com.example.books_kmp.domain.auth

import com.example.books_kmp.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableSharedFlow<AuthSessionState>(replay = 1)
    var signInResult: Result<Unit, AuthRepositoryError> = Result.Success(Unit)
    var signUpResult: Result<Unit, AuthRepositoryError> = Result.Success(Unit)
    var signOutException: Exception? = null
    var signInCalled = false
    var signUpCalled = false

    // Optional gates — tests set these to suspend sign-in/up until completed,
    // allowing deterministic observation of in-flight ViewModel state.
    var signInGate: CompletableDeferred<Unit>? = null
    var signUpGate: CompletableDeferred<Unit>? = null

    override val sessionStatus: Flow<AuthSessionState> = sessionFlow

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError> {
        signUpCalled = true
        signUpGate?.await()
        return signUpResult
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError> {
        signInCalled = true
        signInGate?.await()
        return signInResult
    }

    override suspend fun signOut() {
        signOutException?.let { throw it }
    }
}
