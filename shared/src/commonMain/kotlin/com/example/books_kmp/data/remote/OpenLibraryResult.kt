package com.example.books_kmp.data.remote

import com.example.books_kmp.domain.model.BookLookupData

sealed class OpenLibraryResult {
    data class Found(val bookData: BookLookupData) : OpenLibraryResult()
    data object NotFound : OpenLibraryResult()
    data class NetworkError(val cause: Throwable) : OpenLibraryResult()
    data object RateLimited : OpenLibraryResult()
    data object MalformedResponse : OpenLibraryResult()
}
