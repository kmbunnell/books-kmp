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
                    "validation_failed" -> AuthRepositoryError.InvalidEmail
                    else -> AuthRepositoryError.Unknown
                },
            )
        } catch (_: Exception) {
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
                    else -> AuthRepositoryError.Unknown
                },
            )
        } catch (_: Exception) {
            Result.Failure(AuthRepositoryError.NetworkError)
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }
}
