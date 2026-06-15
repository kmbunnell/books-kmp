package com.example.books_kmp.ui.paywall

import app.cash.turbine.test
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.EntitlementError
import com.example.books_kmp.domain.entitlement.FakeEntitlementState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var entitlement: FakeEntitlementState
    private lateinit var vm: PaywallViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        entitlement = FakeEntitlementState()
        vm = PaywallViewModel(entitlement)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GoPremium sets isLoading true while in-flight`() =
        runTest {
            entitlement.setPremiumStatusGate = CompletableDeferred()
            vm.onIntent(PaywallIntent.GoPremium)
            assertTrue(vm.uiState.value.isLoading)
        }

    @Test
    fun `GoPremium clears isLoading and emits ShowConfirmation isPremium true on success`() =
        runTest {
            vm.effects.test {
                vm.onIntent(PaywallIntent.GoPremium)
                val effect = awaitItem()
                assertIs<PaywallEffect.ShowConfirmation>(effect)
                assertTrue(effect.isPremium)
            }
            assertFalse(vm.uiState.value.isLoading)
            assertEquals(true, entitlement.lastSetPremium)
        }

    @Test
    fun `GoPremium clears isLoading and emits ShowError on failure`() =
        runTest {
            entitlement.setPremiumStatusResult = Result.Failure(EntitlementError.NetworkError)
            vm.effects.test {
                vm.onIntent(PaywallIntent.GoPremium)
                val effect = awaitItem()
                assertIs<PaywallEffect.ShowError>(effect)
            }
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `GoPremium while isLoading does nothing`() =
        runTest {
            entitlement.setPremiumStatusGate = CompletableDeferred()
            vm.onIntent(PaywallIntent.GoPremium)
            entitlement.lastSetPremium = null
            vm.onIntent(PaywallIntent.GoPremium)
            assertEquals(null, entitlement.lastSetPremium)
        }

    @Test
    fun `Downgrade sets isLoading true while in-flight`() =
        runTest {
            entitlement.setIsPremium(true)
            entitlement.setPremiumStatusGate = CompletableDeferred()
            vm.onIntent(PaywallIntent.Downgrade)
            assertTrue(vm.uiState.value.isLoading)
        }

    @Test
    fun `Downgrade clears isLoading and emits ShowConfirmation isPremium false on success`() =
        runTest {
            entitlement.setIsPremium(true)
            vm.effects.test {
                vm.onIntent(PaywallIntent.Downgrade)
                val effect = awaitItem()
                assertIs<PaywallEffect.ShowConfirmation>(effect)
                assertFalse(effect.isPremium)
            }
            assertFalse(vm.uiState.value.isLoading)
            assertEquals(false, entitlement.lastSetPremium)
        }

    @Test
    fun `Downgrade clears isLoading and emits ShowError on failure`() =
        runTest {
            entitlement.setIsPremium(true)
            entitlement.setPremiumStatusResult = Result.Failure(EntitlementError.NetworkError)
            vm.effects.test {
                vm.onIntent(PaywallIntent.Downgrade)
                val effect = awaitItem()
                assertIs<PaywallEffect.ShowError>(effect)
            }
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `Downgrade while isLoading does nothing`() =
        runTest {
            entitlement.setIsPremium(true)
            entitlement.setPremiumStatusGate = CompletableDeferred()
            vm.onIntent(PaywallIntent.Downgrade)
            entitlement.lastSetPremium = null
            vm.onIntent(PaywallIntent.Downgrade)
            assertEquals(null, entitlement.lastSetPremium)
        }

    @Test
    fun `isPremium from EntitlementState flow is reflected in uiState`() =
        runTest {
            assertFalse(vm.uiState.value.isPremium)
            entitlement.setIsPremium(true)
            assertTrue(vm.uiState.value.isPremium)
        }
}
