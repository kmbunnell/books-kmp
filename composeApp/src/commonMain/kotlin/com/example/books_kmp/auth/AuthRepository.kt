package com.example.books_kmp.auth

import com.example.books_kmp.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionStatus: Flow<AuthSessionState>

    suspend fun signUp(
        email: String,
        password: String,
    ): Result<AuthRepositoryError>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthRepositoryError>

    suspend fun signOut()
}
