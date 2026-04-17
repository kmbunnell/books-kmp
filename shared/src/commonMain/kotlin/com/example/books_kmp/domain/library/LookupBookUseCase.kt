package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import kotlin.coroutines.cancellation.CancellationException

class LookupBookUseCase(
    private val bookRepository: BookRepository,
    private val lookupService: BookLookupService,
) {
    suspend operator fun invoke(isbn: String): Result<BookLookupData, AddBookError> =
        try {
            if (bookRepository.isbnExists(isbn)) return Result.Failure(AddBookError.Duplicate)

            when (val result = lookupService.lookupByIsbn(isbn)) {
                is Result.Failure ->
                    Result.Failure(
                        when (val error = result.error) {
                            is BookLookupError.NotFound -> AddBookError.NotFound
                            is BookLookupError.NetworkError -> AddBookError.NetworkError(error.cause)
                            is BookLookupError.RateLimited -> AddBookError.RateLimited
                            is BookLookupError.MalformedResponse -> AddBookError.MalformedResponse
                        },
                    )
                is Result.Success -> Result.Success(result.data)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(AddBookError.NetworkError(e))
        }
}
