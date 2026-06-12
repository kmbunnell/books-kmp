package com.example.books_kmp.domain.profile

sealed interface ProfileRepositoryError {
    data object NetworkError : ProfileRepositoryError

    data object NotAuthenticated : ProfileRepositoryError

    data object NotFound : ProfileRepositoryError
}
