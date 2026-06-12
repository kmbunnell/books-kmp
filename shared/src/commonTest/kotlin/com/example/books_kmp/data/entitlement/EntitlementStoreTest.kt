package com.example.books_kmp.data.entitlement

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.auth.AuthSessionState
import com.example.books_kmp.domain.auth.FakeAuthRepository
import com.example.books_kmp.domain.entitlement.EntitlementError
import com.example.books_kmp.domain.entitlement.FakeEntitlementRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class EntitlementStoreTest {
    private val fakeAuth = FakeAuthRepository()
    private val fakeRepo = FakeEntitlementRepository()

    // UnconfinedTestDispatcher makes the init collector start eagerly, so emissions
    // are processed before the next line without needing advanceUntilIdle().
    private fun buildStore(scope: CoroutineScope) = EntitlementStore(fakeRepo, fakeAuth, scope)

    // --- session state → isPremium ---

    @Test
    fun `Authenticated sets isPremium from repository on success`() =
        runTest {
            fakeRepo.fetchIsPremiumResult = Result.Success(true)
            val store = buildStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            fakeAuth.sessionFlow.emit(AuthSessionState.Authenticated("u1"))

            assertEquals(true, store.isPremium.value)
            assertEquals("u1", fakeRepo.lastFetchUserId)
        }

    @Test
    fun `Authenticated defaults isPremium to false when fetch fails`() =
        runTest {
            fakeRepo.fetchIsPremiumResult = Result.Failure(EntitlementError.NetworkError)
            val store = buildStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            fakeAuth.sessionFlow.emit(AuthSessionState.Authenticated("u1"))

            assertEquals(false, store.isPremium.value)
        }

    @Test
    fun `NotAuthenticated resets isPremium to false`() =
        runTest {
            fakeRepo.fetchIsPremiumResult = Result.Success(true)
            val store = buildStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            fakeAuth.sessionFlow.emit(AuthSessionState.Authenticated("u1"))
            assertEquals(true, store.isPremium.value)

            fakeAuth.sessionFlow.emit(AuthSessionState.NotAuthenticated)

            assertEquals(false, store.isPremium.value)
        }

    @Test
    fun `Error state resets isPremium to false`() =
        runTest {
            fakeRepo.fetchIsPremiumResult = Result.Success(true)
            val store = buildStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            fakeAuth.sessionFlow.emit(AuthSessionState.Authenticated("u1"))
            assertEquals(true, store.isPremium.value)

            fakeAuth.sessionFlow.emit(AuthSessionState.Error)

            assertEquals(false, store.isPremium.value)
        }

    // --- setPremiumStatus ---

    @Test
    fun `setPremiumStatus returns NotAuthenticated when no user is logged in`() =
        runTest {
            val store = buildStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            val result = store.setPremiumStatus(true)

            assertIs<Result.Failure<EntitlementError>>(result)
            assertEquals(EntitlementError.NotAuthenticated, result.error)
        }

    @Test
    fun `setPremiumStatus updates isPremium on success`() =
        runTest {
            fakeRepo.fetchIsPremiumResult = Result.Success(false)
            val store = buildStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            fakeAuth.sessionFlow.emit(AuthSessionState.Authenticated("u1"))

            val result = store.setPremiumStatus(true)

            assertIs<Result.Success<Unit>>(result)
            assertEquals(true, store.isPremium.value)
            assertEquals("u1", fakeRepo.lastSetUserId)
            assertEquals(true, fakeRepo.lastSetPremium)
        }

    @Test
    fun `setPremiumStatus does not update isPremium on failure`() =
        runTest {
            fakeRepo.fetchIsPremiumResult = Result.Success(false)
            fakeRepo.setPremiumStatusResult = Result.Failure(EntitlementError.NetworkError)
            val store = buildStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            fakeAuth.sessionFlow.emit(AuthSessionState.Authenticated("u1"))

            val result = store.setPremiumStatus(true)

            assertIs<Result.Failure<EntitlementError>>(result)
            assertEquals(false, store.isPremium.value)
        }
}
