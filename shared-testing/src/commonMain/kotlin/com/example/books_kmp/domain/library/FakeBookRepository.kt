package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlinx.coroutines.CompletableDeferred

class FakeBookRepository(
    var addBookShouldThrow: Boolean = false,
    var isbnExistsShouldThrow: Boolean = false,
) : BookRepository {
    private val books = mutableListOf<Book>()
    var isbnExistsOverride: Boolean? = null
    var lastAddedBook: NewBook? = null
    var addBookCalled = false

    // Optional gate — tests set this to suspend addBook until completed,
    // allowing deterministic observation of in-flight ViewModel state.
    var addBookGate: CompletableDeferred<Unit>? = null

    override suspend fun getBooksByUser(): List<Book> = books.toList()

    override suspend fun getBookByIsbn(isbn: String): Book? = books.find { it.isbn == isbn }

    override suspend fun addBook(book: NewBook): Book {
        addBookCalled = true
        addBookGate?.await()
        if (addBookShouldThrow) throw RuntimeException("addBook failed")
        lastAddedBook = book
        val saved =
            Book(
                id = "fake-id",
                isbn = book.isbn,
                title = book.title,
                authors = book.authors,
                coverImageUrl = book.coverImageUrl,
            )
        books.add(saved)
        return saved
    }

    override suspend fun isbnExists(isbn: String?): Boolean {
        if (isbnExistsShouldThrow) throw RuntimeException("isbnExists failed")
        return isbnExistsOverride ?: books.any { it.isbn == isbn }
    }
}
