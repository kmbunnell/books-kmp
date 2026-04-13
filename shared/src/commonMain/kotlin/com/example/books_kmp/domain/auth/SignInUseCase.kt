package com.example.books_kmp.domain.auth

import com.example.books_kmp.domain.Result

class SignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
    ): Result<Unit, SignInError> {
        if (email.isBlank()) return Result.Failure(SignInError.EmptyEmail)
        if (password.isBlank()) return Result.Failure(SignInError.EmptyPassword)
        return when (val result = authRepository.signIn(email, password)) {
            is Result.Success -> Result.Success(Unit)
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
