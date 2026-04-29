package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class AddBookUseCaseTest {
    private val repo = FakeBookRepository()
    private val useCase = AddBookUseCase(repo)

    private val lookupData =
        BookLookupData(
            isbn = "9780140449136",
            title = "The Iliad",
            authors = listOf("Homer"),
            coverImageUrl = "http://example.com/cover.jpg",
        )

    @Test
    fun `invoke calls addBook with NewBook constructed correctly from BookLookupData`() =
        runTest {
            useCase(lookupData)
            val book = assertNotNull(repo.lastAddedBook)
            assertEquals("9780140449136", book.isbn)
            assertEquals("The Iliad", book.title)
            assertEquals(listOf("Homer"), book.authors)
            assertEquals("http://example.com/cover.jpg", book.coverImageUrl)
        }

    @Test
    fun `invoke returns Success with persisted Book`() =
        runTest {
            val result = useCase(lookupData)
            assertIs<Result.Success<*>>(result)
        }

    @Test
    fun `invoke returns NetworkError when repository fails`() =
        runTest {
            repo.addBookShouldFail = true
            val result = useCase(lookupData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NetworkError, result.error)
        }
}
