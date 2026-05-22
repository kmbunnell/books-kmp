package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.BookLookupError

fun BookLookupError.toAddBookError(): AddBookError =
    when (this) {
        BookLookupError.NotFound -> AddBookError.NotFound
        BookLookupError.NetworkError -> AddBookError.NetworkError
        BookLookupError.Unauthenticated -> AddBookError.Unauthenticated
        BookLookupError.RateLimited -> AddBookError.RateLimited
        BookLookupError.MalformedResponse -> AddBookError.MalformedResponse
    }

fun BookLookupError.toLookupByTitleError(): LookupByTitleError =
    when (this) {
        BookLookupError.NotFound -> LookupByTitleError.NotFound
        BookLookupError.NetworkError -> LookupByTitleError.NetworkError
        BookLookupError.Unauthenticated -> LookupByTitleError.Unauthenticated
        BookLookupError.RateLimited -> LookupByTitleError.RateLimited
        BookLookupError.MalformedResponse -> LookupByTitleError.MalformedResponse
    }
