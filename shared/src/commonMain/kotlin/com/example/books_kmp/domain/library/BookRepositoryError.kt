package com.example.books_kmp.domain.library

sealed interface BookRepositoryError {
    data object NetworkError : BookRepositoryError
}
