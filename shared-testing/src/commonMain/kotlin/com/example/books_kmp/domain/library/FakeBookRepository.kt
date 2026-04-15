package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook

class FakeBookRepository(
    var addBookShouldThrow: Boolean = false,
) : BookRepository {
    private val books = mutableListOf<Book>()
    var isbnExistsOverride: Boolean? = null
    var lastAddedBook: NewBook? = null
    var addBookCalled = false

    override suspend fun getBooksByUser(): List<Book> = books.toList()

    override suspend fun getBookByIsbn(isbn: String): Book? = books.find { it.isbn == isbn }

    override suspend fun addBook(book: NewBook): Book {
        addBookCalled = true
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

    override suspend fun isbnExists(isbn: String?): Boolean = isbnExistsOverride ?: books.any { it.isbn == isbn }
}
