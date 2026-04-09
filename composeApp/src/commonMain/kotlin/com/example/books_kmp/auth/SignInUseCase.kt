package com.example.books_kmp.auth

import com.example.books_kmp.Result

class SignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
    ): Result<SignInError> {
        if (email.isBlank()) return Result.Failure(SignInError.EmptyEmail)
        if (password.isBlank()) return Result.Failure(SignInError.EmptyPassword)
        return when (val result = authRepository.signIn(email, password)) {
            Result.Success -> Result.Success
            is Result.Failure ->
                Result.Failure(
                    when (result.error) {
                        AuthRepositoryError.InvalidCredentials -> SignInError.InvalidCredentials
                        AuthRepositoryError.EmailAlreadyInUse,
                        AuthRepositoryError.WeakPassword,
                        AuthRepositoryError.InvalidEmail,
                        AuthRepositoryError.NetworkError,
                        AuthRepositoryError.Unknown -> SignInError.SignInFailed
                    },
                )
        }
    }
}
