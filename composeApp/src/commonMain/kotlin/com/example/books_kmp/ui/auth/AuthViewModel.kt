package com.example.books_kmp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.auth.AuthRepository
import com.example.books_kmp.domain.auth.AuthSessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
)

class AuthViewModel(authRepository: AuthRepository) : ViewModel() {
    val uiState: StateFlow<AuthUiState> =
        authRepository.sessionStatus
            .map { sessionState ->
                when (sessionState) {
                    AuthSessionState.Loading -> AuthUiState(isLoading = true)
                    is AuthSessionState.Authenticated ->
                        AuthUiState(isAuthenticated = true, userId = sessionState.userId)
                    AuthSessionState.NotAuthenticated -> AuthUiState()
                    AuthSessionState.Error -> AuthUiState()
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AuthUiState(isLoading = true))
}
