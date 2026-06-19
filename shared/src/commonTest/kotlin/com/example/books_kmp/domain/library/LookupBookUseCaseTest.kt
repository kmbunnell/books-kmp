package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class LookupBookUseCaseTest {
    private val repo = FakeBookRepository()
    private val lookup = FakeBookLookupService()
    private val useCase = LookupBookUseCase( lookup)

    private val iliadLookupData =
        BookLookupData(
            isbn = "9780140449136",
            title = "The Iliad",
            authors = listOf("Homer"),
            coverImageUrl = null,
        )

    @Test
    fun `invoke returns Success even when isbn is already in library`() =
        runTest {
            repo.seedBooks(
                Book(
                    id = "existing-id",
                    isbn = "9780140449136",
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = null,
                ),
            )
            lookup.lookupResult = Result.Success(iliadLookupData)
            val result = useCase("9780140449136")
            assertIs<Result.Success<BookLookupData>>(result)
            assertEquals(iliadLookupData, result.data)
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
    fun `invoke returns Unauthenticated when API returns Unauthenticated`() =
        runTest {
            lookup.lookupResult = Result.Failure(BookLookupError.Unauthenticated)
            val result = useCase("9780140449136")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.Unauthenticated, result.error)
        }
}
