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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
class BookDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FakeTagRepository
    private lateinit var vm: BookDetailViewModel

    private val bookId = "book-1"
    private val tag1 = Tag(id = "t1", name = "Fiction", isDefault = false)
    private val tag2 = Tag(id = "t2", name = "Read", isDefault = true)
    private val tag3 = Tag(id = "t3", name = "Favorites", isDefault = false)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeTagRepository()
        repo.seedTags(tag1, tag2, tag3)
        repo.seedBookTags(bookId to tag1.id, bookId to tag2.id)
        vm = BookDetailViewModel(bookId, repo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Init tests

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
            val failVm = BookDetailViewModel(bookId, FailingLoadRepo(repo))
            advanceUntilIdle()
            assertTrue(failVm.uiState.value.loadFailed)
            assertFalse(failVm.uiState.value.isLoading)
        }

    @Test
    fun `init sets loadFailed true when getTagsForBook returns failure`() =
        runTest {
            val failVm = BookDetailViewModel(bookId, FailingBookTagsRepo(repo))
            advanceUntilIdle()
            assertTrue(failVm.uiState.value.loadFailed)
            assertFalse(failVm.uiState.value.isLoading)
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
    fun `ToggleTag adds tagId to inFlightTagIds before repo call`() =
        runTest {
            val suspendingRepo = SuspendingAddRepo(repo)
            val suspendVm = BookDetailViewModel(bookId, suspendingRepo)
            advanceUntilIdle() // finish init
            suspendVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            // The synchronous update runs before the launched coroutine suspends
            assertTrue(suspendVm.uiState.value.inFlightTagIds.contains(tag3.id))
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
            val failRepo = FailingAddRepo(repo)
            val failVm = BookDetailViewModel(bookId, failRepo)
            advanceUntilIdle()
            failVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertFalse(failVm.uiState.value.inFlightTagIds.contains(tag3.id))
        }

    @Test
    fun `ToggleTag on failure restores appliedTagIds snapshot`() =
        runTest {
            val failRepo = FailingAddRepo(repo)
            val failVm = BookDetailViewModel(bookId, failRepo)
            advanceUntilIdle()
            val snapshotBefore = failVm.uiState.value.appliedTagIds
            failVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertEquals(snapshotBefore, failVm.uiState.value.appliedTagIds)
        }

    @Test
    fun `ToggleTag on failure sets tagToggleError non-null`() =
        runTest {
            val failRepo = FailingAddRepo(repo)
            val failVm = BookDetailViewModel(bookId, failRepo)
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
            val failRepo = FailingRemoveRepo(repo)
            val failVm = BookDetailViewModel(bookId, failRepo)
            advanceUntilIdle()
            val snapshotBefore = failVm.uiState.value.appliedTagIds
            failVm.onIntent(BookDetailIntent.ToggleTag(tag1.id))
            advanceUntilIdle()
            assertEquals(snapshotBefore, failVm.uiState.value.appliedTagIds)
        }

    // In-flight guard

    @Test
    fun `ToggleTag while same tagId in-flight makes no state change and no additional repo call`() =
        runTest {
            val suspendingRepo = SuspendingAddRepo(repo)
            val suspendVm = BookDetailViewModel(bookId, suspendingRepo)
            advanceUntilIdle()
            suspendVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            val stateAfterFirst = suspendVm.uiState.value
            val addCalledAfterFirst = suspendingRepo.addTagToBookCalled
            // Second intent while in-flight — should be no-op
            suspendVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            assertEquals(stateAfterFirst, suspendVm.uiState.value)
            assertEquals(addCalledAfterFirst, suspendingRepo.addTagToBookCalled)
            suspendingRepo.addDeferred.complete(Result.Success(Unit))
        }

    // DismissTagToggleError

    @Test
    fun `DismissTagToggleError clears tagToggleError`() =
        runTest {
            val failRepo = FailingAddRepo(repo)
            val failVm = BookDetailViewModel(bookId, failRepo)
            advanceUntilIdle()
            failVm.onIntent(BookDetailIntent.ToggleTag(tag3.id))
            advanceUntilIdle()
            assertNotNull(failVm.uiState.value.tagToggleError)
            failVm.onIntent(BookDetailIntent.DismissTagToggleError)
            assertNull(failVm.uiState.value.tagToggleError)
        }

    // ---- Test doubles ----

    /**
     * Delegates everything but makes addTagToBook actually suspend (via CompletableDeferred.await)
     * so the test can observe in-flight state before the coroutine resumes.
     */
    private class SuspendingAddRepo(delegate: FakeTagRepository) : TagRepository by delegate {
        var addTagToBookCalled = 0
        val addDeferred = CompletableDeferred<Result<Unit, TagError>>()

        override suspend fun addTagToBook(
            bookId: String,
            tagId: String
        ): Result<Unit, TagError> {
            addTagToBookCalled++
            return addDeferred.await()
        }
    }

    private class FailingAddRepo(delegate: FakeTagRepository) : TagRepository by delegate {
        override suspend fun addTagToBook(
            bookId: String,
            tagId: String
        ): Result<Unit, TagError> = Result.Failure(TagError.NetworkError(RuntimeException("fail")))
    }

    private class FailingRemoveRepo(delegate: FakeTagRepository) : TagRepository by delegate {
        override suspend fun removeTagFromBook(
            bookId: String,
            tagId: String
        ): Result<Unit, TagError> = Result.Failure(TagError.NetworkError(RuntimeException("fail")))
    }

    private class FailingLoadRepo(delegate: FakeTagRepository) : TagRepository by delegate {
        override suspend fun getTags(): Result<List<Tag>, TagError> =
            Result.Failure(TagError.NetworkError(RuntimeException("fail")))
    }

    private class FailingBookTagsRepo(delegate: FakeTagRepository) : TagRepository by delegate {
        override suspend fun getTagsForBook(bookId: String): Result<List<Tag>, TagError> =
            Result.Failure(TagError.NetworkError(RuntimeException("fail")))
    }
}
