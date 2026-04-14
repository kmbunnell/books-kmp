package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book

class FakeBookRepository : BookRepository {
    private val books = mutableListOf<Book>()
    var isbnExistsOverride: Boolean? = null
    var lastAddedBook: Book? = null

    override suspend fun getBooksByUser(): List<Book> = books.toList()

    override suspend fun getBookByIsbn(isbn: String): Book? = books.find { it.isbn == isbn }

    override suspend fun addBook(book: Book): Book {
        lastAddedBook = book
        val saved = book.copy(id = "fake-id")
        books.add(saved)
        return saved
    }

    override suspend fun isbnExists(isbn: String?): Boolean = isbnExistsOverride ?: books.any { it.isbn == isbn }
}
