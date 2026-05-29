package com.example.books_kmp.domain.auth

import com.example.books_kmp.domain.Result

class SignUpUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
    ): Result<Unit, SignUpError> {
        if (email.isBlank()) return Result.Failure(SignUpError.EmptyEmail)
        if (password.isBlank()) return Result.Failure(SignUpError.EmptyPassword)
        if (confirmPassword.isBlank()) return Result.Failure(SignUpError.EmptyConfirmPassword)
        if (password != confirmPassword) return Result.Failure(SignUpError.PasswordMismatch)
        return when (val result = authRepository.signUp(email, password)) {
            is Result.Success -> result
            is Result.Failure ->
                Result.Failure(
                    when (result.error) {
                        AuthRepositoryError.EmailAlreadyInUse -> SignUpError.EmailAlreadyInUse
                        AuthRepositoryError.WeakPassword -> SignUpError.WeakPassword
                        AuthRepositoryError.InvalidEmail -> SignUpError.InvalidEmail
                        AuthRepositoryError.EmailRateLimitExceeded -> SignUpError.EmailRateLimitExceeded
                        AuthRepositoryError.InvalidCredentials,
                        AuthRepositoryError.EmailNotVerified,
                        AuthRepositoryError.NetworkError,
                        AuthRepositoryError.Unknown -> SignUpError.SignUpFailed
                    },
                )
        }
    }
}
