package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupError
import com.example.books_kmp.domain.model.NewBook

class AddBookUseCase(
    private val bookRepository: BookRepository,
    private val lookupService: BookLookupService,
) {
    suspend operator fun invoke(isbn: String): Result<Book, AddBookError> {
        if (bookRepository.isbnExists(isbn)) return Result.Failure(AddBookError.Duplicate)

        return when (val result = lookupService.lookupByIsbn(isbn)) {
            is Result.Failure ->
                Result.Failure(
                    when (val error = result.error) {
                        is BookLookupError.NotFound -> AddBookError.NotFound
                        is BookLookupError.NetworkError -> AddBookError.NetworkError(error.cause)
                        is BookLookupError.RateLimited -> AddBookError.RateLimited
                        is BookLookupError.MalformedResponse -> AddBookError.MalformedResponse
                    },
                )
            is Result.Success -> {
                val data = result.data
                val book =
                    NewBook(
                        isbn = data.isbn,
                        title = data.title,
                        authors = data.authors,
                        coverImageUrl = data.coverImageUrl,
                    )
                Result.Success(bookRepository.addBook(book))
            }
        }
    }
}
