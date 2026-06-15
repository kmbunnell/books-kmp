package com.example.books_kmp.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.EntitlementState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaywallUiState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface PaywallError {
    data object UpdateFailed : PaywallError
}

sealed interface PaywallIntent {
    data object GoPremium : PaywallIntent

    data object Downgrade : PaywallIntent
}

sealed interface PaywallEffect {
    data class ShowConfirmation(val isPremium: Boolean) : PaywallEffect

    data class ShowError(val error: PaywallError) : PaywallEffect
}

class PaywallViewModel(
    private val entitlementState: EntitlementState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PaywallEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            entitlementState.isPremium.collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun onIntent(intent: PaywallIntent) {
        when (intent) {
            PaywallIntent.GoPremium -> handleSetPremium(true)
            PaywallIntent.Downgrade -> handleSetPremium(false)
        }
    }

    private fun handleSetPremium(premium: Boolean) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (entitlementState.setPremiumStatus(premium)) {
                is Result.Success -> _effects.send(PaywallEffect.ShowConfirmation(premium))
                is Result.Failure -> _effects.send(PaywallEffect.ShowError(PaywallError.UpdateFailed))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
