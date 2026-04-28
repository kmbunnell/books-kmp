package com.example.books_kmp.ui.tags

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.TagFormMode
import com.example.books_kmp.viewmodel.TagFormState
import com.example.books_kmp.viewmodel.TagManagementError
import com.example.books_kmp.viewmodel.TagManagementIntent
import com.example.books_kmp.viewmodel.TagManagementUiState
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TagManagementScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultTag = Tag(id = "d1", name = "Fiction", isDefault = true)
    private val customTag = Tag(id = "c1", name = "Favorites", isDefault = false)

    @Test
    fun `default section shows tag names with no options button`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(defaultTags = listOf(defaultTag)),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Fiction").assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.TagManagement.optionsButton("d1")).assertDoesNotExist()
    }

    @Test
    fun `custom section shows tag name and options button per tag`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(customTags = listOf(customTag)),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.TagManagement.optionsButton("c1")).assertIsDisplayed()
    }

    @Test
    fun `add tag button visible below custom section`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.AddTagButton).assertIsDisplayed()
    }

    @Test
    fun `clicking add tag button dispatches OpenCreateForm`() {
        val dispatched = mutableListOf<TagManagementIntent>()
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.AddTagButton).performClick()
        assertEquals(TagManagementIntent.OpenCreateForm, dispatched.last())
    }

    @Test
    fun `clicking rename in options menu dispatches OpenEditForm with correct tag`() {
        val dispatched = mutableListOf<TagManagementIntent>()
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(customTags = listOf(customTag)),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.optionsButton("c1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.TagManagement.renameMenuItem("c1")).performClick()
        assertEquals(TagManagementIntent.OpenEditForm(customTag), dispatched.last())
    }

    @Test
    fun `clicking delete in options menu dispatches RequestDeleteTag with correct tag`() {
        val dispatched = mutableListOf<TagManagementIntent>()
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(customTags = listOf(customTag)),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.optionsButton("c1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.TagManagement.deleteMenuItem("c1")).performClick()
        assertEquals(TagManagementIntent.RequestDeleteTag(customTag), dispatched.last())
    }

    @Test
    fun `form sheet visible when tagFormState non-null`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState = TagFormState(mode = TagFormMode.Create),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.FormNameField).assertIsDisplayed()
    }

    @Test
    fun `form sheet title is New Tag in Create mode`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState = TagFormState(mode = TagFormMode.Create),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("New Tag").assertIsDisplayed()
    }

    @Test
    fun `form sheet title is Edit Tag in Edit mode`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState =
                            TagFormState(
                                mode = TagFormMode.Edit(customTag),
                                draftName = customTag.name,
                            ),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Edit Tag").assertIsDisplayed()
    }

    @Test
    fun `save button disabled when draft name is blank`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState = TagFormState(mode = TagFormMode.Create, draftName = ""),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.FormSaveButton).assertIsNotEnabled()
    }

    @Test
    fun `save button enabled when draft name has text`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState = TagFormState(mode = TagFormMode.Create, draftName = "New"),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.FormSaveButton).assertIsEnabled()
    }

    @Test
    fun `empty name error shown as supporting text`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState =
                            TagFormState(
                                mode = TagFormMode.Create,
                                nameError = TagManagementError.EmptyName,
                            ),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Tag name cannot be empty").assertIsDisplayed()
    }

    @Test
    fun `duplicate name error shown as supporting text`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState =
                            TagFormState(
                                mode = TagFormMode.Create,
                                nameError = TagManagementError.DuplicateName,
                            ),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("A tag with this name already exists").assertIsDisplayed()
    }

    @Test
    fun `delete dialog shown when pendingDeleteTag non-null`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        pendingDeleteTag = customTag,
                        pendingDeleteBookCount = 3,
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.DeleteDialogConfirm).assertIsDisplayed()
    }

    @Test
    fun `delete dialog body contains tag name and book count`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        pendingDeleteTag = customTag,
                        pendingDeleteBookCount = 3,
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Delete \"Favorites\"? It is used by 3 book(s).").assertIsDisplayed()
    }

    @Test
    fun `confirm delete dispatches ConfirmDeleteTag`() {
        val dispatched = mutableListOf<TagManagementIntent>()
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        pendingDeleteTag = customTag,
                        pendingDeleteBookCount = 0,
                    ),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.DeleteDialogConfirm).performClick()
        assertEquals(TagManagementIntent.ConfirmDeleteTag, dispatched.last())
    }

    @Test
    fun `cancel delete dispatches CancelDelete`() {
        val dispatched = mutableListOf<TagManagementIntent>()
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        pendingDeleteTag = customTag,
                        pendingDeleteBookCount = 0,
                    ),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.DeleteDialogCancel).performClick()
        assertEquals(TagManagementIntent.CancelDelete, dispatched.last())
    }

    @Test
    fun `loading indicator shown when isLoading true`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(isLoading = true),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.LoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun `form shows character count in supporting text`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState = TagFormState(mode = TagFormMode.Create, draftName = "Hi"),
                    ),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("2/20").assertIsDisplayed()
    }

    @Test
    fun `typing beyond max length does not dispatch UpdateFormName`() {
        val dispatched = mutableListOf<TagManagementIntent>()
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState =
                    TagManagementUiState(
                        tagFormState =
                            TagFormState(
                                mode = TagFormMode.Create,
                                draftName = "12345678901234567890",
                            ),
                    ),
                onIntent = { dispatched.add(it) },
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TagManagement.FormNameField).performTextInput("x")
        assertEquals(0, dispatched.size)
    }

    @Test
    fun `default section collapses and expands on header click`() {
        composeTestRule.setContent {
            TagManagementScreenContent(
                uiState = TagManagementUiState(defaultTags = listOf(defaultTag)),
                onIntent = {},
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithText("Fiction").assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.TagManagement.DefaultSectionHeader).performClick()
        composeTestRule.onNodeWithText("Fiction").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.TagManagement.DefaultSectionHeader).performClick()
        composeTestRule.onNodeWithText("Fiction").assertDoesNotExist()
    }
}
