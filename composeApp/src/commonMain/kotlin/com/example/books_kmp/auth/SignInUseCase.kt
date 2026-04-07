package com.example.books_kmp.auth

import com.example.books_kmp.Result

class SignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<SignInError> {
        if (email.isBlank()) return Result.Failure(SignInError.EmptyEmail)
        if (password.isBlank()) return Result.Failure(SignInError.EmptyPassword)
        return try {
            authRepository.signIn(email, password)
            Result.Success
        } catch (_: Exception) {
            Result.Failure(SignInError.InvalidCredentials)
        }
    }
}
