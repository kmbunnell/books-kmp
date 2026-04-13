package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book

class FakeBookRepository(private val books: List<Book> = emptyList()) : BookRepository {
    override suspend fun isbnExists(isbn: String?): Boolean {
        if (isbn == null) return false
        return books.any { it.isbn == isbn }
    }

    override suspend fun getBooksByUser(): List<Book> = TODO("Not yet implemented")

    override suspend fun getBookByIsbn(isbn: String): Book? = TODO("Not yet implemented")

    override suspend fun addBook(book: Book): Book = TODO("Not yet implemented")
}
