package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData

class LookupBookUseCase(
    private val lookupService: BookLookupService,
) {
    suspend operator fun invoke(isbn: String): Result<BookLookupData, AddBookError> =
        when (val result = lookupService.lookupByIsbn(isbn)) {
            is Result.Failure -> Result.Failure(result.error.toAddBookError())
            is Result.Success -> Result.Success(result.data)
        }
}
