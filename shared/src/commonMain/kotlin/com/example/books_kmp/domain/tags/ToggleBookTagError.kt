package com.example.books_kmp.domain.tags

sealed interface ToggleBookTagError {
    data object NetworkError : ToggleBookTagError
}
