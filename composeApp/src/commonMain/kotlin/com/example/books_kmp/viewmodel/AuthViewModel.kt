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

sealed interface AuthError {
    data object EmailRequired : AuthError
    data object PasswordRequired : AuthError
    data class SignInFailed(val cause: String?) : AuthError
    data class SignUpFailed(val cause: String?) : AuthError
    data class SignOutFailed(val cause: String?) : AuthError
    data class SessionError(val cause: String?) : AuthError
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val emailError: AuthError? = null,
    val passwordError: AuthError? = null,
)

sealed interface AuthIntent {
    data class SignUpWithEmail(val email: String, val password: String) : AuthIntent

    data class SignInWithEmail(val email: String, val password: String) : AuthIntent

    data object SignOut : AuthIntent
}

sealed interface AuthEffect {
    data class ShowError(val error: AuthError) : AuthEffect
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { sessionState ->
                when (sessionState) {
                    AuthSessionState.Loading ->
                        _uiState.update { it.copy(isLoading = true, isAuthenticated = false, userId = null) }
                    is AuthSessionState.Authenticated ->
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true, userId = sessionState.userId) }
                    AuthSessionState.NotAuthenticated ->
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = false, userId = null) }
                    is AuthSessionState.Error -> {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = false, userId = null) }
                        _effects.emit(AuthEffect.ShowError(AuthError.SessionError(sessionState.message)))
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
            _effects.emit(AuthEffect.ShowError(AuthError.SignUpFailed(e.message)))
        }
    }

    private suspend fun handleSignIn(
        email: String,
        password: String,
    ) {
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = AuthError.EmailRequired, passwordError = null) }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(emailError = null, passwordError = AuthError.PasswordRequired) }
            return
        }
        _uiState.update { it.copy(emailError = null, passwordError = null) }
        try {
            authRepository.signIn(email, password)
        } catch (e: Exception) {
            _effects.emit(AuthEffect.ShowError(AuthError.SignInFailed(e.message)))
        }
    }

    private suspend fun handleSignOut() {
        try {
            authRepository.signOut()
        } catch (e: Exception) {
            _effects.emit(AuthEffect.ShowError(AuthError.SignOutFailed(e.message)))
        }
    }
}
