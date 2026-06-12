package com.example.books_kmp.domain.profile

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Profile
import kotlinx.coroutines.CompletableDeferred

class FakeProfileRepository : ProfileRepository {
    var getProfileResult: Result<Profile, ProfileRepositoryError> =
        Result.Success(Profile(id = "user-1", isPremium = false))
    var getProfileCalled = false

    // Optional gate — tests set this to suspend getProfile until completed,
    // allowing deterministic observation of in-flight consumer state.
    var getProfileGate: CompletableDeferred<Unit>? = null

    override suspend fun getProfile(): Result<Profile, ProfileRepositoryError> {
        getProfileCalled = true
        getProfileGate?.await()
        return getProfileResult
    }
}
