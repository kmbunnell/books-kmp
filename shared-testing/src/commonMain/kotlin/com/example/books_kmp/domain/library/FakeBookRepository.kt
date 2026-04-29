package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.LoadBooksError
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlinx.coroutines.CompletableDeferred

class FakeBookRepository(
    var addBookShouldThrow: Boolean = false,
    var isbnExistsShouldThrow: Boolean = false,
    var getBooksShouldThrow: Boolean = false,
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

    override suspend fun getBooksByUser(): Result<List<Book>, LoadBooksError> {
        getBooksByUserCalled++
        getBooksByUserGate?.await()
        return if (getBooksShouldThrow)
            Result.Failure(LoadBooksError.NetworkError(RuntimeException("getBooksByUser failed")))
        else
            Result.Success(books.toList())
    }

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
