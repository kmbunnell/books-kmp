package com.example.books_kmp.domain.recommendation

sealed interface RecommendationError {
    data object NotPremium : RecommendationError

    data object NoResults : RecommendationError

    data class NetworkError(val cause: Throwable) : RecommendationError

    data object Unauthenticated : RecommendationError
}
