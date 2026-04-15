package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlin.coroutines.cancellation.CancellationException

class SaveManualBookUseCase(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(
        title: String,
        author: String,
    ): Result<Book, SaveManualBookError> =
        try {
            val book =
                NewBook(
                    title = title,
                    authors = listOf(author),
                )
            Result.Success(bookRepository.addBook(book))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(SaveManualBookError.SaveFailed)
        }
}
