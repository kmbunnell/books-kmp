package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlinx.coroutines.CompletableDeferred

class FakeBookRepository(
    var addBookShouldFail: Boolean = false,
    var isbnExistsShouldFail: Boolean = false,
    var getBooksShouldFail: Boolean = false,
) : BookRepository {
    private val books = mutableListOf<Book>()
    var isbnExistsOverride: Boolean? = null
    var lastAddedBook: NewBook? = null
    var addBookCalled = false
    var getBooksByUserCalled = 0

    var addBookGate: CompletableDeferred<Unit>? = null
    var getBooksByUserGate: CompletableDeferred<Unit>? = null

    fun seedBooks(vararg booksToSeed: Book) {
        books.addAll(booksToSeed)
    }

    override suspend fun getBooksByUser(): Result<List<Book>, BookRepositoryError> {
        getBooksByUserCalled++
        getBooksByUserGate?.await()
        return if (getBooksShouldFail) {
            Result.Failure(BookRepositoryError.NetworkError)
        } else {
            Result.Success(books.toList())
        }
    }

    override suspend fun getBookByIsbn(isbn: String): Result<Book?, BookRepositoryError> =
        Result.Success(books.find { it.isbn == isbn })

    override suspend fun addBook(book: NewBook): Result<Book, BookRepositoryError> {
        addBookCalled = true
        addBookGate?.await()
        if (addBookShouldFail) return Result.Failure(BookRepositoryError.NetworkError)
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
        return Result.Success(saved)
    }

    override suspend fun isbnExists(isbn: String?): Result<Boolean, BookRepositoryError> {
        if (isbnExistsShouldFail) return Result.Failure(BookRepositoryError.NetworkError)
        return Result.Success(isbnExistsOverride ?: books.any { it.isbn == isbn })
    }
}
