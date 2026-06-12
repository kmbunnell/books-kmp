package com.example.books_kmp.domain.entitlement

sealed interface EntitlementError {
    data object NotAuthenticated : EntitlementError

    data object NetworkError : EntitlementError
}
