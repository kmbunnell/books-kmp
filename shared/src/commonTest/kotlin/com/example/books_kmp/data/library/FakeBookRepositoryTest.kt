package com.example.books_kmp.data.library

import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FakeBookRepositoryTest {
    private val books =
        listOf(
            Book(
                id = "1",
                isbn = "9780140449136",
                title = "The Iliad",
                authors = listOf("Homer"),
                coverImageUrl = null,
            ),
            Book(
                id = "2",
                isbn = "9780743273565",
                title = "The Great Gatsby",
                authors = listOf("F. Scott Fitzgerald"),
                coverImageUrl = null,
            ),
            Book(
                id = "3",
                isbn = null,
                title = "Manual Entry Book",
                authors = listOf("Unknown"),
                coverImageUrl = null,
            ),
        )

    private val repository = FakeBookRepository(books)

    @Test
    fun `isbnExists returns true when isbn matches a book in cache`() =
        runTest {
            assertTrue(repository.isbnExists("9780140449136"))
        }

    @Test
    fun `isbnExists returns false when isbn is not in cache`() =
        runTest {
            assertFalse(repository.isbnExists("0000000000000"))
        }

    @Test
    fun `isbnExists returns false when isbn is null`() =
        runTest {
            assertFalse(repository.isbnExists(null))
        }
}
