package com.example.books_kmp.domain.auth

sealed interface SignInError {
    data object EmptyEmail : SignInError

    data object EmptyPassword : SignInError

    data object InvalidCredentials : SignInError

    data object EmailNotVerified : SignInError

    data object SignInFailed : SignInError
}
