package com.example.books_kmp.domain.library

sealed interface AddBookError {
    data object NotFound : AddBookError

    data object Duplicate : AddBookError

    data class NetworkError(val cause: Throwable) : AddBookError

    data object RateLimited : AddBookError

    data object MalformedResponse : AddBookError
}
