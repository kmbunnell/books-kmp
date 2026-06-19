package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.domain.MAX_DISPLAYED_RECOMMENDATIONS
import com.example.books_kmp.domain.MAX_RECOMMENDATION_SEED_BOOKS
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.EntitlementState
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.library.BookRepository

class GetRecommendationsUseCase(
    private val entitlementState: EntitlementState,
    private val bookRepository: BookRepository,
    private val recommendationRepository: RecommendationRepository,
    private val bookLookupService: BookLookupService,
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

        val candidates =
            when (val result = recommendationRepository.getRecommendations(seedBooks)) {
                is Result.Success -> result.data
                is Result.Failure -> return result
            }

        val libraryIsbnSet = library.mapNotNull { it.isbn }.toHashSet()
        val libraryTitleSet = library.map { it.title.trim().lowercase() }.toHashSet()

        val results = mutableListOf<BookRecommendation>()
        for (candidate in candidates) {
            if (results.size >= MAX_DISPLAYED_RECOMMENDATIONS) break

            val isbn = candidate.isbn ?: continue
            if (isbn in libraryIsbnSet) continue

            val normalizedTitle = candidate.title.trim().lowercase()
            if (normalizedTitle in libraryTitleSet) continue

            val lookupData =
                when (val lookup = bookLookupService.lookupByIsbn(isbn)) {
                    is Result.Success -> lookup.data
                    is Result.Failure -> continue
                }

            if (lookupData.title.trim().lowercase() != normalizedTitle) continue

            results.add(
                candidate.copy(
                    isbn = lookupData.isbn ?: candidate.isbn,
                    coverUrl = lookupData.coverImageUrl,
                    authors = lookupData.authors,
                ),
            )
        }

        return Result.Success(results)
    }
}
