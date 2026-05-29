package com.example.books_kmp.domain.auth

import com.example.books_kmp.domain.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionStatus: Flow<AuthSessionState>

    suspend fun signUp(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError>

    suspend fun signOut(): Result<Unit, AuthRepositoryError>
}
