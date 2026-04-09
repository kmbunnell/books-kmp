package com.example.books_kmp.auth

import com.example.books_kmp.Result

class SignUpUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
    ): Result<SignUpError> {
        if (email.isBlank()) return Result.Failure(SignUpError.EmptyEmail)
        if (password.isBlank()) return Result.Failure(SignUpError.EmptyPassword)
        if (confirmPassword.isBlank()) return Result.Failure(SignUpError.EmptyConfirmPassword)
        if (password != confirmPassword) return Result.Failure(SignUpError.PasswordMismatch)
        return when (val result = authRepository.signUp(email, password)) {
            Result.Success -> Result.Success
            is Result.Failure ->
                Result.Failure(
                    when (result.error) {
                        AuthRepositoryError.EmailAlreadyInUse -> SignUpError.EmailAlreadyInUse
                        AuthRepositoryError.WeakPassword -> SignUpError.WeakPassword
                        AuthRepositoryError.InvalidEmail -> SignUpError.InvalidEmail
                        AuthRepositoryError.InvalidCredentials,
                        AuthRepositoryError.NetworkError,
                        AuthRepositoryError.Unknown -> SignUpError.SignUpFailed
                    },
                )
        }
    }
}
