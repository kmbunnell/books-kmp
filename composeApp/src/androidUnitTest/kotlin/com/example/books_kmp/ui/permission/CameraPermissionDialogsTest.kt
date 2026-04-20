package com.example.books_kmp.ui.permission

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.books_kmp.ui.TestTags
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CameraPermissionDialogsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `rationale dialog shows title`() {
        composeTestRule.setContent {
            CameraRationaleDialog(onRetry = {}, onDismiss = {})
        }
        composeTestRule.onNodeWithTag(TestTags.CameraPermission.RationaleDialog).assertIsDisplayed()
    }

    @Test
    fun `rationale dialog retry button invokes onRetry`() {
        var retried = false
        composeTestRule.setContent {
            CameraRationaleDialog(onRetry = { retried = true }, onDismiss = {})
        }
        composeTestRule.onNodeWithTag(TestTags.CameraPermission.RetryButton).performClick()
        assertTrue(retried)
    }

    @Test
    fun `rationale dialog not-now button invokes onDismiss`() {
        var dismissed = false
        composeTestRule.setContent {
            CameraRationaleDialog(onRetry = {}, onDismiss = { dismissed = true })
        }
        composeTestRule.onNodeWithTag(TestTags.CameraPermission.DismissButton).performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `settings dialog shows permanently denied content`() {
        composeTestRule.setContent {
            CameraSettingsDialog(onOpenSettings = {}, onDismiss = {})
        }
        composeTestRule.onNodeWithTag(TestTags.CameraPermission.SettingsDialog).assertIsDisplayed()
    }

    @Test
    fun `settings dialog open settings button invokes onOpenSettings`() {
        var opened = false
        composeTestRule.setContent {
            CameraSettingsDialog(onOpenSettings = { opened = true }, onDismiss = {})
        }
        composeTestRule.onNodeWithTag(TestTags.CameraPermission.OpenSettingsButton).performClick()
        assertTrue(opened)
    }

    @Test
    fun `settings dialog not-now button invokes onDismiss`() {
        var dismissed = false
        composeTestRule.setContent {
            CameraSettingsDialog(onOpenSettings = {}, onDismiss = { dismissed = true })
        }
        composeTestRule.onNodeWithTag(TestTags.CameraPermission.DismissButton).performClick()
        assertTrue(dismissed)
    }
}
