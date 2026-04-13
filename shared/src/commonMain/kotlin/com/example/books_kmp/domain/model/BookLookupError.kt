package com.example.books_kmp.domain.model

sealed interface BookLookupError {
    data object NotFound : BookLookupError

    data class NetworkError(val cause: Throwable) : BookLookupError

    data object RateLimited : BookLookupError

    data object MalformedResponse : BookLookupError
}
