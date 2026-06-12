package com.example.books_kmp.domain.entitlement

import com.example.books_kmp.domain.Result

interface EntitlementRepository {
    suspend fun fetchIsPremium(userId: String): Result<Boolean, EntitlementError>

    suspend fun setPremiumStatus(
        userId: String,
        premium: Boolean
    ): Result<Unit, EntitlementError>
}
