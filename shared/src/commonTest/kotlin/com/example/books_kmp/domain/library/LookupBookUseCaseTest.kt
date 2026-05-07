package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LookupBookUseCaseTest {
    private val repo = FakeBookRepository()
    private val lookup = FakeBookLookupService()
    private val useCase = LookupBookUseCase(repo, lookup)

    private val iliadLookupData =
        BookLookupData(
            isbn = "9780140449136",
            title = "The Iliad",
            authors = listOf("Homer"),
            coverImageUrl = null,
        )

    @Test
    fun `invoke returns Duplicate with lookup data and still calls API when isbn already exists`() =
        runTest {
            repo.isbnExistsOverride = true
            lookup.lookupResult = Result.Success(iliadLookupData)
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            val error = assertIs<AddBookError.Duplicate>(result.error)
            assertEquals(iliadLookupData, error.lookupData)
            assertTrue(lookup.lookupCalled)
        }

    @Test
    fun `invoke returns API error when isbn already exists but API fails`() =
        runTest {
            repo.isbnExistsOverride = true
            lookup.lookupResult = Result.Failure(BookLookupError.NotFound)
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NotFound, result.error)
        }

    @Test
    fun `invoke returns Success with BookLookupData for new valid ISBN`() =
        runTest {
            lookup.lookupResult = Result.Success(iliadLookupData)
            val result = useCase("9780140449136")
            assertIs<Result.Success<BookLookupData>>(result)
            assertEquals(iliadLookupData, result.data)
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
            lookup.lookupResult = Result.Failure(BookLookupError.NetworkError)
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NetworkError, result.error)
        }

    @Test
    fun `invoke returns RateLimited on HTTP 429`() =
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

    @Test
    fun `invoke returns NetworkError when isbnExists fails`() =
        runTest {
            repo.isbnExistsShouldFail = true
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertIs<AddBookError.NetworkError>(result.error)
        }
}
