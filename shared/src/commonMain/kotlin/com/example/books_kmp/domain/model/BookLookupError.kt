package com.example.books_kmp.domain.model

sealed interface BookLookupError {
    data object NotFound : BookLookupError

    data object NetworkError : BookLookupError

    data object RateLimited : BookLookupError

    data object MalformedResponse : BookLookupError

    data object Unauthenticated : BookLookupError
}
