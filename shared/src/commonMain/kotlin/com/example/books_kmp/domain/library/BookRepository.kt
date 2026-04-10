package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book

interface BookRepository {
    suspend fun getBooksByUser(): List<Book>

    suspend fun getBookByIsbn(isbn: String): Book?

    suspend fun addBook(book: Book): Book
}
