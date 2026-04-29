package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class SaveManualBookUseCaseTest {
    private val repo = FakeBookRepository()
    private val useCase = SaveManualBookUseCase(repo)

    @Test
    fun `invoke creates book with null isbn and null coverImageUrl`() =
        runTest {
            useCase("The Odyssey", "Homer")
            val book = assertNotNull(repo.lastAddedBook)
            assertNull(book.isbn)
            assertNull(book.coverImageUrl)
        }

    @Test
    fun `invoke creates book with authors list containing the provided author`() =
        runTest {
            useCase("The Odyssey", "Homer")
            val book = assertNotNull(repo.lastAddedBook)
            assertEquals(listOf("Homer"), book.authors)
        }

    @Test
    fun `invoke returns Success with the saved book on repository success`() =
        runTest {
            val result = useCase("The Odyssey", "Homer")
            assertIs<Result.Success<Book>>(result)
            assertEquals("fake-id", result.data.id)
            assertEquals("The Odyssey", result.data.title)
        }

    @Test
    fun `invoke returns Failure with SaveFailed when repository fails`() =
        runTest {
            repo.addBookShouldFail = true
            val result = useCase("The Odyssey", "Homer")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertEquals(SaveManualBookError.SaveFailed, result.error)
        }
}
