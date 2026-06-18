package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.domain.MAX_RECOMMENDATION_SEED_BOOKS
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.EntitlementState
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.library.BookRepository

class GetRecommendationsUseCase(
    private val entitlementState: EntitlementState,
    private val bookRepository: BookRepository,
    private val recommendationRepository: RecommendationRepository,
    @Suppress("unused") private val bookLookupService: BookLookupService,
) {
    suspend operator fun invoke(selectedTagIds: List<String>): Result<List<BookRecommendation>, RecommendationError> {
        if (!entitlementState.isPremium.value) {
            return Result.Failure(RecommendationError.NotPremium)
        }

        val library = bookRepository.booksFlow.value.orEmpty()

        val filtered =
            if (selectedTagIds.isEmpty()) {
                library
            } else {
                library.filter { book -> book.tags.any { it in selectedTagIds } }
            }

        if (filtered.isEmpty()) {
            return Result.Failure(RecommendationError.NoResults)
        }

        val seedBooks =
            if (filtered.size > MAX_RECOMMENDATION_SEED_BOOKS) {
                filtered.shuffled().take(MAX_RECOMMENDATION_SEED_BOOKS)
            } else {
                filtered
            }

        return recommendationRepository.getRecommendations(seedBooks)
    }
}
