package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError

class LookupByTitleUseCase(private val bookLookupService: BookLookupService) {
    suspend operator fun invoke(title: String): Result<List<BookLookupData>, AddBookError> =
        when (val result = bookLookupService.lookupByTitle(title)) {
            is Result.Success ->
                if (result.data.isEmpty()) {
                    Result.Failure(AddBookError.NotFound)
                } else {
                    Result.Success(result.data)
                }
            is Result.Failure ->
                Result.Failure(
                    when (result.error) {
                        BookLookupError.NotFound -> AddBookError.NotFound
                        BookLookupError.RateLimited -> AddBookError.RateLimited
                        BookLookupError.MalformedResponse -> AddBookError.MalformedResponse
                        BookLookupError.NetworkError -> AddBookError.NetworkError
                    },
                )
        }
}
