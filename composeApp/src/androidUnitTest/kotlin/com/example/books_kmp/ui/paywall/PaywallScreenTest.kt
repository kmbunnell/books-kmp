package com.example.books_kmp.ui.paywall

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.books_kmp.ui.TestTags
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PaywallScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `free tier label shown when not premium`() {
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = false),
                effects = MutableSharedFlow(),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Current tier: Free").assertIsDisplayed()
    }

    @Test
    fun `premium tier label shown when premium`() {
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = true),
                effects = MutableSharedFlow(),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Current tier: Premium").assertIsDisplayed()
    }

    @Test
    fun `go premium button visible when not premium`() {
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = false),
                effects = MutableSharedFlow(),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Paywall.GoPremiumButton).assertIsDisplayed()
    }

    @Test
    fun `downgrade button visible when premium`() {
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = true),
                effects = MutableSharedFlow(),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Paywall.DowngradeButton).assertIsDisplayed()
    }

    @Test
    fun `go premium button disabled when loading`() {
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = false, isLoading = true),
                effects = MutableSharedFlow(),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Paywall.GoPremiumButton).assertIsNotEnabled()
    }

    @Test
    fun `loading indicator shown when loading`() {
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = false, isLoading = true),
                effects = MutableSharedFlow(),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Paywall.LoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun `snackbar shown on ShowConfirmation effect`() {
        val effects = MutableSharedFlow<PaywallEffect>(extraBufferCapacity = 1)
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = true),
                effects = effects,
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.waitForIdle()
        check(effects.tryEmit(PaywallEffect.ShowConfirmation(isPremium = true)))
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Upgraded to Premium!").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Upgraded to Premium!").assertIsDisplayed()
    }

    @Test
    fun `snackbar shown on ShowError effect`() {
        val effects = MutableSharedFlow<PaywallEffect>(extraBufferCapacity = 1)
        composeTestRule.setContent {
            PaywallScreenContent(
                uiState = PaywallUiState(isPremium = false),
                effects = effects,
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.waitForIdle()
        check(effects.tryEmit(PaywallEffect.ShowError(PaywallError.UpdateFailed)))
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule
                .onAllNodesWithText("Couldn't update your subscription. Please try again.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText("Couldn't update your subscription. Please try again.")
            .assertIsDisplayed()
    }
}
