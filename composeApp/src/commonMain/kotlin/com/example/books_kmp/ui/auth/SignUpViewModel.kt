package com.example.books_kmp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.auth.SignUpError
import com.example.books_kmp.domain.auth.SignUpUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val isLoading: Boolean = false,
    val emailError: SignUpError? = null,
    val passwordError: SignUpError? = null,
    val confirmPasswordError: SignUpError? = null,
)

sealed interface SignUpIntent {
    data class SignUp(
        val email: String,
        val password: String,
        val confirmPassword: String,
    ) : SignUpIntent
}

sealed interface SignUpEffect {
    data class ShowError(val error: SignUpError) : SignUpEffect
}

class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SignUpEffect>()
    val effects: SharedFlow<SignUpEffect> = _effects.asSharedFlow()

    fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.SignUp ->
                launchIfIdle {
                    handleSignUp(
                        intent.email,
                        intent.password,
                        intent.confirmPassword
                    )
                }
        }
    }

    private fun launchIfIdle(block: suspend () -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch { block() }
    }

    private suspend fun handleSignUp(
        email: String,
        password: String,
        confirmPassword: String,
    ) {
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                isLoading = true,
            )
        }

        when (val result = signUpUseCase(email, password, confirmPassword)) {
            is Result.Failure ->
                when (result.error) {
                    SignUpError.EmptyEmail ->
                        _uiState.update { it.copy(isLoading = false, emailError = SignUpError.EmptyEmail) }
                    SignUpError.EmptyPassword ->
                        _uiState.update { it.copy(isLoading = false, passwordError = SignUpError.EmptyPassword) }
                    SignUpError.EmptyConfirmPassword ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                confirmPasswordError = SignUpError.EmptyConfirmPassword
                            )
                        }
                    SignUpError.PasswordMismatch ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                confirmPasswordError = SignUpError.PasswordMismatch
                            )
                        }
                    SignUpError.WeakPassword ->
                        _uiState.update { it.copy(isLoading = false, passwordError = SignUpError.WeakPassword) }
                    SignUpError.InvalidEmail ->
                        _uiState.update { it.copy(isLoading = false, emailError = SignUpError.InvalidEmail) }
                    SignUpError.EmailAlreadyInUse,
                    SignUpError.EmailRateLimitExceeded,
                    SignUpError.SignUpFailed -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effects.emit(SignUpEffect.ShowError(result.error))
                    }
                }
            is Result.Success ->
                // Email confirmation is disabled in Supabase while we lack a custom domain for
                // App Links. Signup immediately creates an active session, so auth state
                // observation navigates the user automatically. Re-enable verificationEmailSent
                // once App Links + custom domain are configured (see docs/store-readiness.md).
                _uiState.update { it.copy(isLoading = false) }
        }
    }
}
