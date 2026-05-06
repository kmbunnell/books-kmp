package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError

class FakeBookLookupService : BookLookupService {
    var lookupResult: Result<BookLookupData, BookLookupError> =
        Result.Failure(BookLookupError.NotFound)
    var lookupCalled = false
    var lookupByTitleResult: Result<List<BookLookupData>, BookLookupError> =
        Result.Failure(BookLookupError.NotFound)

    override suspend fun lookupByIsbn(isbn: String): Result<BookLookupData, BookLookupError> {
        lookupCalled = true
        return lookupResult
    }

    override suspend fun lookupByTitle(title: String): Result<List<BookLookupData>, BookLookupError> =
        lookupByTitleResult
}
