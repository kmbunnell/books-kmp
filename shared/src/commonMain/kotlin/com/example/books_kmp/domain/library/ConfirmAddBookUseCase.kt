package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.NewBook
import kotlin.coroutines.cancellation.CancellationException

class ConfirmAddBookUseCase(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(lookupData: BookLookupData): Result<Book, ConfirmAddBookError> =
        try {
            val newBook =
                NewBook(
                    isbn = lookupData.isbn,
                    title = lookupData.title,
                    authors = lookupData.authors,
                    coverImageUrl = lookupData.coverImageUrl,
                )
            Result.Success(bookRepository.addBook(newBook))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(ConfirmAddBookError.NetworkError(e))
        }
}
