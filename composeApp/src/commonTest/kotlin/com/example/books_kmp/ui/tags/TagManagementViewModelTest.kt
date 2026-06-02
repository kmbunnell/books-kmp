package com.example.books_kmp.ui.tags

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.FakeTagRepository
import com.example.books_kmp.domain.tags.TagError
import com.example.books_kmp.domain.tags.TagRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TagManagementViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FakeTagRepository
    private lateinit var vm: TagManagementViewModel

    private val defaultTag = Tag(id = "d1", name = "Read", isDefault = true)
    private val customTag1 = Tag(id = "c1", name = "Favorites", isDefault = false)
    private val customTag2 = Tag(id = "c2", name = "To Read", isDefault = false)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeTagRepository()
        repo.seedTags(defaultTag, customTag1, customTag2)
        vm = TagManagementViewModel(repo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads tags and splits into defaultTags and customTags`() =
        runTest {
            val state = vm.uiState.value
            assertEquals(listOf(defaultTag), state.defaultTags)
            assertEquals(listOf(customTag1, customTag2), state.customTags)
            assertNull(state.error)
        }

    @Test
    fun `init on network error sets error in UiState`() =
        runTest {
            val errorRepo = failingGetTagsRepo()
            val errorVm = TagManagementViewModel(errorRepo)
            val state = errorVm.uiState.value
            assertIs<TagManagementError.NetworkError>(state.error)
        }

    @Test
    fun `OpenCreateForm sets tagFormState with Create mode and empty draftName`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            val form = vm.uiState.value.tagFormState
            assertNotNull(form)
            assertIs<TagFormMode.Create>(form.mode)
            assertEquals("", form.draftName)
        }

    @Test
    fun `OpenEditForm on custom tag sets tagFormState with Edit mode and tag name as draftName`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenEditForm(customTag1))
            val form = vm.uiState.value.tagFormState
            assertNotNull(form)
            val mode = assertIs<TagFormMode.Edit>(form.mode)
            assertEquals(customTag1, mode.tag)
            assertEquals(customTag1.name, form.draftName)
        }

    @Test
    fun `OpenEditForm on default tag produces zero state changes`() =
        runTest {
            val before = vm.uiState.value
            vm.onIntent(TagManagementIntent.OpenEditForm(defaultTag))
            val after = vm.uiState.value
            assertEquals(before, after)
        }

    @Test
    fun `UpdateFormName updates draftName`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            vm.onIntent(TagManagementIntent.UpdateFormName("New Name"))
            assertEquals("New Name", vm.uiState.value.tagFormState?.draftName)
        }

    @Test
    fun `UpdateFormName clears existing nameError`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            vm.onIntent(TagManagementIntent.SubmitForm) // triggers EmptyName
            assertIs<TagManagementError.EmptyName>(vm.uiState.value.tagFormState?.nameError)
            vm.onIntent(TagManagementIntent.UpdateFormName("something"))
            assertNull(vm.uiState.value.tagFormState?.nameError)
        }

    @Test
    fun `SubmitForm with blank name sets nameError to EmptyName and leaves tagFormState non-null`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            vm.onIntent(TagManagementIntent.SubmitForm)
            val form = vm.uiState.value.tagFormState
            assertNotNull(form)
            assertIs<TagManagementError.EmptyName>(form.nameError)
        }

    @Test
    fun `SubmitForm with whitespace-only name sets nameError to EmptyName`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            vm.onIntent(TagManagementIntent.UpdateFormName("   "))
            vm.onIntent(TagManagementIntent.SubmitForm)
            assertIs<TagManagementError.EmptyName>(vm.uiState.value.tagFormState?.nameError)
        }

    @Test
    fun `SubmitForm Create mode duplicate name sets DuplicateName error and keeps form open`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            vm.onIntent(TagManagementIntent.UpdateFormName(customTag1.name))
            vm.onIntent(TagManagementIntent.SubmitForm)
            val form = vm.uiState.value.tagFormState
            assertNotNull(form)
            assertIs<TagManagementError.DuplicateName>(form.nameError)
        }

    @Test
    fun `SubmitForm Edit mode duplicate name sets DuplicateName error and keeps form open`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenEditForm(customTag1))
            vm.onIntent(TagManagementIntent.UpdateFormName(customTag2.name))
            vm.onIntent(TagManagementIntent.SubmitForm)
            val form = vm.uiState.value.tagFormState
            assertNotNull(form)
            assertIs<TagManagementError.DuplicateName>(form.nameError)
        }

    @Test
    fun `SubmitForm in Create mode with unique name clears tagFormState and refreshes lists`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            vm.onIntent(TagManagementIntent.UpdateFormName("Brand New Tag"))
            vm.onIntent(TagManagementIntent.SubmitForm)
            val state = vm.uiState.value
            assertNull(state.tagFormState)
            assertTrue(state.customTags.any { it.name == "Brand New Tag" })
        }

    @Test
    fun `SubmitForm in Edit mode with unique name clears tagFormState and refreshes lists`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenEditForm(customTag1))
            vm.onIntent(TagManagementIntent.UpdateFormName("Renamed Tag"))
            vm.onIntent(TagManagementIntent.SubmitForm)
            val state = vm.uiState.value
            assertNull(state.tagFormState)
            assertTrue(state.customTags.any { it.id == customTag1.id && it.name == "Renamed Tag" })
        }

    @Test
    fun `SubmitForm on NetworkError sets top-level error and keeps tagFormState non-null`() =
        runTest {
            val networkError = RuntimeException("network")
            val delegate = FakeTagRepository().also { it.seedTags(defaultTag, customTag1, customTag2) }
            val errorRepo =
                object : TagRepository by delegate {
                    override suspend fun createTag(name: String) =
                        Result.Failure<TagError>(TagError.NetworkError(networkError))
                }
            val errorVm = TagManagementViewModel(errorRepo)
            errorVm.onIntent(TagManagementIntent.OpenCreateForm)
            errorVm.onIntent(TagManagementIntent.UpdateFormName("Unique"))
            errorVm.onIntent(TagManagementIntent.SubmitForm)
            val state = errorVm.uiState.value
            assertIs<TagManagementError.NetworkError>(state.error)
            assertNotNull(state.tagFormState)
        }

    @Test
    fun `DismissForm clears tagFormState`() =
        runTest {
            vm.onIntent(TagManagementIntent.OpenCreateForm)
            vm.onIntent(TagManagementIntent.DismissForm)
            assertNull(vm.uiState.value.tagFormState)
        }

    @Test
    fun `RequestDeleteTag sets pendingDeleteTag and correct pendingDeleteBookCount`() =
        runTest {
            // Add customTag1 to a book via the repo
            repo.addTagToBook("book1", customTag1.id)
            vm.onIntent(TagManagementIntent.RequestDeleteTag(customTag1))
            val state = vm.uiState.value
            assertEquals(customTag1, state.pendingDeleteTag)
            assertEquals(1, state.pendingDeleteBookCount)
        }

    @Test
    fun `ConfirmDeleteTag deletes tag, clears pendingDelete state, and refreshes lists`() =
        runTest {
            vm.onIntent(TagManagementIntent.RequestDeleteTag(customTag1))
            vm.onIntent(TagManagementIntent.ConfirmDeleteTag)
            val state = vm.uiState.value
            assertNull(state.pendingDeleteTag)
            assertNull(state.pendingDeleteBookCount)
            assertFalse(state.customTags.any { it.id == customTag1.id })
            assertTrue(repo.deleteTagCalled >= 1)
        }

    @Test
    fun `CancelDelete clears pendingDeleteTag and pendingDeleteBookCount`() =
        runTest {
            vm.onIntent(TagManagementIntent.RequestDeleteTag(customTag1))
            vm.onIntent(TagManagementIntent.CancelDelete)
            val state = vm.uiState.value
            assertNull(state.pendingDeleteTag)
            assertNull(state.pendingDeleteBookCount)
        }

    @Test
    fun `DismissError clears error`() =
        runTest {
            val errorRepo = failingGetTagsRepo()
            val errorVm = TagManagementViewModel(errorRepo)
            assertNotNull(errorVm.uiState.value.error)
            errorVm.onIntent(TagManagementIntent.DismissError)
            assertNull(errorVm.uiState.value.error)
        }

    private fun failingGetTagsRepo(): TagRepository =
        object : TagRepository {
            override val tagsFlow = MutableStateFlow<List<Tag>?>(null)

            override suspend fun getTags() = Result.Failure<TagError>(TagError.NetworkError(RuntimeException("fail")))

            override suspend fun createTag(name: String) = Result.Success(Tag("", name, false))

            override suspend fun renameTag(
                id: String,
                newName: String
            ) = Result.Success(Tag(id, newName, false))

            override suspend fun deleteTag(id: String) = Result.Success(Unit)

            override suspend fun addTagToBook(
                bookId: String,
                tagId: String
            ) = Result.Success(Unit)

            override suspend fun removeTagFromBook(
                bookId: String,
                tagId: String
            ) = Result.Success(Unit)

            override suspend fun getBookCountForTag(tagId: String) = Result.Success(0)
        }
}
