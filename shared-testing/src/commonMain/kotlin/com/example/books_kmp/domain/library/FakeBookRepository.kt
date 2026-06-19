package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBookRepository(
    var addBookShouldFail: Boolean = false,
    var addBookShouldReturnNotAuthenticated: Boolean = false,
    var getBooksShouldFail: Boolean = false,
    var getBookByIdShouldFail: Boolean = false,
    var getBookCountShouldFail: Boolean = false,
) : BookRepository {
    private val books = mutableListOf<Book>()
    private val _booksFlow = MutableStateFlow<List<Book>?>(null)
    override val booksFlow: StateFlow<List<Book>?> = _booksFlow.asStateFlow()

    var lastAddedBook: NewBook? = null
    var addBookCalled = false
    var getBooksByUserCalled = 0
    var applyTagDeltaCalled = 0
    var lastApplyTagDeltaArgs: Triple<String, String, Boolean>? = null

    var addBookGate: CompletableDeferred<Unit>? = null
    var getBooksByUserGate: CompletableDeferred<Unit>? = null
    var deleteBookGate: CompletableDeferred<Unit>? = null
    var deleteBookShouldFail = false
    var deleteBookCalled = 0
    var findDuplicateShouldFail = false

    fun seedBooks(vararg booksToSeed: Book) {
        books.addAll(booksToSeed)
    }

    fun setBooksFlow(booksToSet: List<Book>) {
        _booksFlow.value = booksToSet
    }

    override suspend fun getBooksByUser(): Result<List<Book>, BookRepositoryError> {
        getBooksByUserCalled++
        getBooksByUserGate?.await()
        return if (getBooksShouldFail) {
            Result.Failure(BookRepositoryError.NetworkError)
        } else {
            val result = books.toList()
            _booksFlow.value = result
            Result.Success(result)
        }
    }

    override fun applyTagDelta(
        bookId: String,
        tagId: String,
        wasApplied: Boolean,
    ) {
        applyTagDeltaCalled++
        lastApplyTagDeltaArgs = Triple(bookId, tagId, wasApplied)
        val current = _booksFlow.value ?: return
        _booksFlow.value =
            current.map { book ->
                if (book.id != bookId) {
                    book
                } else {
                    book.copy(tags = if (wasApplied) book.tags - tagId else book.tags + tagId)
                }
            }
    }

    override suspend fun getBookByIsbn(isbn: String): Result<Book?, BookRepositoryError> =
        Result.Success(books.find { it.isbn == isbn })

    override suspend fun getBookById(id: String): Result<Book?, BookRepositoryError> {
        if (getBookByIdShouldFail) return Result.Failure(BookRepositoryError.NetworkError)
        return Result.Success(books.find { it.id == id })
    }

    override suspend fun addBook(book: NewBook): Result<Book, BookRepositoryError> {
        addBookCalled = true
        addBookGate?.await()
        if (addBookShouldReturnNotAuthenticated) return Result.Failure(BookRepositoryError.NotAuthenticated)
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

    override suspend fun deleteBook(bookId: String): Result<Unit, BookRepositoryError> {
        deleteBookCalled++
        deleteBookGate?.await()
        if (deleteBookShouldFail) return Result.Failure(BookRepositoryError.NetworkError)
        books.removeAll { it.id == bookId }
        _booksFlow.value = books.toList()
        return Result.Success(Unit)
    }

    override suspend fun findDuplicate(
        isbn: String?,
        normalisedTitle: String,
        normalisedAuthors: List<String>,
    ): Result<Book?, BookRepositoryError> {
        if (findDuplicateShouldFail) return Result.Failure(BookRepositoryError.NetworkError)
        val match = books.find { matchesDuplicate(it, isbn, normalisedTitle, normalisedAuthors) }
        return Result.Success(match)
    }

    override suspend fun getBookCount(): Result<Int, BookRepositoryError> {
        if (getBookCountShouldFail) return Result.Failure(BookRepositoryError.NetworkError)
        return Result.Success(books.size)
    }
}
