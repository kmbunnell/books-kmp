package com.example.books_kmp.domain.tags

sealed interface TagError {
    data object DuplicateName : TagError

    data class NetworkError(val cause: Throwable) : TagError

    data object NotFound : TagError
}
