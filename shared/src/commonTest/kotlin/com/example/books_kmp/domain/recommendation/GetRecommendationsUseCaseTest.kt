package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.FakeEntitlementState
import com.example.books_kmp.domain.library.FakeBookLookupService
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GetRecommendationsUseCaseTest {
    private fun book(
        id: String,
        tags: List<String> = emptyList(),
    ): Book =
        Book(
            id = id,
            isbn = "isbn-$id",
            title = "Title $id",
            authors = listOf("Author $id"),
            coverImageUrl = null,
            tags = tags,
        )

    private val rec1 =
        BookRecommendation(
            title = "Rec 1",
            authors = listOf("A"),
            isbn = null,
            coverUrl = null,
            reason = "because",
            description = "desc",
        )
    private val rec2 = rec1.copy(title = "Rec 2")

    private fun useCase(
        entitlementState: FakeEntitlementState,
        bookRepository: FakeBookRepository,
        recommendationRepository: FakeRecommendationRepository,
    ) = GetRecommendationsUseCase(
        entitlementState = entitlementState,
        bookRepository = bookRepository,
        recommendationRepository = recommendationRepository,
        bookLookupService = FakeBookLookupService(),
    )

    @Test
    fun `not premium returns Failure NotPremium without calling repo`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(false) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1", listOf("sci-fi")))) }
            val recRepo = FakeRecommendationRepository()

            val result = useCase(entitlement, bookRepo, recRepo).invoke(emptyList())

            assertEquals(Result.Failure(RecommendationError.NotPremium), result)
            assertEquals(0, recRepo.getRecommendationsCalled)
        }

    @Test
    fun `null booksFlow returns Failure NoResults`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository()
            val recRepo = FakeRecommendationRepository()

            val result = useCase(entitlement, bookRepo, recRepo).invoke(emptyList())

            assertEquals(Result.Failure(RecommendationError.NoResults), result)
            assertEquals(0, recRepo.getRecommendationsCalled)
        }

    @Test
    fun `tag filter with no matches returns Failure NoResults`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo =
                FakeBookRepository().apply {
                    setBooksFlow(listOf(book("1", listOf("sci-fi")), book("2", listOf("sci-fi"))))
                }
            val recRepo = FakeRecommendationRepository()

            val result = useCase(entitlement, bookRepo, recRepo).invoke(listOf("mystery"))

            assertEquals(Result.Failure(RecommendationError.NoResults), result)
            assertEquals(0, recRepo.getRecommendationsCalled)
        }

    @Test
    fun `empty selectedTagIds uses all books`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val books = listOf(book("1", listOf("sci-fi")), book("2", listOf("mystery")), book("3"))
            val bookRepo = FakeBookRepository().apply { setBooksFlow(books) }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(listOf(rec1)) }

            val result = useCase(entitlement, bookRepo, recRepo).invoke(emptyList())

            assertEquals(Result.Success(listOf(rec1)), result)
            assertEquals(books, recRepo.lastSeedBooks)
        }

    @Test
    fun `tag filter passes only matching books`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val sciFi1 = book("1", listOf("sci-fi"))
            val sciFi2 = book("2", listOf("sci-fi", "mystery"))
            val books =
                listOf(
                    sciFi1,
                    sciFi2,
                    book("3", listOf("mystery")),
                    book("4", listOf("romance")),
                    book("5"),
                )
            val bookRepo = FakeBookRepository().apply { setBooksFlow(books) }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(listOf(rec1)) }

            val result = useCase(entitlement, bookRepo, recRepo).invoke(listOf("sci-fi"))

            assertTrue(result is Result.Success)
            assertEquals(listOf(sciFi1, sciFi2), recRepo.lastSeedBooks)
        }

    @Test
    fun `caps seed books at MAX when count exceeds limit`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val books = (1..21).map { book(it.toString(), listOf("sci-fi")) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(books) }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(listOf(rec1)) }

            val result = useCase(entitlement, bookRepo, recRepo).invoke(listOf("sci-fi"))

            assertTrue(result is Result.Success)
            assertEquals(20, recRepo.lastSeedBooks?.size)
        }

    @Test
    fun `small library passes through without trimming`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val books = listOf(book("1"), book("2"), book("3"))
            val bookRepo = FakeBookRepository().apply { setBooksFlow(books) }
            val recRepo =
                FakeRecommendationRepository().apply { nextResult = Result.Success(listOf(rec1, rec2)) }

            val result = useCase(entitlement, bookRepo, recRepo).invoke(emptyList())

            assertEquals(Result.Success(listOf(rec1, rec2)), result)
            assertEquals(3, recRepo.lastSeedBooks?.size)
        }

    @Test
    fun `repo NetworkError is propagated`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val books = (1..5).map { book(it.toString()) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(books) }
            val cause = RuntimeException("boom")
            val recRepo =
                FakeRecommendationRepository().apply {
                    nextResult = Result.Failure(RecommendationError.NetworkError(cause))
                }

            val result = useCase(entitlement, bookRepo, recRepo).invoke(emptyList())

            assertEquals(Result.Failure(RecommendationError.NetworkError(cause)), result)
        }

    @Test
    fun `repo NoResults is propagated`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val books = (1..5).map { book(it.toString()) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(books) }
            val recRepo =
                FakeRecommendationRepository().apply { nextResult = Result.Failure(RecommendationError.NoResults) }

            val result = useCase(entitlement, bookRepo, recRepo).invoke(emptyList())

            assertEquals(Result.Failure(RecommendationError.NoResults), result)
        }
}
