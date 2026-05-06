package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class LookupByTitleUseCaseTest {
    private val fakeService = FakeBookLookupService()
    private val useCase = LookupByTitleUseCase(fakeService)

    private val bookLookupData =
        BookLookupData(
            isbn = null,
            title = "The Iliad",
            authors = listOf("Homer"),
            coverImageUrl = null,
        )

    @Test
    fun `returns mapped results on success`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Success(listOf(bookLookupData))
            val result = useCase("The Iliad")
            assertIs<Result.Success<List<BookLookupData>>>(result)
            assertEquals(listOf(bookLookupData), result.data)
        }

    @Test
    fun `returns AddBookError NotFound when result list is empty`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Success(emptyList())
            val result = useCase("The Iliad")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NotFound, result.error)
        }

    @Test
    fun `maps BookLookupError NetworkError to AddBookError NetworkError`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.NetworkError)
            val result = useCase("The Iliad")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NetworkError, result.error)
        }

    @Test
    fun `maps BookLookupError RateLimited to AddBookError RateLimited`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.RateLimited)
            val result = useCase("The Iliad")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.RateLimited, result.error)
        }

    @Test
    fun `maps BookLookupError MalformedResponse to AddBookError MalformedResponse`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.MalformedResponse)
            val result = useCase("The Iliad")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.MalformedResponse, result.error)
        }

    @Test
    fun `maps BookLookupError NotFound to AddBookError NotFound`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.NotFound)
            val result = useCase("unknown")
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NotFound, result.error)
        }
}
