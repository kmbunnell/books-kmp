package com.example.books_kmp.auth

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.auth.AuthRepository
import com.example.books_kmp.domain.auth.AuthRepositoryError
import com.example.books_kmp.domain.auth.AuthSessionState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupabaseAuthRepository(private val supabase: SupabaseClient) : AuthRepository {
    override val sessionStatus: Flow<AuthSessionState> =
        supabase.auth.sessionStatus.map { status ->
            when (status) {
                SessionStatus.Initializing -> AuthSessionState.Loading
                is SessionStatus.Authenticated -> AuthSessionState.Authenticated(status.session.user?.id ?: "")
                is SessionStatus.NotAuthenticated -> AuthSessionState.NotAuthenticated
                is SessionStatus.RefreshFailure -> AuthSessionState.Error
            }
        }

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            Result.Failure(
                when (e.error) {
                    "user_already_exists" -> AuthRepositoryError.EmailAlreadyInUse
                    "weak_password" -> AuthRepositoryError.WeakPassword
                    "over_email_send_rate_limit" -> AuthRepositoryError.EmailRateLimitExceeded
                    "validation_failed" -> AuthRepositoryError.InvalidEmail
                    else -> {
                        println(
                            "SupabaseAuthRepository.signUp unknown RestException: " +
                                "error=${e.error}, message=${e.message}",
                        )
                        AuthRepositoryError.Unknown
                    }
                },
            )
        } catch (e: Exception) {
            println("SupabaseAuthRepository.signUp failed: ${e::class.simpleName}: ${e.message}")
            Result.Failure(AuthRepositoryError.NetworkError)
        }
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<Unit, AuthRepositoryError> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            Result.Failure(
                when (e.error) {
                    "invalid_credentials" -> AuthRepositoryError.InvalidCredentials
                    "email_not_confirmed" -> AuthRepositoryError.EmailNotVerified
                    else -> {
                        println(
                            "SupabaseAuthRepository.signIn unknown RestException: " +
                                "error=${e.error}, message=${e.message}",
                        )
                        AuthRepositoryError.Unknown
                    }
                },
            )
        } catch (e: Exception) {
            println("SupabaseAuthRepository.signIn failed: ${e::class.simpleName}: ${e.message}")
            Result.Failure(AuthRepositoryError.NetworkError)
        }
    }

    override suspend fun signOut(): Result<Unit, AuthRepositoryError> {
        return try {
            supabase.auth.signOut()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("SupabaseAuthRepository.signOut failed: ${e::class.simpleName}: ${e.message}")
            Result.Failure(AuthRepositoryError.NetworkError)
        }
    }
}
