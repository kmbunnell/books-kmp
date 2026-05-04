package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook

interface BookRepository {
    suspend fun getBooksByUser(): Result<List<Book>, BookRepositoryError>

    suspend fun getBookByIsbn(isbn: String): Result<Book?, BookRepositoryError>

    suspend fun getBookById(id: String): Result<Book?, BookRepositoryError>

    suspend fun addBook(book: NewBook): Result<Book, BookRepositoryError>

    suspend fun isbnExists(isbn: String?): Result<Boolean, BookRepositoryError>
}
