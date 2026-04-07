package com.example.books_kmp.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
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
        password: String
    ) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }
}
