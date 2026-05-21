package com.example.books_kmp.ui.bookdetail

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.library.BookRepositoryError
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.DefaultToggleBookTagUseCase
import com.example.books_kmp.domain.tags.FakeTagRepository
import com.example.books_kmp.domain.tags.TagError
import com.example.books_kmp.domain.tags.TagRepository
import com.example.books_kmp.domain.tags.ToggleBookTagError
import com.example.books_kmp.domain.tags.ToggleBookTagUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FakeTagRepository
    private lateinit var bookRepo: FakeBookRepository
    private lateinit var vm: BookDetailViewModel

    private val bookId = "book-1"
    private val testBook =
        Book(
            id = bookId,
            isbn = null,
            title = "Test Book",
            authors = listOf("Author"),
            coverImageUrl = null,
            tags = listOf("t1", "t2")
        )
    private val tag1 = Tag(id = "t1", name = "Fiction", isDefault = false)
    private val tag2 = Tag(id = "t2", name = "Read", isDefault = true)
    private val tag3 = Tag(id = "t3", name = "Favorites", isDefault = false)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeTagRepository()
        repo.seedTags(tag1, tag2, tag3)
        bookRepo = FakeBookRepository()
        bookRepo.seedBooks(testBook)
        vm = BookDetailViewModel(bookId, repo, bookRepo, DefaultToggleBookTagUseCase(repo, bookRepo))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Init tests

    @Test
    fun `init loads book into state`() =
        runTest {
            assertEquals(testBook, vm.uiState.value.book)
        }

    @Test
    fun `init loads allTags and appliedTagIds from repository`() =
        runTest {
            val state = vm.uiState.value
            assertEquals(setOf("t1", "t2"), state.appliedTagIds)
            assertTrue(state.allTags.containsAll(listOf(tag1, tag2, tag3)))
        }

    @Test
    fun `init sets isLoading true then false`() =
        runTest {
            // With UnconfinedTestDispatcher, init coroutine completes synchronously by the time we
            // check, so isLoading should be false after full construction.
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `init sets loadFailed true when getTags returns failure`() =
        runTest {
            val failVm =
                BookDetailViewModel(
                    bookId,
                    FailingLoadRepo(repo),
                    bookRepo,
                    successToggleUseCase(),
                )
            advanceUntilIdle()
            assertTrue(failVm.uiState.value.loadFailed)
            assertFalse(failVm.uiState.value.isLoading)
        }

    @Test
    fun `init sets loadFailed true when getBookById returns failure`() =
        runTest {
            val failBookRepo = FakeBookRepository(getBookByIdShouldFail = true)
            val failVm =
                BookDetailViewModel(
                    bookId,
                    repo,
                    failBookRepo,
                    successToggleUseCase(),
                )
            advanceUntilIdle()
            assertTrue(failVm.uiState.value.loadFailed)
            assertFalse(failVm.uiState.value.isLoading)
        }

    @Test
    fun `init sets loadFailed true when book not found (getBookById returns null)`() =
        runTest {
            val emptyBookRepo = FakeBookRepository()
            val nullVm =
                BookDetailViewModel(
                    bookId,
                    repo,
                    emptyBookRepo,
                    successToggleUseCase(),
                )
            advanceUntilIdle()
            assertTrue(nullVm.uiState.value.loadFailed)
        }

    @Test
    fun `allTags sorted alphabetically in state`() =
        runTest {
            // setUp seeds [Fiction (t1), Read (t2), Favorites (t3)]
            // sorted by name: Favorites, Fiction, Read
            val allTags = vm.uiState.value.allTags
            assertEquals(listOf(tag3, tag1, tag2), allTags)
        }

    // ToggleTag — applying (not currently applied)

    @Test
    fun `ToggleTag adds tagId to appliedTagIds optimistically before coroutine suspends`() =
        runTest {
            // tag3 is not applied; after onIntent the optimistic update is synchronous
            vm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            assertTrue(vm.uiState.value.appliedTagIds.contains(tag3.id))
        }

    @Test
    fun `ToggleTag adds tagId to inFlightTagIds before use case completes`() =
        runTest {
            val suspendingUseCase = SuspendingToggleUseCase()
            val suspendVm = BookDetailViewModel(bookId, repo, bookRepo, suspendingUseCase)
            advanceUntilIdle() // finish init
            suspendVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            // The synchronous optimistic update runs before the launched coroutine suspends
            assertTrue(suspendVm.uiState.value.inFlightTagIds.contains(tag3.id))
            suspendingUseCase.deferred.complete(Result.Success(Unit))
        }

    @Test
    fun `ToggleTag on success removes tagId from inFlightTagIds`() =
        runTest {
            vm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertFalse(vm.uiState.value.inFlightTagIds.contains(tag3.id))
        }

    @Test
    fun `ToggleTag on success calls addTagToBook exactly once`() =
        runTest {
            val before = repo.addTagToBookCalled
            vm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertEquals(before + 1, repo.addTagToBookCalled)
        }

    @Test
    fun `ToggleTag on failure removes tagId from inFlightTagIds`() =
        runTest {
            val failVm = BookDetailViewModel(bookId, repo, bookRepo, failingToggleUseCase())
            advanceUntilIdle()
            failVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertFalse(failVm.uiState.value.inFlightTagIds.contains(tag3.id))
        }

    @Test
    fun `ToggleTag on failure restores appliedTagIds snapshot`() =
        runTest {
            val failVm = BookDetailViewModel(bookId, repo, bookRepo, failingToggleUseCase())
            advanceUntilIdle()
            val snapshotBefore = failVm.uiState.value.appliedTagIds
            failVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertEquals(snapshotBefore, failVm.uiState.value.appliedTagIds)
        }

    @Test
    fun `ToggleTag on failure sets tagToggleError non-null`() =
        runTest {
            val failVm = BookDetailViewModel(bookId, repo, bookRepo, failingToggleUseCase())
            advanceUntilIdle()
            failVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertNotNull(failVm.uiState.value.tagToggleError)
        }

    // ToggleTag — removing (currently applied)

    @Test
    fun `ToggleTag removes tagId from appliedTagIds optimistically`() =
        runTest {
            vm.onIntent(BookDetailIntent.ToggleTag(tag1.id))
            assertFalse(vm.uiState.value.appliedTagIds.contains(tag1.id))
        }

    @Test
    fun `ToggleTag remove on success calls removeTagFromBook exactly once`() =
        runTest {
            val before = repo.removeTagFromBookCalled
            vm.onIntent(BookDetailIntent.ToggleTag(tag1.id))
            advanceUntilIdle()
            assertEquals(before + 1, repo.removeTagFromBookCalled)
        }

    @Test
    fun `ToggleTag remove on failure restores appliedTagIds`() =
        runTest {
            val failVm = BookDetailViewModel(bookId, repo, bookRepo, failingToggleUseCase())
            advanceUntilIdle()
            val snapshotBefore = failVm.uiState.value.appliedTagIds
            failVm.onIntent(BookDetailIntent.ToggleTag(tag1.id))
            advanceUntilIdle()
            assertEquals(snapshotBefore, failVm.uiState.value.appliedTagIds)
        }

    // In-flight guard

    @Test
    fun `ToggleTag while same tagId in-flight makes no state change and no additional use case call`() =
        runTest {
            val suspendingUseCase = SuspendingToggleUseCase()
            val suspendVm = BookDetailViewModel(bookId, repo, bookRepo, suspendingUseCase)
            advanceUntilIdle()
            suspendVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            val stateAfterFirst = suspendVm.uiState.value
            val callCountAfterFirst = suspendingUseCase.callCount
            // Second intent while in-flight — should be no-op
            suspendVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            assertEquals(stateAfterFirst, suspendVm.uiState.value)
            assertEquals(callCountAfterFirst, suspendingUseCase.callCount)
            suspendingUseCase.deferred.complete(Result.Success(Unit))
        }

    // DismissTagToggleError

    @Test
    fun `DismissTagToggleError clears tagToggleError`() =
        runTest {
            val failVm = BookDetailViewModel(bookId, repo, bookRepo, failingToggleUseCase())
            advanceUntilIdle()
            failVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertNotNull(failVm.uiState.value.tagToggleError)
            failVm.onIntent(BookDetailIntent.DismissTagToggleError)
            assertNull(failVm.uiState.value.tagToggleError)
        }

    // Reload

    @Test
    fun `Reload after failure clears loadFailed and populates state on second success`() =
        runTest {
            val failFirstRepo = FailFirstGetTagsRepo(repo)
            val failVm =
                BookDetailViewModel(
                    bookId,
                    failFirstRepo,
                    bookRepo,
                    successToggleUseCase(),
                )
            advanceUntilIdle()
            assertTrue(failVm.uiState.value.loadFailed)

            failVm.onIntent(BookDetailIntent.Reload)
            advanceUntilIdle()
            assertFalse(failVm.uiState.value.loadFailed)
            assertFalse(failVm.uiState.value.isLoading)
            assertTrue(failVm.uiState.value.allTags.isNotEmpty())
        }

    @Test
    fun `Reload clears loadFailed and repopulates book on second success`() =
        runTest {
            val failFirstBookRepo = FailFirstBookRepo(bookRepo)
            val failVm =
                BookDetailViewModel(
                    bookId,
                    repo,
                    failFirstBookRepo,
                    successToggleUseCase(),
                )
            advanceUntilIdle()
            assertTrue(failVm.uiState.value.loadFailed)

            failVm.onIntent(BookDetailIntent.Reload)
            advanceUntilIdle()
            assertFalse(failVm.uiState.value.loadFailed)
            assertNotNull(failVm.uiState.value.book)
        }

    @Test
    fun `Reload is ignored while load is already in flight`() =
        runTest {
            val suspendingRepo = SuspendingGetTagsRepo(repo)
            val suspendVm =
                BookDetailViewModel(
                    bookId,
                    suspendingRepo,
                    bookRepo,
                    successToggleUseCase(),
                )
            assertTrue(suspendVm.uiState.value.isLoading)

            suspendVm.onIntent(BookDetailIntent.Reload)
            assertEquals(1, suspendingRepo.getTagsCallCount)
        }

    // --- Delete book ---

    @Test
    fun `DeleteBook sets showDeleteConfirm true`() =
        runTest {
            vm.onIntent(BookDetailIntent.DeleteBook)
            assertTrue(vm.uiState.value.showDeleteConfirm)
        }

    @Test
    fun `DismissDelete clears showDeleteConfirm`() =
        runTest {
            vm.onIntent(BookDetailIntent.DeleteBook)
            vm.onIntent(BookDetailIntent.DismissDelete)
            assertFalse(vm.uiState.value.showDeleteConfirm)
        }

    @Test
    fun `ConfirmDelete sets isDeleting true while in-flight`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            bookRepo.deleteBookGate = gate
            vm.onIntent(BookDetailIntent.DeleteBook)
            vm.onIntent(BookDetailIntent.ConfirmDelete)
            assertTrue(vm.uiState.value.isDeleting)
            gate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `ConfirmDelete on success emits NavigateUp effect`() =
        runTest {
            val effects = mutableListOf<BookDetailEffect>()
            val job = launch { vm.effects.collect { effects.add(it) } }
            vm.onIntent(BookDetailIntent.DeleteBook)
            vm.onIntent(BookDetailIntent.ConfirmDelete)
            advanceUntilIdle()
            job.cancel()
            assertTrue(effects.contains(BookDetailEffect.NavigateUp))
        }

    @Test
    fun `ConfirmDelete on failure clears isDeleting and sets deleteError`() =
        runTest {
            bookRepo.deleteBookShouldFail = true
            vm.onIntent(BookDetailIntent.DeleteBook)
            vm.onIntent(BookDetailIntent.ConfirmDelete)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isDeleting)
            assertEquals(BookDetailError.DeleteFailed, vm.uiState.value.deleteError)
        }

    @Test
    fun `ConfirmDelete while already deleting is a no-op`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            bookRepo.deleteBookGate = gate
            vm.onIntent(BookDetailIntent.DeleteBook)
            vm.onIntent(BookDetailIntent.ConfirmDelete)
            vm.onIntent(BookDetailIntent.ConfirmDelete)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, bookRepo.deleteBookCalled)
        }

    @Test
    fun `DismissDeleteError clears deleteError`() =
        runTest {
            bookRepo.deleteBookShouldFail = true
            vm.onIntent(BookDetailIntent.DeleteBook)
            vm.onIntent(BookDetailIntent.ConfirmDelete)
            advanceUntilIdle()
            assertEquals(BookDetailError.DeleteFailed, vm.uiState.value.deleteError)
            vm.onIntent(BookDetailIntent.DismissDeleteError)
            assertNull(vm.uiState.value.deleteError)
        }

    // ---- Test doubles ----

    private fun successToggleUseCase(): ToggleBookTagUseCase = ToggleBookTagUseCase { _, _, _ -> Result.Success(Unit) }

    private fun failingToggleUseCase(): ToggleBookTagUseCase =
        ToggleBookTagUseCase { _, _, _ -> Result.Failure(ToggleBookTagError.NetworkError) }

    private inner class SuspendingToggleUseCase : ToggleBookTagUseCase {
        var callCount = 0
        val deferred = CompletableDeferred<Result<Unit, ToggleBookTagError>>()

        override suspend fun invoke(
            bookId: String,
            tagId: String,
            wasApplied: Boolean,
        ): Result<Unit, ToggleBookTagError> {
            callCount++
            return deferred.await()
        }
    }

    private class FailingLoadRepo(delegate: FakeTagRepository) : TagRepository by delegate {
        override suspend fun getTags(): Result<List<Tag>, TagError> =
            Result.Failure(TagError.NetworkError(RuntimeException("fail")))
    }

    private class FailFirstGetTagsRepo(private val delegate: FakeTagRepository) : TagRepository by delegate {
        private var callCount = 0

        override suspend fun getTags(): Result<List<Tag>, TagError> {
            return if (callCount++ == 0) {
                Result.Failure(TagError.NetworkError(RuntimeException("fail")))
            } else {
                delegate.getTags()
            }
        }
    }

    private class FailFirstBookRepo(
        private val delegate: FakeBookRepository,
    ) : BookRepository by delegate {
        private var callCount = 0

        override suspend fun getBookById(id: String): Result<Book?, BookRepositoryError> {
            return if (callCount++ == 0) {
                Result.Failure(BookRepositoryError.NetworkError)
            } else {
                delegate.getBookById(id)
            }
        }
    }

    private class SuspendingGetTagsRepo(private val delegate: FakeTagRepository) : TagRepository by delegate {
        var getTagsCallCount = 0
        private val deferred = CompletableDeferred<Result<List<Tag>, TagError>>()

        override suspend fun getTags(): Result<List<Tag>, TagError> {
            getTagsCallCount++
            return deferred.await()
        }
    }
}
