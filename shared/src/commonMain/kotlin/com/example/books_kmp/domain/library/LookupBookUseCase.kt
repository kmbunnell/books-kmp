package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError

class LookupBookUseCase(
    private val bookRepository: BookRepository,
    private val lookupService: BookLookupService,
) {
    suspend operator fun invoke(isbn: String): Result<BookLookupData, AddBookError> {
        val isbnAlreadyExists =
            when (val result = bookRepository.isbnExists(isbn)) {
                is Result.Failure -> return Result.Failure(AddBookError.NetworkError)
                is Result.Success -> result.data
            }
        if (isbnAlreadyExists) return Result.Failure(AddBookError.Duplicate)

        return when (val result = lookupService.lookupByIsbn(isbn)) {
            is Result.Failure ->
                Result.Failure(
                    when (result.error) {
                        is BookLookupError.NotFound -> AddBookError.NotFound
                        is BookLookupError.NetworkError -> AddBookError.NetworkError
                        is BookLookupError.RateLimited -> AddBookError.RateLimited
                        is BookLookupError.MalformedResponse -> AddBookError.MalformedResponse
                    },
                )
            is Result.Success -> Result.Success(result.data)
        }
    }
}
