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
    fun `returns LookupByTitleError NotFound when result list is empty`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Success(emptyList())
            val result = useCase("The Iliad")
            assertIs<Result.Failure<LookupByTitleError>>(result)
            assertEquals(LookupByTitleError.NotFound, result.error)
        }

    @Test
    fun `maps BookLookupError NetworkError to LookupByTitleError NetworkError`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.NetworkError)
            val result = useCase("The Iliad")
            assertIs<Result.Failure<LookupByTitleError>>(result)
            assertEquals(LookupByTitleError.NetworkError, result.error)
        }

    @Test
    fun `maps BookLookupError RateLimited to LookupByTitleError RateLimited`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.RateLimited)
            val result = useCase("The Iliad")
            assertIs<Result.Failure<LookupByTitleError>>(result)
            assertEquals(LookupByTitleError.RateLimited, result.error)
        }

    @Test
    fun `maps BookLookupError MalformedResponse to LookupByTitleError MalformedResponse`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.MalformedResponse)
            val result = useCase("The Iliad")
            assertIs<Result.Failure<LookupByTitleError>>(result)
            assertEquals(LookupByTitleError.MalformedResponse, result.error)
        }

    @Test
    fun `maps BookLookupError NotFound to LookupByTitleError NotFound`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.NotFound)
            val result = useCase("unknown")
            assertIs<Result.Failure<LookupByTitleError>>(result)
            assertEquals(LookupByTitleError.NotFound, result.error)
        }

    @Test
    fun `maps BookLookupError Unauthenticated to LookupByTitleError Unauthenticated`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.Unauthenticated)
            val result = useCase("The Iliad")
            assertIs<Result.Failure<LookupByTitleError>>(result)
            assertEquals(LookupByTitleError.Unauthenticated, result.error)
        }
}
