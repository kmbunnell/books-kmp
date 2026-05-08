package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.NewBook
import com.example.books_kmp.util.normalise

class AddBookUseCase(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(
        lookupData: BookLookupData,
        forceAdd: Boolean = false,
    ): Result<Book, AddBookError> {
        if (lookupData.isbn == null && !forceAdd) {
            when (val check = bookRepository.findDuplicateTitle(normalise(lookupData.title).lowercase())) {
                is Result.Failure -> return Result.Failure(AddBookError.NetworkError)
                is Result.Success ->
                    if (check.data != null) {
                        return Result.Failure(
                            AddBookError.DuplicateTitle(check.data.title)
                        )
                    }
            }
        }
        val newBook =
            NewBook(
                isbn = lookupData.isbn,
                title = lookupData.title,
                authors = lookupData.authors,
                coverImageUrl = lookupData.coverImageUrl,
            )
        return when (val result = bookRepository.addBook(newBook)) {
            is Result.Success -> Result.Success(result.data)
            is Result.Failure -> Result.Failure(AddBookError.NetworkError)
        }
    }
}
