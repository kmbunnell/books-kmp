package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.FakeEntitlementState
import com.example.books_kmp.domain.library.FakeBookLookupService
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import com.example.books_kmp.testing.TEST_INSTANT
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
            updatedAt = TEST_INSTANT,
        )

    private val rec1 =
        BookRecommendation(
            title = "Rec 1",
            authors = listOf("Author Rec 1"),
            isbn = "isbn-rec1",
            coverUrl = "url-rec1",
            reason = "because",
            description = "desc",
        )
    private val rec2 =
        rec1.copy(
            title = "Rec 2",
            authors = listOf("Author Rec 2"),
            isbn = "isbn-rec2",
            coverUrl = "url-rec2",
        )

    private fun lookupFor(rec: BookRecommendation): BookLookupData =
        BookLookupData(
            isbn = rec.isbn,
            title = rec.title,
            authors = rec.authors,
            coverImageUrl = rec.coverUrl,
        )

    private fun useCase(
        entitlementState: FakeEntitlementState,
        bookRepository: FakeBookRepository,
        recommendationRepository: FakeRecommendationRepository,
        bookLookupService: FakeBookLookupService = FakeBookLookupService(),
    ) = GetRecommendationsUseCase(
        entitlementState = entitlementState,
        bookRepository = bookRepository,
        recommendationRepository = recommendationRepository,
        bookLookupService = bookLookupService,
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
            val lookup =
                FakeBookLookupService().apply {
                    lookupByTitleQueue.add(Result.Success(listOf(lookupFor(rec1))))
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

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
            val lookup =
                FakeBookLookupService().apply {
                    lookupByTitleQueue.add(Result.Success(listOf(lookupFor(rec1))))
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(listOf("sci-fi"))

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
            val lookup =
                FakeBookLookupService().apply {
                    lookupByTitleQueue.add(Result.Success(listOf(lookupFor(rec1))))
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(listOf("sci-fi"))

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
            val lookup =
                FakeBookLookupService().apply {
                    lookupByTitleQueue.add(Result.Success(listOf(lookupFor(rec1))))
                    lookupByTitleQueue.add(Result.Success(listOf(lookupFor(rec2))))
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertEquals(Result.Success(listOf(rec1, rec2)), result)
            assertEquals(3, recRepo.lastSeedBooks?.size)
        }

    @Test
    fun `title lookup always attempted — failure includes candidate without cover`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val noIsbnRec = rec1.copy(isbn = null, coverUrl = null)
            val recRepo =
                FakeRecommendationRepository().apply {
                    nextResult = Result.Success(listOf(noIsbnRec))
                }
            val lookup = FakeBookLookupService()

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertEquals(Result.Success(listOf(noIsbnRec)), result)
            assertEquals(listOf("Rec 1"), lookup.lookupByTitleCalledWith)
        }

    @Test
    fun `candidate isbn already in library is skipped — no lookup called`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val recRepo =
                FakeRecommendationRepository().apply {
                    nextResult = Result.Success(listOf(rec1.copy(isbn = "isbn-1")))
                }
            val lookup = FakeBookLookupService()

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertEquals(Result.Success(emptyList()), result)
            assertTrue(lookup.lookupByTitleCalledWith.isEmpty())
        }

    @Test
    fun `candidate title already in library is skipped — no lookup called`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val recRepo =
                FakeRecommendationRepository().apply {
                    nextResult = Result.Success(listOf(rec1.copy(title = "  title 1  ")))
                }
            val lookup = FakeBookLookupService()

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertEquals(Result.Success(emptyList()), result)
            assertTrue(lookup.lookupByTitleCalledWith.isEmpty())
        }

    @Test
    fun `title lookup Failure includes candidate without cover`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(listOf(rec1)) }
            val lookup =
                FakeBookLookupService().apply {
                    lookupByTitleQueue.add(Result.Failure(BookLookupError.NotFound))
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertEquals(Result.Success(listOf(rec1.copy(isbn = null, coverUrl = null))), result)
            assertEquals(listOf("Rec 1"), lookup.lookupByTitleCalledWith)
        }

    @Test
    fun `title lookup cover replaces candidate cover`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(listOf(rec1)) }
            val lookupData = lookupFor(rec1).copy(coverImageUrl = "url-from-google")
            val lookup =
                FakeBookLookupService().apply {
                    lookupByTitleQueue.add(Result.Success(listOf(lookupData)))
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertEquals(Result.Success(listOf(rec1.copy(coverUrl = "url-from-google"))), result)
        }

    @Test
    fun `early exit — 4th and 5th candidates not looked up`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val candidates =
                (1..5).map { i ->
                    rec1.copy(title = "Cand $i", isbn = "isbn-cand$i", coverUrl = "url-cand$i")
                }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(candidates) }
            val lookup =
                FakeBookLookupService().apply {
                    candidates.forEach { lookupByTitleQueue.add(Result.Success(listOf(lookupFor(it)))) }
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertTrue(result is Result.Success)
            assertEquals(3, result.data.size)
            assertEquals(3, lookup.lookupByTitleCalledWith.size)
        }

    @Test
    fun `happy path — 3 enriched results`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val candidates =
                (1..3).map { i ->
                    rec1.copy(title = "Cand $i", isbn = "isbn-cand$i", coverUrl = "url-cand$i")
                }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(candidates) }
            val lookup =
                FakeBookLookupService().apply {
                    candidates.forEach { lookupByTitleQueue.add(Result.Success(listOf(lookupFor(it)))) }
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            assertEquals(Result.Success(candidates), result)
        }

    @Test
    fun `all candidates fail lookup — returns Success with candidates without covers`() =
        runTest {
            val entitlement = FakeEntitlementState().apply { setIsPremium(true) }
            val bookRepo = FakeBookRepository().apply { setBooksFlow(listOf(book("1"))) }
            val candidates =
                (1..3).map { i -> rec1.copy(title = "Cand $i", isbn = "isbn-cand$i", coverUrl = null) }
            val recRepo = FakeRecommendationRepository().apply { nextResult = Result.Success(candidates) }
            val lookup =
                FakeBookLookupService().apply {
                    repeat(3) { lookupByTitleQueue.add(Result.Failure(BookLookupError.NotFound)) }
                }

            val result = useCase(entitlement, bookRepo, recRepo, lookup).invoke(emptyList())

            val expected = candidates.map { it.copy(isbn = null) }
            assertEquals(Result.Success(expected), result)
            assertEquals(3, lookup.lookupByTitleCalledWith.size)
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
