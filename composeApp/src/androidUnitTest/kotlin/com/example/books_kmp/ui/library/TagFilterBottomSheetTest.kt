package com.example.books_kmp.ui.library

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.testing.TEST_INSTANT
import com.example.books_kmp.ui.TestTags
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TagFilterBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tag1 = Tag(id = "t1", name = "Fiction", isDefault = false, updatedAt = TEST_INSTANT)
    private val tag2 = Tag(id = "t2", name = "Sci-Fi", isDefault = false, updatedAt = TEST_INSTANT)

    @Test
    fun `all tags render as chips in sheet`() {
        composeTestRule.setContent {
            TagFilterBottomSheet(
                tags = listOf(tag1, tag2),
                selectedTagIds = emptySet(),
                onTagSelected = {},
                onClearAll = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.filterSheetChip("t1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Library.filterSheetChip("t2")).assertIsDisplayed()
    }

    @Test
    fun `selected chip in selectedTagIds has selected semantic`() {
        composeTestRule.setContent {
            TagFilterBottomSheet(
                tags = listOf(tag1),
                selectedTagIds = setOf("t1"),
                onTagSelected = {},
                onClearAll = {},
                onDismiss = {},
            )
        }
        composeTestRule
            .onNodeWithTag(TestTags.Library.filterSheetChip("t1"))
            .assertIsSelected()
    }

    @Test
    fun `unselected chip not in selectedTagIds has unselected semantic`() {
        composeTestRule.setContent {
            TagFilterBottomSheet(
                tags = listOf(tag1),
                selectedTagIds = emptySet(),
                onTagSelected = {},
                onClearAll = {},
                onDismiss = {},
            )
        }
        composeTestRule
            .onNodeWithTag(TestTags.Library.filterSheetChip("t1"))
            .assertIsNotSelected()
    }

    @Test
    fun `tapping chip calls onTagSelected with correct tag id`() {
        val selected = mutableListOf<String>()
        composeTestRule.setContent {
            TagFilterBottomSheet(
                tags = listOf(tag1, tag2),
                selectedTagIds = emptySet(),
                onTagSelected = { selected.add(it) },
                onClearAll = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.filterSheetChip("t1")).performClick()
        assertEquals("t1", selected.last())
    }

    @Test
    fun `tapping Clear All calls onClearAll`() {
        var clearAllCalled = false
        composeTestRule.setContent {
            TagFilterBottomSheet(
                tags = listOf(tag1),
                selectedTagIds = emptySet(),
                onTagSelected = {},
                onClearAll = { clearAllCalled = true },
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterSheetClearAll).performClick()
        assertTrue(clearAllCalled)
    }

    @Test
    fun `tapping Apply calls onDismiss`() {
        var dismissCalled = false
        composeTestRule.setContent {
            TagFilterBottomSheet(
                tags = listOf(tag1),
                selectedTagIds = emptySet(),
                onTagSelected = {},
                onClearAll = {},
                onDismiss = { dismissCalled = true },
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterSheetApply).performClick()
        assertTrue(dismissCalled)
    }

    @Test
    fun `empty tag list shows no-tags message`() {
        composeTestRule.setContent {
            TagFilterBottomSheet(
                tags = emptyList(),
                selectedTagIds = emptySet(),
                onTagSelected = {},
                onClearAll = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Library.FilterSheetNoTags).assertIsDisplayed()
    }
}
