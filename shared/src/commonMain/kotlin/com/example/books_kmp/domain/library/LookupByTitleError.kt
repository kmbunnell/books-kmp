package com.example.books_kmp.domain.library

sealed interface LookupByTitleError {
    data object NotFound : LookupByTitleError

    data object NetworkError : LookupByTitleError

    data object Unauthenticated : LookupByTitleError

    data object RateLimited : LookupByTitleError

    data object MalformedResponse : LookupByTitleError
}
