package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.domain.MAX_DISPLAYED_RECOMMENDATIONS
import com.example.books_kmp.domain.MAX_RECOMMENDATION_SEED_BOOKS
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.EntitlementState
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.model.BookLookupData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

        // Filter out books already in the library, then cap at display limit.
        val toEnrich = candidates
            .filter { candidate ->
                val isbn = candidate.isbn
                candidate.title.trim().lowercase() !in libraryTitleSet &&
                    (isbn == null || isbn !in libraryIsbnSet)
            }
            .take(MAX_DISPLAYED_RECOMMENDATIONS)

        // Look up cover images for all candidates in parallel.
        // Try ISBN first; fall back to title search if the ISBN is missing or not found.
        // If both fail, include the recommendation without a cover rather than dropping it.
        val results = coroutineScope {
            toEnrich.map { candidate ->
                async {
                    val lookupData = lookupMetadata(candidate)
                    candidate.copy(
                        isbn = lookupData?.isbn ?: candidate.isbn,
                        coverUrl = lookupData?.coverImageUrl,
                        authors = lookupData?.authors ?: candidate.authors,
                    )
                }
            }.awaitAll()
        }

        return Result.Success(results)
    }

    private suspend fun lookupMetadata(candidate: BookRecommendation): BookLookupData? {
        return when (val result = bookLookupService.lookupByTitle(candidate.title)) {
            is Result.Success -> result.data.firstOrNull()
            is Result.Failure -> null
        }
    }
}
