package com.example.books_kmp.domain.auth

sealed interface AuthRepositoryError {
    data object EmailAlreadyInUse : AuthRepositoryError

    data object InvalidCredentials : AuthRepositoryError

    data object WeakPassword : AuthRepositoryError

    data object InvalidEmail : AuthRepositoryError

    data object NetworkError : AuthRepositoryError

    data object Unknown : AuthRepositoryError
}
