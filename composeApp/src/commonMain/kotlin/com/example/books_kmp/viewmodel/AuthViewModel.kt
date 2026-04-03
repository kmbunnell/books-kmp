package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.auth.AuthRepository
import com.example.books_kmp.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
)

sealed interface AuthIntent {
    data class SignUpWithEmail(val email: String, val password: String) : AuthIntent

    data class SignInWithEmail(val email: String, val password: String) : AuthIntent

    data object SignOut : AuthIntent
}

sealed interface AuthEffect {
    data class ShowError(val message: String) : AuthEffect
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { sessionState ->
                _uiState.update { current ->
                    when (sessionState) {
                        AuthSessionState.Loading ->
                            current.copy(
                                isLoading = true,
                                isAuthenticated = false,
                                userId = null
                            )
                        is AuthSessionState.Authenticated ->
                            current.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                userId = sessionState.userId
                            )
                        AuthSessionState.NotAuthenticated ->
                            current.copy(
                                isLoading = false,
                                isAuthenticated = false,
                                userId = null
                            )
                        is AuthSessionState.Error ->
                            current.copy(
                                isLoading = false,
                                isAuthenticated = false,
                                userId = null
                            )
                    }
                }
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        viewModelScope.launch {
            when (intent) {
                is AuthIntent.SignUpWithEmail -> handleSignUp(intent.email, intent.password)
                is AuthIntent.SignInWithEmail -> handleSignIn(intent.email, intent.password)
                AuthIntent.SignOut -> handleSignOut()
            }
        }
    }

    private suspend fun handleSignUp(
        email: String,
        password: String
    ) {
        try {
            authRepository.signUp(email, password)
        } catch (e: Exception) {
            _effects.emit(AuthEffect.ShowError(e.message ?: "Sign up failed"))
        }
    }

    private suspend fun handleSignIn(
        email: String,
        password: String
    ) {
        try {
            authRepository.signIn(email, password)
        } catch (e: Exception) {
            _effects.emit(AuthEffect.ShowError(e.message ?: "Sign in failed"))
        }
    }

    private suspend fun handleSignOut() {
        try {
            authRepository.signOut()
        } catch (e: Exception) {
            _effects.emit(AuthEffect.ShowError(e.message ?: "Sign out failed"))
        }
    }
}
