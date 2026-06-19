package com.example.books_kmp.domain.library

sealed interface AddBookError {
    data object NotFound : AddBookError

    data object DuplicateBook : AddBookError

    data object NetworkError : AddBookError

    data object Unauthenticated : AddBookError

    data object RateLimited : AddBookError

    data object MalformedResponse : AddBookError

    data object LibraryLimitReached : AddBookError
}
