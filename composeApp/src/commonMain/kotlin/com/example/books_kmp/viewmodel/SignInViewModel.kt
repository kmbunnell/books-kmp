package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.auth.SignInError
import com.example.books_kmp.domain.auth.SignInUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val isLoading: Boolean = false,
    val emailError: SignInError? = null,
    val passwordError: SignInError? = null,
)

sealed interface SignInIntent {
    data class SignIn(val email: String, val password: String) : SignInIntent
}

sealed interface SignInEffect {
    data class ShowError(val error: SignInError) : SignInEffect
}

class SignInViewModel(
    private val signInUseCase: SignInUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SignInEffect>()
    val effects: SharedFlow<SignInEffect> = _effects.asSharedFlow()

    fun onIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.SignIn -> viewModelScope.launch { handleSignIn(intent.email, intent.password) }
        }
    }

    private suspend fun handleSignIn(
        email: String,
        password: String,
    ) {
        _uiState.update { it.copy(emailError = null, passwordError = null, isLoading = true) }

        when (val result = signInUseCase(email, password)) {
            is Result.Failure ->
                when (result.error) {
                    SignInError.EmptyEmail ->
                        _uiState.update { it.copy(isLoading = false, emailError = SignInError.EmptyEmail) }
                    SignInError.EmptyPassword ->
                        _uiState.update { it.copy(isLoading = false, passwordError = SignInError.EmptyPassword) }
                    SignInError.InvalidCredentials -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effects.emit(SignInEffect.ShowError(SignInError.InvalidCredentials))
                    }
                    SignInError.SignInFailed -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effects.emit(SignInEffect.ShowError(SignInError.SignInFailed))
                    }
                }
            is Result.Success ->
                _uiState.update { it.copy(isLoading = false) }
        }
    }
}
