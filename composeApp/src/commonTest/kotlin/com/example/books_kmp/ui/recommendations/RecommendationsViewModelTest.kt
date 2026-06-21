package com.example.books_kmp.ui.recommendations

import app.cash.turbine.test
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.FakeEntitlementState
import com.example.books_kmp.domain.library.FakeBookLookupService
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.recommendation.BookRecommendation
import com.example.books_kmp.domain.recommendation.FakeRecommendationRepository
import com.example.books_kmp.domain.recommendation.GetRecommendationsUseCase
import com.example.books_kmp.domain.recommendation.RecommendationError
import com.example.books_kmp.domain.recommendation.RecommendationRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val seedBook =
        Book(
            id = "b1",
            isbn = "111",
            title = "Seed Book",
            authors = listOf("Author"),
            coverImageUrl = null,
            tags = listOf("t1"),
        )

    private val recommendation =
        BookRecommendation(
            title = "Recommended Book",
            authors = listOf("Other Author"),
            isbn = "222",
            coverUrl = "http://cover",
            reason = "Because you liked Seed Book",
            description = "A description",
        )

    private lateinit var entitlementState: FakeEntitlementState
    private lateinit var bookRepository: FakeBookRepository
    private lateinit var recommendationRepository: FakeRecommendationRepository
    private lateinit var lookupService: FakeBookLookupService

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        entitlementState = FakeEntitlementState().apply { setIsPremium(true) }
        bookRepository = FakeBookRepository().apply { setBooksFlow(listOf(seedBook)) }
        recommendationRepository = FakeRecommendationRepository()
        lookupService = FakeBookLookupService()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun useCase(repository: RecommendationRepository = recommendationRepository): GetRecommendationsUseCase =
        GetRecommendationsUseCase(entitlementState, bookRepository, repository, lookupService)

    @Test
    fun `Load emits Loading then transitions to Success`() =
        runTest {
            val gate = CompletableDeferred<Result<List<BookRecommendation>, RecommendationError>>()
            val suspendingRepo =
                object : RecommendationRepository {
                    override suspend fun getRecommendations(
                        books: List<Book>,
                    ): Result<List<BookRecommendation>, RecommendationError> = gate.await()
                }
            val vm = RecommendationsViewModel(listOf("t1"), useCase(suspendingRepo))
            vm.uiState.test {
                assertIs<RecommendationsUiState.Loading>(awaitItem())
                gate.complete(Result.Success(emptyList()))
                assertIs<RecommendationsUiState.Success>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Load success sets Success state with recommendations`() =
        runTest {
            lookupService.lookupResult =
                Result.Success(
                    BookLookupData(
                        isbn = "222",
                        title = "Recommended Book",
                        authors = listOf("Other Author"),
                        coverImageUrl = "http://cover",
                    ),
                )
            recommendationRepository.nextResult = Result.Success(listOf(recommendation))
            val vm = RecommendationsViewModel(listOf("t1"), useCase())
            vm.uiState.test {
                val state = awaitItem()
                assertIs<RecommendationsUiState.Success>(state)
                assertEquals(1, state.recommendations.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Load failure sets Error state`() =
        runTest {
            recommendationRepository.nextResult =
                Result.Failure(RecommendationError.NetworkError(RuntimeException("x")))
            val vm = RecommendationsViewModel(listOf("t1"), useCase())
            vm.uiState.test {
                val state = awaitItem()
                assertIs<RecommendationsUiState.Error>(state)
                assertIs<RecommendationError.NetworkError>(state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
