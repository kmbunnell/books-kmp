package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlinx.coroutines.flow.StateFlow

interface BookRepository {
    val booksFlow: StateFlow<List<Book>?>

    suspend fun getBooksByUser(): Result<List<Book>, BookRepositoryError>

    suspend fun getBookByIsbn(isbn: String): Result<Book?, BookRepositoryError>

    suspend fun getBookById(id: String): Result<Book?, BookRepositoryError>

    suspend fun addBook(book: NewBook): Result<Book, BookRepositoryError>

    suspend fun findDuplicate(
        isbn: String?,
        normalisedTitle: String,
        normalisedAuthors: List<String>,
    ): Result<Book?, BookRepositoryError>

    suspend fun getBookCount(): Result<Int, BookRepositoryError>

    suspend fun deleteBook(bookId: String): Result<Unit, BookRepositoryError>

    fun applyTagDelta(
        bookId: String,
        tagId: String,
        wasApplied: Boolean,
    )
}
