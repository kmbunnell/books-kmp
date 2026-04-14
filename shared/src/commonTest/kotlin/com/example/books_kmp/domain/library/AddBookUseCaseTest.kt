package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class AddBookUseCaseTest {
    private val repo = FakeBookRepository()
    private val lookup = FakeBookLookupService()
    private val useCase = AddBookUseCase(repo, lookup)

    @Test
    fun `invoke returns Duplicate without calling the API when isbn already exists`() =
        runTest {
            repo.isbnExistsOverride = true
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.Duplicate, result.error)
            assertFalse(lookup.lookupCalled)
        }

    @Test
    fun `invoke returns Success with persisted Book for new valid ISBN`() =
        runTest {
            lookup.lookupResult =
                Result.Success(
                    BookLookupData(
                        isbn = "9780140449136",
                        title = "The Iliad",
                        authors = listOf("Homer"),
                        coverImageUrl = null,
                    ),
                )
            val result = useCase("9780140449136")
            assertIs<Result.Success<*>>(result)
        }

    @Test
    fun `invoke calls addBook with Book constructed correctly from BookLookupData`() =
        runTest {
            lookup.lookupResult =
                Result.Success(
                    BookLookupData(
                        isbn = "9780140449136",
                        title = "The Iliad",
                        authors = listOf("Homer"),
                        coverImageUrl = "http://example.com/cover.jpg",
                    ),
                )
            useCase("9780140449136")
            val book = assertNotNull(repo.lastAddedBook)
            assertEquals("9780140449136", book.isbn)
            assertEquals("The Iliad", book.title)
            assertEquals(listOf("Homer"), book.authors)
            assertEquals("http://example.com/cover.jpg", book.coverImageUrl)
        }

    @Test
    fun `invoke returns NotFound when API returns NotFound`() =
        runTest {
            lookup.lookupResult = Result.Failure(BookLookupError.NotFound)
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NotFound, result.error)
        }

    @Test
    fun `invoke returns NetworkError on connectivity failure`() =
        runTest {
            val cause = RuntimeException("no network")
            lookup.lookupResult = Result.Failure(BookLookupError.NetworkError(cause))
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertIs<AddBookError.NetworkError>(result.error)
            assertEquals(cause, result.error.cause)
        }

    @Test
    fun `invoke returns RateLimited immediately on HTTP 429`() =
        runTest {
            lookup.lookupResult = Result.Failure(BookLookupError.RateLimited)
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.RateLimited, result.error)
        }

    @Test
    fun `invoke returns MalformedResponse on bad JSON`() =
        runTest {
            lookup.lookupResult = Result.Failure(BookLookupError.MalformedResponse)
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.MalformedResponse, result.error)
        }
}
