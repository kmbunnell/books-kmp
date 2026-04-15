package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook

interface BookRepository {
    suspend fun getBooksByUser(): List<Book>

    suspend fun getBookByIsbn(isbn: String): Book?

    suspend fun addBook(book: NewBook): Book

    suspend fun isbnExists(isbn: String?): Boolean
}
