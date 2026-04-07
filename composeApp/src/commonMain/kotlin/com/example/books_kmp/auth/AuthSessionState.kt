package com.example.books_kmp.auth

sealed interface AuthSessionState {
    data object Loading : AuthSessionState

    data class Authenticated(val userId: String) : AuthSessionState

    data object NotAuthenticated : AuthSessionState

    data object Error : AuthSessionState
}
