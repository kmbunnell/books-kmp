package com.example.books_kmp.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionStatus: Flow<AuthSessionState>

    suspend fun signUp(
        email: String,
        password: String
    )

    suspend fun signIn(
        email: String,
        password: String
    )

    suspend fun signOut()
}
