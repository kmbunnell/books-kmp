package com.example.books_kmp.ui.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.model.Book
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var bookRepo: FakeBookRepository
    private lateinit var repo: FakeTagRepository
    private lateinit var vm: LibraryViewModel

    private val tag1 = Tag(id = "t1", name = "Read", isDefault = true)
    private val tag2 = Tag(id = "t2", name = "Favorites", isDefault = false)
    private val book1 =
        Book(id = "b1", isbn = "111", title = "Book One", authors = listOf("Author A"), coverImageUrl = null)
    private val book2 =
        Book(id = "b2", isbn = "222", title = "Book Two", authors = listOf("Author B"), coverImageUrl = null)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        bookRepo = FakeBookRepository()
        bookRepo.seedBooks(book1, book2)
        repo = FakeTagRepository()
        repo.seedTags(tag1, tag2)
        vm = LibraryViewModel(bookRepo, repo)
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
    fun `init loads books into uiState`() =
        runTest {
            val state = vm.uiState.value
            assertEquals(listOf(book1, book2), state.books)
        }

    @Test
    fun `init makes exactly two repository calls`() =
        runTest {
            assertEquals(1, bookRepo.getBooksByUserCalled)
            assertEquals(1, repo.getTagsCalled)
        }

    @Test
    fun `both repository calls are made concurrently`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val concurrentBookRepo = FakeBookRepository()
            concurrentBookRepo.seedBooks(book1)
            concurrentBookRepo.getBooksByUserGate = gate

            val concurrentTagRepo = FakeTagRepository()
            concurrentTagRepo.seedTags(tag1)

            val concurrentVm = LibraryViewModel(concurrentBookRepo, concurrentTagRepo)

            // books fetch is gated — tags fetch should have completed before we release the gate
            assertEquals(1, concurrentTagRepo.getTagsCalled)

            gate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `init sets isLoading false after result`() =
        runTest {
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `loadLibrary success populates books and tags with isLoading false`() =
        runTest {
            val state = vm.uiState.value
            assertEquals(listOf(book1, book2), state.books)
            assertEquals(listOf(tag1, tag2), state.tags)
            assertFalse(state.isLoading)
        }

    @Test
    fun `loadLibrary book failure sets loadFailed true and isLoading false, leaves lists empty`() =
        runTest {
            bookRepo = FakeBookRepository(getBooksShouldFail = true)
            val failVm = LibraryViewModel(bookRepo, repo)
            val state = failVm.uiState.value
            assertTrue(state.loadFailed)
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertTrue(state.tags.isEmpty())
        }

    @Test
    fun `loadLibrary tag failure sets loadFailed true and isLoading false, leaves lists empty`() =
        runTest {
            val failVm = LibraryViewModel(bookRepo, failingGetTagsRepo())
            val state = failVm.uiState.value
            assertTrue(state.loadFailed)
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertTrue(state.tags.isEmpty())
        }

    @Test
    fun `init network error sets loadFailed flag`() =
        runTest {
            val errorVm = LibraryViewModel(bookRepo, failingGetTagsRepo())
            assertTrue(errorVm.uiState.value.loadFailed)
            assertFalse(errorVm.uiState.value.isLoading)
        }

    @Test
    fun `Refresh after failure resets loadFailed to false and re-fetches both`() =
        runTest {
            bookRepo = FakeBookRepository(getBooksShouldFail = true)
            val retryVm = LibraryViewModel(bookRepo, repo)
            assertTrue(retryVm.uiState.value.loadFailed)

            bookRepo.getBooksShouldFail = false
            retryVm.onIntent(LibraryIntent.Refresh)

            assertFalse(retryVm.uiState.value.loadFailed)
            assertEquals(2, bookRepo.getBooksByUserCalled)
            assertEquals(listOf(tag1, tag2), retryVm.uiState.value.tags)
        }

    @Test
    fun `second load while in-flight does not trigger duplicate calls`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val gatedBookRepo = FakeBookRepository()
            gatedBookRepo.seedBooks(book1)
            gatedBookRepo.getBooksByUserGate = gate

            val gatedTagRepo = FakeTagRepository()
            gatedTagRepo.seedTags(tag1)

            val gatedVm = LibraryViewModel(gatedBookRepo, gatedTagRepo)
            // Load is in-flight (gated). Send Refresh — should be ignored.
            gatedVm.onIntent(LibraryIntent.Refresh)

            assertEquals(1, gatedBookRepo.getBooksByUserCalled)

            gate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `Refresh fetches tags again from repository`() =
        runTest {
            val countBefore = repo.getTagsCalled
            vm.onIntent(LibraryIntent.Refresh)
            assertEquals(countBefore + 1, repo.getTagsCalled)
        }

    @Test
    fun `Refresh updates tag list with renamed tag`() =
        runTest {
            repo.renameTag(tag2.id, "Renamed")
            vm.onIntent(LibraryIntent.Refresh)
            val tags = vm.uiState.value.tags
            assertTrue(tags.any { it.id == tag2.id && it.name == "Renamed" })
        }

    @Test
    fun `Refresh removes deleted tag from tags list`() =
        runTest {
            repo.deleteTag(tag2.id)
            vm.onIntent(LibraryIntent.Refresh)
            assertFalse(vm.uiState.value.tags.any { it.id == tag2.id })
        }

    @Test
    fun `Refresh removes deleted tag id from activeFilterTagIds`() =
        runTest {
            vm.onIntent(LibraryIntent.ToggleFilter(tag2.id))
            assertTrue(vm.uiState.value.activeFilterTagIds.contains(tag2.id))
            repo.deleteTag(tag2.id)
            vm.onIntent(LibraryIntent.Refresh)
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

    @Test
    fun `filteredBooks is all books when no filter is active`() =
        runTest {
            val taggedBook1 = book1.copy(tags = listOf("t1"))
            val taggedBook2 = book2.copy(tags = listOf("t2"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(taggedBook1, taggedBook2)
            val localVm = LibraryViewModel(localRepo, repo)
            assertEquals(listOf(taggedBook1, taggedBook2), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks returns only books whose tags intersect activeFilterTagIds`() =
        runTest {
            val taggedBook1 = book1.copy(tags = listOf("t1"))
            val taggedBook2 = book2.copy(tags = listOf("t2"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(taggedBook1, taggedBook2)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            assertEquals(listOf(taggedBook1), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks updates when filter is toggled off`() =
        runTest {
            val taggedBook1 = book1.copy(tags = listOf("t1"))
            val taggedBook2 = book2.copy(tags = listOf("t2"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(taggedBook1, taggedBook2)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            assertEquals(listOf(taggedBook1, taggedBook2), localVm.uiState.value.filteredBooks)
        }

    // AND tag filter logic

    @Test
    fun `filteredBooks AND tag logic — selecting two tags returns only books that carry both`() =
        runTest {
            val bothTags = book1.copy(tags = listOf("t1", "t2"))
            val onlyT1 = book2.copy(tags = listOf("t1"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(bothTags, onlyT1)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            localVm.onIntent(LibraryIntent.ToggleFilter("t2"))
            assertEquals(listOf(bothTags), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks AND tag logic — selecting one tag returns books with that tag`() =
        runTest {
            val taggedBook1 = book1.copy(tags = listOf("t1"))
            val taggedBook2 = book2.copy(tags = listOf("t2"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(taggedBook1, taggedBook2)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            assertEquals(listOf(taggedBook1), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks AND tag logic — selecting zero tags returns all books`() =
        runTest {
            val taggedBook1 = book1.copy(tags = listOf("t1"))
            val taggedBook2 = book2.copy(tags = listOf("t2"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(taggedBook1, taggedBook2)
            val localVm = LibraryViewModel(localRepo, repo)
            assertEquals(listOf(taggedBook1, taggedBook2), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks AND tag logic — deselecting one tag from two-tag selection widens result`() =
        runTest {
            val bothTags = book1.copy(tags = listOf("t1", "t2"))
            val onlyT1 = book2.copy(tags = listOf("t1"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(bothTags, onlyT1)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            localVm.onIntent(LibraryIntent.ToggleFilter("t2"))
            assertEquals(listOf(bothTags), localVm.uiState.value.filteredBooks)
            localVm.onIntent(LibraryIntent.ToggleFilter("t2"))
            assertEquals(listOf(bothTags, onlyT1), localVm.uiState.value.filteredBooks)
        }

    // Text search

    @Test
    fun `filteredBooks text search — case-insensitive — harry matches title Harry Potter`() =
        runTest {
            val harryPotter = book1.copy(title = "Harry Potter")
            val other = book2.copy(title = "Other Book")
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(harryPotter, other)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ChangeSearchQuery("harry"))
            assertEquals(listOf(harryPotter), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks text search — diacritic-insensitive — Bronte matches author Bronte with umlaut`() =
        runTest {
            val bronte = book1.copy(title = "Jane Eyre", authors = listOf("Charlotte Brontë"))
            val other = book2.copy(title = "Other")
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(bronte, other)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ChangeSearchQuery("Bronte"))
            assertEquals(listOf(bronte), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks text search — partial match — Har matches Harry Potter`() =
        runTest {
            val harryPotter = book1.copy(title = "Harry Potter")
            val other = book2.copy(title = "Other Book")
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(harryPotter, other)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ChangeSearchQuery("Har"))
            assertEquals(listOf(harryPotter), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks text search — blank query returns all books`() =
        runTest {
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(book1, book2)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ChangeSearchQuery(""))
            assertEquals(listOf(book1, book2), localVm.uiState.value.filteredBooks)
        }

    // Tag filter and text search combined

    @Test
    fun `filteredBooks tag filter and text search apply simultaneously`() =
        runTest {
            val taggedHarry = book1.copy(title = "Harry Potter", tags = listOf("t1"))
            val taggedOther = book2.copy(title = "Other Book", tags = listOf("t1"))
            val untaggedHarry =
                Book(
                    id = "b3",
                    isbn = "333",
                    title = "Harry Houdini",
                    authors = listOf("Bio"),
                    coverImageUrl = null,
                    tags = listOf("t2")
                )
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(taggedHarry, taggedOther, untaggedHarry)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            localVm.onIntent(LibraryIntent.ChangeSearchQuery("harry"))
            assertEquals(listOf(taggedHarry), localVm.uiState.value.filteredBooks)
        }

    // Sort

    @Test
    fun `filteredBooks TITLE_ASC produces A-Z order by full title`() =
        runTest {
            val bookA = book1.copy(title = "Zebra")
            val bookB = book2.copy(title = "Apple")
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(bookA, bookB)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ChangeSortOrder(SortOrder.TITLE_ASC))
            assertEquals(listOf(bookB, bookA), localVm.uiState.value.filteredBooks)
        }

    @Test
    fun `filteredBooks AUTHOR_ASC sorts by last token of first author string`() =
        runTest {
            val bookZimmerman = book1.copy(title = "First", authors = listOf("Bob Zimmerman"))
            val bookAdams = book2.copy(title = "Second", authors = listOf("John Adams"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(bookZimmerman, bookAdams)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ChangeSortOrder(SortOrder.AUTHOR_ASC))
            assertEquals(listOf(bookAdams, bookZimmerman), localVm.uiState.value.filteredBooks)
        }

    // Clear filters

    @Test
    fun `ClearFilters resets activeFilterTagIds and searchQuery and filteredBooks returns full sorted list`() =
        runTest {
            val taggedBook1 = book1.copy(tags = listOf("t1"))
            val taggedBook2 = book2.copy(tags = listOf("t2"))
            val localRepo = FakeBookRepository()
            localRepo.seedBooks(taggedBook1, taggedBook2)
            val localVm = LibraryViewModel(localRepo, repo)
            localVm.onIntent(LibraryIntent.ToggleFilter("t1"))
            localVm.onIntent(LibraryIntent.ChangeSearchQuery("Book One"))
            localVm.onIntent(LibraryIntent.ClearFilters)
            val state = localVm.uiState.value
            assertTrue(state.activeFilterTagIds.isEmpty())
            assertEquals("", state.searchQuery)
            assertEquals(listOf(taggedBook1, taggedBook2), state.filteredBooks)
        }

    // booksFlow reactive update

    @Test
    fun `booksFlow emission updates books and filteredBooks in Library state`() =
        runTest {
            advanceUntilIdle()
            bookRepo.applyTagDelta(book1.id, "t1", wasApplied = false)
            advanceUntilIdle()
            val updatedBook = vm.uiState.value.books.find { it.id == "b1" }
            assertEquals(listOf("t1"), updatedBook?.tags)
        }

    // Zero repo calls for filter/sort/search

    @Test
    fun `ChangeSortOrder triggers zero repository calls`() =
        runTest {
            val callsBefore = bookRepo.getBooksByUserCalled
            vm.onIntent(LibraryIntent.ChangeSortOrder(SortOrder.AUTHOR_ASC))
            assertEquals(callsBefore, bookRepo.getBooksByUserCalled)
        }

    @Test
    fun `ChangeSearchQuery triggers zero repository calls`() =
        runTest {
            val callsBefore = bookRepo.getBooksByUserCalled
            vm.onIntent(LibraryIntent.ChangeSearchQuery("test"))
            assertEquals(callsBefore, bookRepo.getBooksByUserCalled)
        }

    @Test
    fun `ClearFilters triggers zero repository calls`() =
        runTest {
            val callsBefore = bookRepo.getBooksByUserCalled
            vm.onIntent(LibraryIntent.ClearFilters)
            assertEquals(callsBefore, bookRepo.getBooksByUserCalled)
        }

    private fun failingGetTagsRepo(): TagRepository =
        object : TagRepository {
            override suspend fun getTags() = Result.Failure<TagError>(TagError.NetworkError(RuntimeException("fail")))

            override suspend fun createTag(name: String) = Result.Success(Tag("", name, false))

            override suspend fun renameTag(
                id: String,
                newName: String,
            ) = Result.Success(Tag(id, newName, false))

            override suspend fun deleteTag(id: String) = Result.Success(Unit)

            override suspend fun addTagToBook(
                bookId: String,
                tagId: String,
            ) = Result.Success(Unit)

            override suspend fun removeTagFromBook(
                bookId: String,
                tagId: String,
            ) = Result.Success(Unit)

            override suspend fun getBookCountForTag(tagId: String) = Result.Success(0)
        }
}
