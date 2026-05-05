package com.example.books_kmp.domain.tags

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class DefaultToggleBookTagUseCaseTest {
    private val bookId = "book-1"
    private val book =
        Book(
            id = bookId,
            isbn = null,
            title = "Test Book",
            authors = listOf("Author"),
            coverImageUrl = null,
            tags = listOf("t1", "t2"),
        )

    @Test
    fun `invoke add - success calls addTagToBook and applies delta`() =
        runTest {
            val tagRepo = FakeTagRepository()
            val bookRepo = FakeBookRepository().also { it.seedBooks(book) }
            val useCase = DefaultToggleBookTagUseCase(tagRepo, bookRepo)

            val result = useCase(bookId, "t3", wasApplied = false)

            assertIs<Result.Success<Unit>>(result)
            assertEquals(1, tagRepo.addTagToBookCalled)
            assertEquals(0, tagRepo.removeTagFromBookCalled)
            assertEquals(1, bookRepo.applyTagDeltaCalled)
            assertEquals(Triple(bookId, "t3", false), bookRepo.lastApplyTagDeltaArgs)
        }

    @Test
    fun `invoke remove - success calls removeTagFromBook and applies delta`() =
        runTest {
            val tagRepo = FakeTagRepository()
            val bookRepo = FakeBookRepository().also { it.seedBooks(book) }
            val useCase = DefaultToggleBookTagUseCase(tagRepo, bookRepo)

            val result = useCase(bookId, "t1", wasApplied = true)

            assertIs<Result.Success<Unit>>(result)
            assertEquals(0, tagRepo.addTagToBookCalled)
            assertEquals(1, tagRepo.removeTagFromBookCalled)
            assertEquals(1, bookRepo.applyTagDeltaCalled)
            assertEquals(Triple(bookId, "t1", true), bookRepo.lastApplyTagDeltaArgs)
        }

    @Test
    fun `invoke add - on failure returns NetworkError and does not apply delta`() =
        runTest {
            val bookRepo = FakeBookRepository().also { it.seedBooks(book) }
            val useCase = DefaultToggleBookTagUseCase(FailingTagRepository(), bookRepo)

            val result = useCase(bookId, "t3", wasApplied = false)

            assertIs<Result.Failure<ToggleBookTagError>>(result)
            assertEquals(ToggleBookTagError.NetworkError, result.error)
            assertEquals(0, bookRepo.applyTagDeltaCalled)
        }

    @Test
    fun `invoke remove - on failure returns NetworkError and does not apply delta`() =
        runTest {
            val bookRepo = FakeBookRepository().also { it.seedBooks(book) }
            val useCase = DefaultToggleBookTagUseCase(FailingTagRepository(), bookRepo)

            val result = useCase(bookId, "t1", wasApplied = true)

            assertIs<Result.Failure<ToggleBookTagError>>(result)
            assertEquals(ToggleBookTagError.NetworkError, result.error)
            assertEquals(0, bookRepo.applyTagDeltaCalled)
        }

    private class FailingTagRepository : TagRepository by FakeTagRepository() {
        override suspend fun addTagToBook(
            bookId: String,
            tagId: String
        ) = Result.Failure<TagError>(TagError.NetworkError(RuntimeException("fail")))

        override suspend fun removeTagFromBook(
            bookId: String,
            tagId: String
        ) = Result.Failure<TagError>(TagError.NetworkError(RuntimeException("fail")))
    }
}
