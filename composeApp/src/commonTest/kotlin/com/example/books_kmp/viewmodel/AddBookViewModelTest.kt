package com.example.books_kmp.viewmodel

import app.cash.turbine.test
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.ConfirmAddBookUseCase
import com.example.books_kmp.domain.library.FakeBookLookupService
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.library.LookupBookUseCase
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AddBookViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeBookRepository
    private lateinit var fakeService: FakeBookLookupService
    private lateinit var lookupUseCase: LookupBookUseCase
    private lateinit var confirmUseCase: ConfirmAddBookUseCase
    private lateinit var viewModel: AddBookViewModel

    private val validLookupData =
        BookLookupData(
            isbn = "9780140449136",
            title = "The Iliad",
            authors = listOf("Homer"),
            coverImageUrl = null,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeBookRepository()
        fakeService = FakeBookLookupService()
        lookupUseCase = LookupBookUseCase(fakeRepo, fakeService)
        confirmUseCase = ConfirmAddBookUseCase(fakeRepo)
        viewModel = AddBookViewModel(lookupUseCase, confirmUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading false, no error, no dialog, no foundBook`() =
        runTest {
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertFalse(state.showDuplicateDialog)
            assertNull(state.foundBook)
        }

    @Test
    fun `IsbnChanged updates isbn in state`() =
        runTest {
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            assertEquals("9780140449136", viewModel.uiState.value.isbn)
        }

    @Test
    fun `LookupIsbn sets isLoading false after completion`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `LookupIsbn with Success sets foundBook in state and does not navigate`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
                viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
                assertEquals(validLookupData, viewModel.uiState.value.foundBook)
                expectNoEvents()
            }
        }

    @Test
    fun `LookupIsbn with Duplicate sets showDuplicateDialog true and clears isLoading`() =
        runTest {
            fakeRepo.isbnExistsOverride = true
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            val state = viewModel.uiState.value
            assertTrue(state.showDuplicateDialog)
            assertFalse(state.isLoading)
        }

    @Test
    fun `DismissDuplicateDialog clears showDuplicateDialog`() =
        runTest {
            fakeRepo.isbnExistsOverride = true
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertTrue(viewModel.uiState.value.showDuplicateDialog)
            viewModel.onIntent(AddBookIntent.DismissDuplicateDialog)
            assertFalse(viewModel.uiState.value.showDuplicateDialog)
        }

    @Test
    fun `LookupIsbn with NotFound emits NavigateToManualEntry effect`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.NotFound)
            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
                viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
                assertIs<AddBookEffect.NavigateToManualEntry>(awaitItem())
            }
        }

    @Test
    fun `LookupIsbn with NetworkError sets error to NetworkError and clears isLoading`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.NetworkError(RuntimeException("err")))
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            val state = viewModel.uiState.value
            assertIs<AddBookScreenError.NetworkError>(state.error)
            assertFalse(state.isLoading)
        }

    @Test
    fun `LookupIsbn with RateLimited sets error to RateLimited and clears isLoading`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.RateLimited)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            val state = viewModel.uiState.value
            assertIs<AddBookScreenError.RateLimited>(state.error)
            assertFalse(state.isLoading)
        }

    @Test
    fun `LookupIsbn with MalformedResponse sets error to NetworkError and clears isLoading`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.MalformedResponse)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            val state = viewModel.uiState.value
            assertIs<AddBookScreenError.NetworkError>(state.error)
            assertFalse(state.isLoading)
        }

    @Test
    fun `LookupIsbn clears stale foundBook when a subsequent lookup fails`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)

            fakeService.lookupResult = Result.Failure(BookLookupError.NetworkError(RuntimeException("err")))
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780553380163"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780553380163"))
            assertNull(viewModel.uiState.value.foundBook)
        }

    @Test
    fun `Retry re-invokes lookup with current isbn state and sets foundBook on success`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.NetworkError(RuntimeException("err")))
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNull(viewModel.uiState.value.foundBook)

            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.Retry)
            assertEquals(validLookupData, viewModel.uiState.value.foundBook)
        }

    @Test
    fun `ConfirmBook calls ConfirmAddBookUseCase and emits NavigateToLibrary on success`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)

            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.ConfirmBook)
                assertIs<AddBookEffect.NavigateToLibrary>(awaitItem())
            }
        }

    @Test
    fun `ConfirmBook on failure sets error to NetworkError`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            fakeRepo.addBookShouldThrow = true
            viewModel.onIntent(AddBookIntent.ConfirmBook)
            assertIs<AddBookScreenError.NetworkError>(viewModel.uiState.value.error)
        }

    @Test
    fun `CancelBookPreview clears foundBook in state`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)
            viewModel.onIntent(AddBookIntent.CancelBookPreview)
            assertNull(viewModel.uiState.value.foundBook)
        }
}
