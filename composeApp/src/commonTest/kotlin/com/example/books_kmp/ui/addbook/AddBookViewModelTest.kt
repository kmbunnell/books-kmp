package com.example.books_kmp.ui.addbook

import app.cash.turbine.test
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.FakeEntitlementState
import com.example.books_kmp.domain.library.AddBookUseCase
import com.example.books_kmp.domain.library.BarcodeScanError
import com.example.books_kmp.domain.library.FakeBookLookupService
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.library.LookupBookUseCase
import com.example.books_kmp.domain.library.LookupByTitleUseCase
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import com.example.books_kmp.testing.TEST_INSTANT
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
    private lateinit var addBookUseCase: AddBookUseCase
    private lateinit var lookupByTitleUseCase: LookupByTitleUseCase
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
        lookupUseCase = LookupBookUseCase(fakeService)
        addBookUseCase = AddBookUseCase(fakeRepo, FakeEntitlementState())
        lookupByTitleUseCase = LookupByTitleUseCase(fakeService)
        viewModel = AddBookViewModel(lookupUseCase, addBookUseCase, lookupByTitleUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading false — no error and no dialog and no foundBook`() =
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

    private val matchingBook =
        Book(
            id = "existing-id",
            isbn = "9780140449136",
            title = "The Iliad",
            authors = listOf("Homer"),
            coverImageUrl = null,
            updatedAt = TEST_INSTANT,
        )

    @Test
    fun `LookupIsbn with duplicate isbn in repo shows book preview with no dialog`() =
        runTest {
            fakeRepo.seedBooks(matchingBook)
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            val state = viewModel.uiState.value
            assertFalse(state.showDuplicateDialog)
            assertEquals(validLookupData, state.foundBook)
        }

    @Test
    fun `ConfirmBook with DuplicateBook sets showDuplicateDialog and clears isLoading`() =
        runTest {
            fakeRepo.seedBooks(matchingBook)
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertEquals(validLookupData, viewModel.uiState.value.foundBook)

            viewModel.onIntent(AddBookIntent.ConfirmBook)
            val state = viewModel.uiState.value
            assertTrue(state.showDuplicateDialog)
            assertEquals(validLookupData, state.foundBook)
            assertFalse(state.isLoading)
        }

    @Test
    fun `DismissDuplicateDialog clears showDuplicateDialog and foundBook`() =
        runTest {
            fakeRepo.seedBooks(matchingBook)
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            viewModel.onIntent(AddBookIntent.ConfirmBook)
            assertTrue(viewModel.uiState.value.showDuplicateDialog)
            viewModel.onIntent(AddBookIntent.DismissDuplicateDialog)
            val state = viewModel.uiState.value
            assertFalse(state.showDuplicateDialog)
            assertNull(state.foundBook)
        }

    @Test
    fun `LookupIsbn with NotFound sets error to NotFound`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.NotFound)
            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
                viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
                assertIs<AddBookScreenError.NotFound>(viewModel.uiState.value.error)
                expectNoEvents()
            }
        }

    @Test
    fun `EnterManually emits NavigateToManualEntry effect`() =
        runTest {
            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.EnterManually)
                assertIs<AddBookEffect.NavigateToManualEntry>(awaitItem())
            }
        }

    @Test
    fun `CancelBookPreview clears isbn in state`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)
            viewModel.onIntent(AddBookIntent.CancelBookPreview)
            assertEquals("", viewModel.uiState.value.isbn)
        }

    @Test
    fun `DismissDuplicateDialog clears isbn in state`() =
        runTest {
            fakeRepo.seedBooks(matchingBook)
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            viewModel.onIntent(AddBookIntent.ConfirmBook)
            assertTrue(viewModel.uiState.value.showDuplicateDialog)
            viewModel.onIntent(AddBookIntent.DismissDuplicateDialog)
            assertEquals("", viewModel.uiState.value.isbn)
        }

    @Test
    fun `AddAnyway emits BookAdded and resets state and inserts book with original isbn`() =
        runTest {
            fakeRepo.seedBooks(matchingBook)
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            viewModel.onIntent(AddBookIntent.ConfirmBook)
            assertTrue(viewModel.uiState.value.showDuplicateDialog)

            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.AddAnyway)
                assertIs<AddBookEffect.BookAdded>(awaitItem())
            }
            assertEquals(AddBookUiState(), viewModel.uiState.value)
            assertEquals("9780140449136", fakeRepo.lastAddedBook?.isbn)
        }

    @Test
    fun `AddAnyway on repo failure sets NetworkError`() =
        runTest {
            fakeRepo.seedBooks(matchingBook)
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            viewModel.onIntent(AddBookIntent.ConfirmBook)
            assertTrue(viewModel.uiState.value.showDuplicateDialog)
            fakeRepo.addBookShouldFail = true
            viewModel.onIntent(AddBookIntent.AddAnyway)
            assertIs<AddBookScreenError.NetworkError>(viewModel.uiState.value.error)
        }

    @Test
    fun `LookupIsbn with NetworkError sets error to NetworkError and clears isLoading`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.NetworkError)
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
    fun `LookupIsbn with Unauthenticated sets error to Unauthenticated and clears isLoading`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.Unauthenticated)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            val state = viewModel.uiState.value
            assertIs<AddBookScreenError.Unauthenticated>(state.error)
            assertFalse(state.isLoading)
        }

    @Test
    fun `LookupIsbn clears stale foundBook when a subsequent lookup fails`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)

            fakeService.lookupResult = Result.Failure(BookLookupError.NetworkError)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780553380163"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780553380163"))
            assertNull(viewModel.uiState.value.foundBook)
        }

    @Test
    fun `Retry re-invokes lookup with current isbn state and sets foundBook on success`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.NetworkError)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNull(viewModel.uiState.value.foundBook)

            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.Retry)
            assertEquals(validLookupData, viewModel.uiState.value.foundBook)
        }

    @Test
    fun `ConfirmBook emits BookAdded and resets state on success`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)

            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.ConfirmBook)
                assertIs<AddBookEffect.BookAdded>(awaitItem())
            }
            assertEquals(AddBookUiState(), viewModel.uiState.value)
        }

    @Test
    fun `ConfirmBook on failure sets error to NetworkError`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            fakeRepo.addBookShouldFail = true
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

    @Test
    fun `StartScan sets isScanning true`() =
        runTest {
            assertFalse(viewModel.uiState.value.isScanning)
            viewModel.onIntent(AddBookIntent.StartScan)
            assertTrue(viewModel.uiState.value.isScanning)
        }

    @Test
    fun `ScanDismissed sets isScanning false`() =
        runTest {
            viewModel.onIntent(AddBookIntent.StartScan)
            assertTrue(viewModel.uiState.value.isScanning)
            viewModel.onIntent(AddBookIntent.ScanDismissed)
            assertFalse(viewModel.uiState.value.isScanning)
        }

    @Test
    fun `StartScan clears previous error and foundBook and isbn`() =
        runTest {
            fakeService.lookupResult = Result.Failure(BookLookupError.NotFound)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertIs<AddBookScreenError.NotFound>(viewModel.uiState.value.error)

            viewModel.onIntent(AddBookIntent.StartScan)

            val state = viewModel.uiState.value
            assertNull(state.error)
            assertNull(state.foundBook)
            assertEquals("", state.isbn)
            assertTrue(state.isScanning)
        }

    @Test
    fun `StartScan after successful lookup clears foundBook and isbn`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)

            viewModel.onIntent(AddBookIntent.StartScan)

            val state = viewModel.uiState.value
            assertNull(state.foundBook)
            assertNull(state.error)
            assertEquals("", state.isbn)
            assertTrue(state.isScanning)
        }

    @Test
    fun `BarcodeScanned sets isScanning false and triggers lookup`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.StartScan)
            assertTrue(viewModel.uiState.value.isScanning)
            viewModel.onIntent(AddBookIntent.BarcodeScanned("9780140449136"))
            assertFalse(viewModel.uiState.value.isScanning)
            assertEquals(validLookupData, viewModel.uiState.value.foundBook)
        }

    @Test
    fun `ScanFailed with Unknown sets isScanning false and error to ScanUnknownError`() =
        runTest {
            viewModel.onIntent(AddBookIntent.StartScan)
            viewModel.onIntent(AddBookIntent.ScanFailed(BarcodeScanError.Unknown(null)))
            val state = viewModel.uiState.value
            assertFalse(state.isScanning)
            assertEquals(AddBookScreenError.ScanUnknownError, state.error)
        }

    @Test
    fun `ScanFailed with CameraPermissionDenied sets isScanning false and error to ScanCameraPermissionDenied`() =
        runTest {
            viewModel.onIntent(AddBookIntent.StartScan)
            viewModel.onIntent(AddBookIntent.ScanFailed(BarcodeScanError.CameraPermissionDenied))
            val state = viewModel.uiState.value
            assertFalse(state.isScanning)
            assertEquals(AddBookScreenError.ScanCameraPermissionDenied, state.error)
        }

    @Test
    fun `ScanFailed with HardwareUnavailable sets isScanning false and error to ScanHardwareUnavailable`() =
        runTest {
            viewModel.onIntent(AddBookIntent.StartScan)
            viewModel.onIntent(AddBookIntent.ScanFailed(BarcodeScanError.HardwareUnavailable))
            val state = viewModel.uiState.value
            assertFalse(state.isScanning)
            assertEquals(AddBookScreenError.ScanHardwareUnavailable, state.error)
        }

    @Test
    fun `ScanFailed with Cancelled sets isScanning false and no error`() =
        runTest {
            viewModel.onIntent(AddBookIntent.StartScan)
            viewModel.onIntent(AddBookIntent.ScanFailed(BarcodeScanError.Cancelled))
            val state = viewModel.uiState.value
            assertFalse(state.isScanning)
            assertNull(state.error)
        }

    // --- Title mode tests ---

    @Test
    fun `SetLookupMode to Title updates mode and clears isbn and error and foundBook`() =
        runTest {
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.SetLookupMode(LookupMode.Title))
            val state = viewModel.uiState.value
            assertEquals(LookupMode.Title, state.lookupMode)
            assertEquals("", state.isbn)
            assertNull(state.error)
            assertNull(state.foundBook)
            assertTrue(state.titleResults.isEmpty())
        }

    @Test
    fun `SetLookupMode to ISBN clears titleQuery and titleResults`() =
        runTest {
            viewModel.onIntent(AddBookIntent.SetLookupMode(LookupMode.Title))
            viewModel.onIntent(AddBookIntent.TitleChanged("The Iliad"))
            viewModel.onIntent(AddBookIntent.SetLookupMode(LookupMode.ISBN))
            val state = viewModel.uiState.value
            assertEquals(LookupMode.ISBN, state.lookupMode)
            assertEquals("", state.titleQuery)
            assertTrue(state.titleResults.isEmpty())
        }

    @Test
    fun `TitleChanged updates titleQuery in state`() =
        runTest {
            viewModel.onIntent(AddBookIntent.TitleChanged("The Iliad"))
            assertEquals("The Iliad", viewModel.uiState.value.titleQuery)
        }

    @Test
    fun `LookupByTitle success sets titleResults and clears isLoading`() =
        runTest {
            val results = listOf(validLookupData)
            fakeService.lookupByTitleResult = Result.Success(results)
            viewModel.onIntent(AddBookIntent.LookupByTitle("The Iliad"))
            val state = viewModel.uiState.value
            assertEquals(results, state.titleResults)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun `LookupByTitle NotFound sets NotFound error and clears isLoading`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.NotFound)
            viewModel.onIntent(AddBookIntent.LookupByTitle("Unknown Title"))
            val state = viewModel.uiState.value
            assertIs<AddBookScreenError.NotFound>(state.error)
            assertFalse(state.isLoading)
            assertTrue(state.titleResults.isEmpty())
        }

    @Test
    fun `LookupByTitle Unauthenticated sets Unauthenticated error`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.Unauthenticated)
            viewModel.onIntent(AddBookIntent.LookupByTitle("The Iliad"))
            assertIs<AddBookScreenError.Unauthenticated>(viewModel.uiState.value.error)
        }

    @Test
    fun `SelectTitleResult sets foundBook and preserves titleResults`() =
        runTest {
            val results = listOf(validLookupData)
            fakeService.lookupByTitleResult = Result.Success(results)
            viewModel.onIntent(AddBookIntent.LookupByTitle("The Iliad"))
            assertEquals(results, viewModel.uiState.value.titleResults)

            viewModel.onIntent(AddBookIntent.SelectTitleResult(validLookupData))
            val state = viewModel.uiState.value
            assertEquals(validLookupData, state.foundBook)
            assertEquals(results, state.titleResults)
        }

    @Test
    fun `CancelBookPreview in title mode restores titleResults`() =
        runTest {
            val results = listOf(validLookupData)
            fakeService.lookupByTitleResult = Result.Success(results)
            viewModel.onIntent(AddBookIntent.SetLookupMode(LookupMode.Title))
            viewModel.onIntent(AddBookIntent.LookupByTitle("The Iliad"))
            viewModel.onIntent(AddBookIntent.SelectTitleResult(validLookupData))
            assertNotNull(viewModel.uiState.value.foundBook)

            viewModel.onIntent(AddBookIntent.CancelBookPreview)
            val state = viewModel.uiState.value
            assertNull(state.foundBook)
            assertEquals(results, state.titleResults)
        }

    @Test
    fun `Retry with blank query does not dispatch lookup`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.NetworkError)
            viewModel.onIntent(AddBookIntent.SetLookupMode(LookupMode.Title))
            viewModel.onIntent(AddBookIntent.TitleChanged("The Iliad"))
            viewModel.onIntent(AddBookIntent.LookupByTitle("The Iliad"))
            assertIs<AddBookScreenError.NetworkError>(viewModel.uiState.value.error)

            viewModel.onIntent(AddBookIntent.TitleChanged(""))
            viewModel.onIntent(AddBookIntent.Retry)
            assertIs<AddBookScreenError.NetworkError>(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // --- ISBN format validation tests ---

    @Test
    fun `LookupIsbn with invalid ISBN format sets isbnFormatError true and does not set isLoading`() =
        runTest {
            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.IsbnChanged("123"))
                viewModel.onIntent(AddBookIntent.LookupIsbn("123"))
                val state = viewModel.uiState.value
                assertFalse(state.isLoading)
                assertTrue(state.isbnFormatError)
                assertNull(state.foundBook)
                expectNoEvents()
            }
        }

    @Test
    fun `LookupIsbn with ISBN padded by whitespace passes validation after trim`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("  9780140449136  "))
            viewModel.onIntent(AddBookIntent.LookupIsbn("  9780140449136  "))
            val state = viewModel.uiState.value
            assertFalse(state.isbnFormatError)
        }

    @Test
    fun `LookupIsbn with valid ISBN format clears isbnFormatError before lookup`() =
        runTest {
            // First trigger the error
            viewModel.onIntent(AddBookIntent.IsbnChanged("123"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("123"))
            assertTrue(viewModel.uiState.value.isbnFormatError)

            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertFalse(viewModel.uiState.value.isbnFormatError)
        }

    @Test
    fun `IsbnChanged clears isbnFormatError`() =
        runTest {
            viewModel.onIntent(AddBookIntent.IsbnChanged("123"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("123"))
            assertTrue(viewModel.uiState.value.isbnFormatError)

            viewModel.onIntent(AddBookIntent.IsbnChanged("1234"))
            assertFalse(viewModel.uiState.value.isbnFormatError)
        }

    @Test
    fun `SetLookupMode clears isbnFormatError`() =
        runTest {
            viewModel.onIntent(AddBookIntent.IsbnChanged("123"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("123"))
            assertTrue(viewModel.uiState.value.isbnFormatError)

            viewModel.onIntent(AddBookIntent.SetLookupMode(LookupMode.Title))
            assertFalse(viewModel.uiState.value.isbnFormatError)
        }

    // --- Add and Tag tests ---

    @Test
    fun `AddAndTag success emits NavigateToBookDetail with bookId and resets state`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            assertNotNull(viewModel.uiState.value.foundBook)

            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.AddAndTag)
                val effect = awaitItem()
                assertIs<AddBookEffect.NavigateToBookDetail>(effect)
                assertEquals("fake-id", effect.bookId)
            }
            assertEquals(AddBookUiState(), viewModel.uiState.value)
        }

    @Test
    fun `AddAndTag duplicate then AddAnyway emits NavigateToBookDetail`() =
        runTest {
            val noIsbnBook =
                BookLookupData(
                    isbn = null,
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = null,
                )
            fakeRepo.seedBooks(
                Book(
                    id = "existing-id",
                    isbn = null,
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = null,
                    updatedAt = TEST_INSTANT,
                ),
            )
            viewModel.onIntent(AddBookIntent.SelectTitleResult(noIsbnBook))
            assertNotNull(viewModel.uiState.value.foundBook)

            viewModel.onIntent(AddBookIntent.AddAndTag)
            assertTrue(viewModel.uiState.value.showDuplicateDialog)

            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.AddAnyway)
                val effect = awaitItem()
                assertIs<AddBookEffect.NavigateToBookDetail>(effect)
                assertEquals("fake-id", effect.bookId)
            }
        }

    @Test
    fun `AddAndTag failure sets NetworkError and does not emit NavigateToBookDetail`() =
        runTest {
            fakeService.lookupResult = Result.Success(validLookupData)
            viewModel.onIntent(AddBookIntent.IsbnChanged("9780140449136"))
            viewModel.onIntent(AddBookIntent.LookupIsbn("9780140449136"))
            fakeRepo.addBookShouldFail = true

            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.AddAndTag)
                expectNoEvents()
            }
            assertIs<AddBookScreenError.NetworkError>(viewModel.uiState.value.error)
        }

    @Test
    fun `AddAndTag duplicate DismissDialog clears flag so subsequent AddAnyway emits BookAdded`() =
        runTest {
            val noIsbnBook =
                BookLookupData(isbn = null, title = "The Iliad", authors = listOf("Homer"), coverImageUrl = null)
            fakeRepo.seedBooks(
                Book(
                    id = "existing-id",
                    isbn = null,
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = null,
                    updatedAt = TEST_INSTANT,
                ),
            )

            // AddAndTag → duplicate dialog → flag set
            viewModel.onIntent(AddBookIntent.SelectTitleResult(noIsbnBook))
            viewModel.onIntent(AddBookIntent.AddAndTag)
            assertTrue(viewModel.uiState.value.showDuplicateDialog)

            // Dismiss clears the flag
            viewModel.onIntent(AddBookIntent.DismissDuplicateDialog)
            assertFalse(viewModel.uiState.value.pendingAddAndTag)

            // Re-select and go through ConfirmBook → duplicate → AddAnyway
            viewModel.onIntent(AddBookIntent.SelectTitleResult(noIsbnBook))
            viewModel.onIntent(AddBookIntent.ConfirmBook)
            assertTrue(viewModel.uiState.value.showDuplicateDialog)

            viewModel.effects.test {
                viewModel.onIntent(AddBookIntent.AddAnyway)
                assertIs<AddBookEffect.BookAdded>(awaitItem())
            }
        }

    // --- Library limit reached effect tests ---
    // AddBookUseCase returns AddBookError.LibraryLimitReached for a free-tier user at the cap.

    private fun cappedFreeViewModel(): AddBookViewModel {
        val cappedRepo = FakeBookRepository()
        cappedRepo.seedBooks(
            *Array(25) { index ->
                Book(
                    id = "cap-$index",
                    isbn = "isbn-$index",
                    title = "Cap $index",
                    authors = listOf("Author"),
                    coverImageUrl = null,
                    updatedAt = TEST_INSTANT,
                )
            },
        )
        val lookup = LookupBookUseCase(fakeService)
        val add = AddBookUseCase(cappedRepo, FakeEntitlementState())
        return AddBookViewModel(lookup, add, lookupByTitleUseCase)
    }

    @Test
    fun `LibraryLimitReached on ConfirmBook emits ShowLibraryLimitReached and leaves error null`() =
        runTest {
            val vm = cappedFreeViewModel()
            vm.onIntent(AddBookIntent.SelectTitleResult(validLookupData))
            assertNotNull(vm.uiState.value.foundBook)

            vm.effects.test {
                vm.onIntent(AddBookIntent.ConfirmBook)
                assertIs<AddBookEffect.ShowLibraryLimitReached>(awaitItem())
            }
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `LibraryLimitReached on AddAndTag emits ShowLibraryLimitReached and leaves error null`() =
        runTest {
            val vm = cappedFreeViewModel()
            vm.onIntent(AddBookIntent.SelectTitleResult(validLookupData))
            assertNotNull(vm.uiState.value.foundBook)

            vm.effects.test {
                vm.onIntent(AddBookIntent.AddAndTag)
                assertIs<AddBookEffect.ShowLibraryLimitReached>(awaitItem())
            }
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `LibraryLimitReached on AddAnyway emits ShowLibraryLimitReached and leaves error null`() =
        runTest {
            val vm = cappedFreeViewModel()
            vm.onIntent(AddBookIntent.SelectTitleResult(validLookupData))
            assertNotNull(vm.uiState.value.foundBook)

            vm.effects.test {
                vm.onIntent(AddBookIntent.AddAnyway)
                assertIs<AddBookEffect.ShowLibraryLimitReached>(awaitItem())
            }
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `Retry in title mode re-invokes LookupByTitle with current titleQuery`() =
        runTest {
            fakeService.lookupByTitleResult = Result.Failure(BookLookupError.NetworkError)
            viewModel.onIntent(AddBookIntent.SetLookupMode(LookupMode.Title))
            viewModel.onIntent(AddBookIntent.TitleChanged("The Iliad"))
            viewModel.onIntent(AddBookIntent.LookupByTitle("The Iliad"))
            assertIs<AddBookScreenError.NetworkError>(viewModel.uiState.value.error)

            val results = listOf(validLookupData)
            fakeService.lookupByTitleResult = Result.Success(results)
            viewModel.onIntent(AddBookIntent.Retry)
            val state = viewModel.uiState.value
            assertEquals(results, state.titleResults)
            assertNull(state.error)
        }
}
