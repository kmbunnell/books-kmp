package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.BookLookupData

sealed interface AddBookError {
    data object NotFound : AddBookError

    data class Duplicate(val lookupData: BookLookupData) : AddBookError

    data class DuplicateTitle(val existingTitle: String) : AddBookError

    data object NetworkError : AddBookError

    data object RateLimited : AddBookError

    data object MalformedResponse : AddBookError
}
