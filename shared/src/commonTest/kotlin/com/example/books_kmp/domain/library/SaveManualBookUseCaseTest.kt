package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            useCase("The Odyssey", "Homer", isbn = null)
            val book = assertNotNull(repo.lastAddedBook)
            assertNull(book.isbn)
            assertNull(book.coverImageUrl)
        }

    @Test
    fun `invoke stores provided 13-digit isbn in NewBook`() =
        runTest {
            useCase("Title", "Author", isbn = "9781234567890")
            assertEquals("9781234567890", repo.lastAddedBook?.isbn)
        }

    @Test
    fun `invoke stores provided 10-digit isbn in NewBook`() =
        runTest {
            useCase("Title", "Author", isbn = "1234567890")
            assertEquals("1234567890", repo.lastAddedBook?.isbn)
        }

    @Test
    fun `invoke stores null isbn when blank string passed`() =
        runTest {
            useCase("Title", "Author", isbn = "")
            assertNull(repo.lastAddedBook?.isbn)
        }

    @Test
    fun `invoke returns InvalidIsbn for 9-digit isbn`() =
        runTest {
            val result = useCase("Title", "Author", isbn = "123456789")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertEquals(SaveManualBookError.InvalidIsbn, result.error)
            assertFalse(repo.addBookCalled)
        }

    @Test
    fun `invoke returns InvalidIsbn for 14-digit isbn`() =
        runTest {
            val result = useCase("Title", "Author", isbn = "12345678901234")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertEquals(SaveManualBookError.InvalidIsbn, result.error)
            assertFalse(repo.addBookCalled)
        }

    @Test
    fun `invoke returns InvalidIsbn for 11-char isbn with hyphens`() =
        runTest {
            val result = useCase("Title", "Author", isbn = "978-1234-567")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertEquals(SaveManualBookError.InvalidIsbn, result.error)
            assertFalse(repo.addBookCalled)
        }

    @Test
    fun `invoke stores 10-char isbn ending in X`() =
        runTest {
            useCase("Title", "Author", isbn = "030640615X")
            assertEquals("030640615X", repo.lastAddedBook?.isbn)
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

    @Test
    fun `invoke returns DuplicateTitle when a book with the same title already exists`() =
        runTest {
            repo.seedBooks(
                Book(id = "1", isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null)
            )
            val result = useCase("The Odyssey", "Homer")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertIs<SaveManualBookError.DuplicateTitle>(result.error)
            assertFalse(repo.addBookCalled)
        }

    @Test
    fun `invoke with forceAdd skips duplicate check and saves the book`() =
        runTest {
            repo.seedBooks(
                Book(id = "1", isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null)
            )
            val result = useCase("The Odyssey", "Homer", forceAdd = true)
            assertIs<Result.Success<Book>>(result)
            assertNotNull(repo.lastAddedBook)
        }

    @Test
    fun `invoke returns DuplicateTitle for case-insensitive title match`() =
        runTest {
            repo.seedBooks(
                Book(id = "1", isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null)
            )
            val result = useCase("the odyssey", "Homer")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertIs<SaveManualBookError.DuplicateTitle>(result.error)
            assertFalse(repo.addBookCalled)
        }

    @Test
    fun `invoke returns DuplicateTitle for title that matches after diacritic normalisation`() =
        runTest {
            repo.seedBooks(
                Book(id = "1", isbn = null, title = "Resume", authors = listOf("Author"), coverImageUrl = null)
            )
            val result = useCase("Résumé", "Author")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertIs<SaveManualBookError.DuplicateTitle>(result.error)
            assertFalse(repo.addBookCalled)
        }

    @Test
    fun `invoke returns SaveFailed when title lookup fails`() =
        runTest {
            repo.findDuplicateShouldFail = true
            val result = useCase("The Odyssey", "Homer")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertEquals(SaveManualBookError.SaveFailed, result.error)
            assertFalse(repo.addBookCalled)
        }

    @Test
    fun `invoke returns DuplicateTitle when entry matches stored title after punctuation stripping`() =
        runTest {
            repo.seedBooks(
                Book(id = "1", isbn = null, title = "So B. It", authors = listOf("Sarah Weeks"), coverImageUrl = null)
            )
            val result = useCase("so b it", "Sarah Weeks")
            assertIs<Result.Failure<SaveManualBookError>>(result)
            assertIs<SaveManualBookError.DuplicateTitle>(result.error)
            assertFalse(repo.addBookCalled)
        }
}
