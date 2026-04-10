package com.example.books_kmp.domain.auth

sealed interface SignUpError {
    data object EmptyEmail : SignUpError

    data object EmptyPassword : SignUpError

    data object EmptyConfirmPassword : SignUpError

    data object PasswordMismatch : SignUpError

    data object WeakPassword : SignUpError

    data object InvalidEmail : SignUpError

    data object EmailAlreadyInUse : SignUpError

    data object SignUpFailed : SignUpError
}
