package com.example.books_kmp.viewmodel

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
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FakeTagRepository
    private lateinit var vm: LibraryViewModel

    private val tag1 = Tag(id = "t1", name = "Read", isDefault = true)
    private val tag2 = Tag(id = "t2", name = "Favorites", isDefault = false)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeTagRepository()
        repo.seedTags(tag1, tag2)
        vm = LibraryViewModel(repo)
        vm.onIntent(LibraryIntent.RefreshTags)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads all tags into uiState`() =
        runTest {
            val state = vm.uiState.value
            assertEquals(listOf(tag1, tag2), state.tags)
        }

    @Test
    fun `init sets isLoading false after result`() =
        runTest {
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `init network error sets loadFailed flag`() =
        runTest {
            val errorVm = LibraryViewModel(failingGetTagsRepo())
            errorVm.onIntent(LibraryIntent.RefreshTags)
            assertTrue(errorVm.uiState.value.loadFailed)
            assertFalse(errorVm.uiState.value.isLoading)
        }

    @Test
    fun `RefreshTags fetches tags again from repository`() =
        runTest {
            val countBefore = repo.getTagsCalled
            vm.onIntent(LibraryIntent.RefreshTags)
            assertEquals(countBefore + 1, repo.getTagsCalled)
        }

    @Test
    fun `RefreshTags updates tag list with renamed tag`() =
        runTest {
            repo.renameTag(tag2.id, "Renamed")
            vm.onIntent(LibraryIntent.RefreshTags)
            val tags = vm.uiState.value.tags
            assertTrue(tags.any { it.id == tag2.id && it.name == "Renamed" })
        }

    @Test
    fun `RefreshTags removes deleted tag from tags list`() =
        runTest {
            repo.deleteTag(tag2.id)
            vm.onIntent(LibraryIntent.RefreshTags)
            assertFalse(vm.uiState.value.tags.any { it.id == tag2.id })
        }

    @Test
    fun `RefreshTags removes deleted tag id from activeFilterTagIds`() =
        runTest {
            vm.onIntent(LibraryIntent.ToggleFilter(tag2.id))
            assertTrue(vm.uiState.value.activeFilterTagIds.contains(tag2.id))
            repo.deleteTag(tag2.id)
            vm.onIntent(LibraryIntent.RefreshTags)
            assertFalse(vm.uiState.value.activeFilterTagIds.contains(tag2.id))
        }

    @Test
    fun `ToggleFilter adds tag id to activeFilterTagIds when not active`() =
        runTest {
            vm.onIntent(LibraryIntent.ToggleFilter(tag1.id))
            assertTrue(vm.uiState.value.activeFilterTagIds.contains(tag1.id))
        }

    @Test
    fun `ToggleFilter removes tag id from activeFilterTagIds when already active`() =
        runTest {
            vm.onIntent(LibraryIntent.ToggleFilter(tag1.id))
            vm.onIntent(LibraryIntent.ToggleFilter(tag1.id))
            assertFalse(vm.uiState.value.activeFilterTagIds.contains(tag1.id))
        }

    private fun failingGetTagsRepo(): TagRepository =
        object : TagRepository {
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

            override suspend fun getTagsForBook(bookId: String): Result<List<Tag>, TagError> =
                Result.Success(emptyList())
        }
}
