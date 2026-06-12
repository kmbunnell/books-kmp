package com.example.books_kmp.domain.entitlement

import com.example.books_kmp.domain.Result

class FakeEntitlementRepository : EntitlementRepository {
    var fetchIsPremiumResult: Result<Boolean, EntitlementError> = Result.Success(false)
    var setPremiumStatusResult: Result<Unit, EntitlementError> = Result.Success(Unit)

    var lastFetchUserId: String? = null
    var lastSetUserId: String? = null
    var lastSetPremium: Boolean? = null

    override suspend fun fetchIsPremium(userId: String): Result<Boolean, EntitlementError> {
        lastFetchUserId = userId
        return fetchIsPremiumResult
    }

    override suspend fun setPremiumStatus(
        userId: String,
        premium: Boolean
    ): Result<Unit, EntitlementError> {
        lastSetUserId = userId
        lastSetPremium = premium
        return setPremiumStatusResult
    }
}
