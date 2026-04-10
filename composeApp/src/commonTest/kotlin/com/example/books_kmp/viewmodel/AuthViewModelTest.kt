package com.example.books_kmp.viewmodel

import app.cash.turbine.test
import com.example.books_kmp.auth.FakeAuthRepository
import com.example.books_kmp.domain.auth.AuthSessionState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is NotAuthenticated when repository emits not authenticated`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo)

            val state = viewModel.uiState.value
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.userId)
        }

    @Test
    fun `state transitions to Authenticated when session emits Authenticated`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo)

            fakeRepo.sessionFlow.emit(AuthSessionState.Authenticated("user-123"))

            val state = viewModel.uiState.value
            assertTrue(state.isAuthenticated)
        }

    @Test
    fun `state transitions to NotAuthenticated after sign-out intent`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.Authenticated("user-123"))
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.onIntent(AuthIntent.SignOut)
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)

            val state = viewModel.uiState.value
            assertFalse(state.isAuthenticated)
            assertNull(state.userId)
        }

    @Test
    fun `ShowError effect with SignOutFailed emitted when signOut throws`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.Authenticated("user-123"))
            fakeRepo.signOutException = Exception("network error")
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.effects.test {
                viewModel.onIntent(AuthIntent.SignOut)
                val effect = awaitItem()
                assertIs<AuthEffect.ShowError>(effect)
                assertIs<AuthError.SignOutFailed>(effect.error)
            }
        }

    @Test
    fun `ShowError effect with SessionExpired emitted when session status emits Error`() =
        runTest {
            fakeRepo.sessionFlow.emit(AuthSessionState.NotAuthenticated)
            val viewModel = AuthViewModel(fakeRepo)

            viewModel.effects.test {
                fakeRepo.sessionFlow.emit(AuthSessionState.Error)
                val effect = awaitItem()
                assertIs<AuthEffect.ShowError>(effect)
                assertIs<AuthError.SessionExpired>(effect.error)
            }
        }
}
