package com.example.books_kmp.domain.entitlement

import com.example.books_kmp.domain.Result
import kotlinx.coroutines.flow.StateFlow

interface EntitlementState {
    val isPremium: StateFlow<Boolean>

    suspend fun setPremiumStatus(premium: Boolean): Result<Unit, EntitlementError>
}
