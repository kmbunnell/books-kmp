package com.example.books_kmp.viewmodel

import app.cash.turbine.test
import com.example.books_kmp.domain.library.FakeBookRepository
import com.example.books_kmp.domain.library.SaveManualBookUseCase
import com.example.books_kmp.domain.model.Book
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ManualEntryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeBookRepository
    private lateinit var useCase: SaveManualBookUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeBookRepository()
        useCase = SaveManualBookUseCase(fakeRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SaveBook with blank title sets titleError and does not call use case`() =
        runTest {
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.onIntent(ManualEntryIntent.SaveBook("", "Homer"))
            assertNotNull(viewModel.uiState.value.titleError)
            assertFalse(fakeRepo.addBookCalled)
        }

    @Test
    fun `SaveBook with blank author sets authorError and does not call use case`() =
        runTest {
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", ""))
            assertNotNull(viewModel.uiState.value.authorError)
            assertFalse(fakeRepo.addBookCalled)
        }

    @Test
    fun `SaveBook with both blank sets both errors`() =
        runTest {
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.onIntent(ManualEntryIntent.SaveBook("", ""))
            assertNotNull(viewModel.uiState.value.titleError)
            assertNotNull(viewModel.uiState.value.authorError)
            assertFalse(fakeRepo.addBookCalled)
        }

    @Test
    fun `SaveBook with valid inputs calls use case with correct title and author`() =
        runTest {
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
            assertTrue(fakeRepo.addBookCalled)
            assertEquals("The Odyssey", fakeRepo.lastAddedBook?.title)
            assertEquals(listOf("Homer"), fakeRepo.lastAddedBook?.authors)
        }

    @Test
    fun `SaveBook success emits NavigateToLibrary effect`() =
        runTest {
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.effects.test {
                viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
                assertIs<ManualEntryEffect.NavigateToLibrary>(awaitItem())
            }
        }

    @Test
    fun `SaveBook failure emits ShowError effect`() =
        runTest {
            fakeRepo.addBookShouldFail = true
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.effects.test {
                viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
                assertIs<ManualEntryEffect.ShowError>(awaitItem())
            }
        }

    @Test
    fun `isLoading transitions false to true to false across a successful save`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            fakeRepo.addBookGate = gate
            val viewModel = ManualEntryViewModel(useCase)
            backgroundScope.launch { viewModel.effects.collect {} }

            viewModel.uiState.test {
                assertFalse(awaitItem().isLoading)
                viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
                assertTrue(awaitItem().isLoading)
                gate.complete(Unit)
                assertFalse(awaitItem().isLoading)
            }
        }

    @Test
    fun `isLoading transitions false to true to false across a failed save`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            fakeRepo.addBookGate = gate
            fakeRepo.addBookShouldFail = true
            val viewModel = ManualEntryViewModel(useCase)
            backgroundScope.launch { viewModel.effects.collect {} }

            viewModel.uiState.test {
                assertFalse(awaitItem().isLoading)
                viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
                assertTrue(awaitItem().isLoading)
                gate.complete(Unit)
                assertFalse(awaitItem().isLoading)
            }
        }

    @Test
    fun `Cancel emits NavigateBack effect`() =
        runTest {
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.effects.test {
                viewModel.onIntent(ManualEntryIntent.Cancel)
                assertIs<ManualEntryEffect.NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `SaveBook with duplicate title sets showDuplicateDialog and does not emit ShowError`() =
        runTest {
            fakeRepo.seedBooks(Book(id = "1", isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null))
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
            assertTrue(viewModel.uiState.value.showDuplicateDialog)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `DismissDuplicateDialog clears showDuplicateDialog`() =
        runTest {
            fakeRepo.seedBooks(Book(id = "1", isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null))
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
            viewModel.onIntent(ManualEntryIntent.DismissDuplicateDialog)
            assertFalse(viewModel.uiState.value.showDuplicateDialog)
        }

    @Test
    fun `AddAnyway saves book and emits NavigateToLibrary`() =
        runTest {
            fakeRepo.seedBooks(Book(id = "1", isbn = null, title = "The Odyssey", authors = listOf("Homer"), coverImageUrl = null))
            val viewModel = ManualEntryViewModel(useCase)
            viewModel.onIntent(ManualEntryIntent.SaveBook("The Odyssey", "Homer"))
            viewModel.effects.test {
                viewModel.onIntent(ManualEntryIntent.AddAnyway)
                assertIs<ManualEntryEffect.NavigateToLibrary>(awaitItem())
            }
        }
}
