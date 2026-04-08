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
    data object SignOutFailed : AuthError
    data object SessionExpired : AuthError
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
)

sealed interface AuthIntent {
    data object SignOut : AuthIntent
}

sealed interface AuthEffect {
    data class ShowError(val error: AuthError) : AuthEffect
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
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
                    AuthSessionState.Error -> {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = false, userId = null) }
                        _effects.emit(AuthEffect.ShowError(AuthError.SessionExpired))
                    }
                }
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        viewModelScope.launch {
            when (intent) {
                AuthIntent.SignOut -> handleSignOut()
            }
        }
    }

    private suspend fun handleSignOut() {
        try {
            authRepository.signOut()
        } catch (_: Exception) {
            _effects.emit(AuthEffect.ShowError(AuthError.SignOutFailed))
        }
    }
}
