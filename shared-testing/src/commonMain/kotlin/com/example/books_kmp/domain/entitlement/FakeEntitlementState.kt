package com.example.books_kmp.domain.entitlement

import com.example.books_kmp.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeEntitlementState : EntitlementState {
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    var setPremiumStatusResult: Result<Unit, EntitlementError> = Result.Success(Unit)
    var lastSetPremium: Boolean? = null
    var setPremiumStatusGate: CompletableDeferred<Unit>? = null

    fun setIsPremium(value: Boolean) {
        _isPremium.value = value
    }

    override suspend fun setPremiumStatus(premium: Boolean): Result<Unit, EntitlementError> {
        lastSetPremium = premium
        setPremiumStatusGate?.await()
        if (setPremiumStatusResult is Result.Success) _isPremium.value = premium
        return setPremiumStatusResult
    }
}
