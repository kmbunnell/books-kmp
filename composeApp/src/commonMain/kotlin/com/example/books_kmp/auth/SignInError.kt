package com.example.books_kmp.auth

sealed interface SignInError {
    data object EmptyEmail : SignInError

    data object EmptyPassword : SignInError

    data object InvalidCredentials : SignInError

    data object SignInFailed : SignInError
}
