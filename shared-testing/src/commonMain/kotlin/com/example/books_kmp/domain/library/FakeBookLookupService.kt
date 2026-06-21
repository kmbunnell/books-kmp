package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError

class FakeBookLookupService : BookLookupService {
    var lookupResult: Result<BookLookupData, BookLookupError> =
        Result.Failure(BookLookupError.NotFound)
    var lookupCalled = false

    val lookupByIsbnQueue: ArrayDeque<Result<BookLookupData, BookLookupError>> = ArrayDeque()
    val lookupByIsbnCalledWith: MutableList<String> = mutableListOf()

    val lookupByTitleQueue: ArrayDeque<Result<List<BookLookupData>, BookLookupError>> = ArrayDeque()
    val lookupByTitleCalledWith: MutableList<String> = mutableListOf()
    var lookupByTitleResult: Result<List<BookLookupData>, BookLookupError> =
        Result.Failure(BookLookupError.NotFound)

    override suspend fun lookupByIsbn(isbn: String): Result<BookLookupData, BookLookupError> {
        lookupCalled = true
        lookupByIsbnCalledWith.add(isbn)
        return if (lookupByIsbnQueue.isNotEmpty()) {
            lookupByIsbnQueue.removeFirst()
        } else {
            lookupResult
        }
    }

    override suspend fun lookupByTitle(title: String): Result<List<BookLookupData>, BookLookupError> {
        lookupByTitleCalledWith.add(title)
        return if (lookupByTitleQueue.isNotEmpty()) {
            lookupByTitleQueue.removeFirst()
        } else {
            lookupByTitleResult
        }
    }
}
