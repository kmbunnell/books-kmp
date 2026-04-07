package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.Result
import com.example.books_kmp.auth.AuthRepository
import com.example.books_kmp.auth.AuthSessionState
import com.example.books_kmp.auth.SignInError
import com.example.books_kmp.auth.SignInUseCase
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
    data object InvalidCredentials : AuthError
    data object SignUpFailed : AuthError
    data object SignOutFailed : AuthError
    data object SessionExpired : AuthError
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

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val signInUseCase: SignInUseCase,
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
                is AuthIntent.SignUpWithEmail -> handleSignUp(intent.email, intent.password)
                is AuthIntent.SignInWithEmail -> handleSignIn(intent.email, intent.password)
                AuthIntent.SignOut -> handleSignOut()
            }
        }
    }

    private suspend fun handleSignUp(
        email: String,
        password: String,
    ) {
        try {
            authRepository.signUp(email, password)
        } catch (_: Exception) {
            _effects.emit(AuthEffect.ShowError(AuthError.SignUpFailed))
        }
    }

    private suspend fun handleSignIn(
        email: String,
        password: String,
    ) {
        _uiState.update { it.copy(emailError = null, passwordError = null, isLoading = true) }

        when (val result = signInUseCase(email, password)) {
            is Result.Failure -> when (result.error) {
                SignInError.EmptyEmail ->
                    _uiState.update { it.copy(isLoading = false, emailError = AuthError.EmailRequired) }
                SignInError.EmptyPassword ->
                    _uiState.update { it.copy(isLoading = false, passwordError = AuthError.PasswordRequired) }
                SignInError.InvalidCredentials -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.emit(AuthEffect.ShowError(AuthError.InvalidCredentials))
                }
            }
            Result.Success -> {
                // Session flow will update isLoading and isAuthenticated
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
