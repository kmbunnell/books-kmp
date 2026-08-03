package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.FakeEntitlementState
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class AddBookUseCaseTest {
    private val repo = FakeBookRepository()
    private val entitlementState = FakeEntitlementState()
    private val useCase = AddBookUseCase(repo, entitlementState)

    private fun seedBooks(count: Int) {
        repeat(count) { index ->
            repo.seedBooks(
                Book(
                    id = "seed-$index",
                    isbn = "isbn-$index",
                    title = "Seed $index",
                    authors = listOf("Author"),
                    coverImageUrl = null,
                ),
            )
        }
    }

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

    @Test
    fun `invoke returns Unauthenticated when repository returns NotAuthenticated`() =
        runTest {
            repo.addBookShouldReturnNotAuthenticated = true
            val result = useCase(lookupData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.Unauthenticated, result.error)
        }

    @Test
    fun `isbn is null and no duplicate title — inserts successfully`() =
        runTest {
            val noIsbnData =
                BookLookupData(isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null)
            val result = useCase(noIsbnData)
            assertIs<Result.Success<*>>(result)
        }

    @Test
    fun `isbn is null and duplicate title plus author exists — returns DuplicateBook error`() =
        runTest {
            repo.seedBooks(
                Book(
                    id = "existing-id",
                    isbn = null,
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = null,
                ),
            )
            val noIsbnData =
                BookLookupData(isbn = null, title = "The Iliad", authors = listOf("Homer"), coverImageUrl = null)
            val result = useCase(noIsbnData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.DuplicateBook, result.error)
        }

    @Test
    fun `isbn is null and duplicate title exists and forceAdd true — inserts successfully`() =
        runTest {
            repo.seedBooks(
                Book(
                    id = "existing-id",
                    isbn = null,
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = null,
                ),
            )
            val noIsbnData =
                BookLookupData(isbn = null, title = "The Iliad", authors = listOf("Homer"), coverImageUrl = null)
            val result = useCase(noIsbnData, forceAdd = true)
            assertIs<Result.Success<*>>(result)
        }

    @Test
    fun `isbn is null and duplicate lookup fails — returns NetworkError`() =
        runTest {
            repo.findDuplicateShouldFail = true
            val noIsbnData =
                BookLookupData(isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null)
            val result = useCase(noIsbnData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NetworkError, result.error)
        }

    @Test
    fun `isbn is non-null and stored isbn null but title and author match — returns DuplicateBook`() =
        runTest {
            repo.seedBooks(
                Book(
                    id = "existing-id",
                    isbn = null,
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = null,
                ),
            )
            val result = useCase(lookupData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.DuplicateBook, result.error)
        }

    @Test
    fun `isbn is non-null and full match on isbn plus title plus author — returns DuplicateBook`() =
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
            val result = useCase(lookupData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.DuplicateBook, result.error)
        }

    @Test
    fun `isbn is non-null and same isbn but author mismatch — still returns DuplicateBook`() =
        runTest {
            repo.seedBooks(
                Book(
                    id = "existing-id",
                    isbn = "9780140449136",
                    title = "The Iliad",
                    authors = listOf("Virgil"),
                    coverImageUrl = null,
                ),
            )
            val result = useCase(lookupData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.DuplicateBook, result.error)
        }

    @Test
    fun `free user at cap of 25 books — returns LibraryLimitReached`() =
        runTest {
            seedBooks(25)
            val result = useCase(lookupData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.LibraryLimitReached, result.error)
        }

    @Test
    fun `free user under cap of 24 books — proceeds normally`() =
        runTest {
            seedBooks(24)
            val result = useCase(lookupData)
            assertIs<Result.Success<*>>(result)
        }

    @Test
    fun `premium user at or over cap — proceeds normally`() =
        runTest {
            seedBooks(25)
            entitlementState.setIsPremium(true)
            val result = useCase(lookupData)
            assertIs<Result.Success<*>>(result)
        }

    @Test
    fun `getBookCount fails — returns NetworkError`() =
        runTest {
            repo.getBookCountShouldFail = true
            val result = useCase(lookupData)
            assertIs<Result.Failure<AddBookError>>(result)
            assertEquals(AddBookError.NetworkError, result.error)
        }
}
