package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.NewBook

class AddBookUseCase(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(lookupData: BookLookupData): Result<Book, AddBookError> {
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
