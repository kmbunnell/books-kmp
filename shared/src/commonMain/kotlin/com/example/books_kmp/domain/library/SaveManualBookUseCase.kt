package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook

class SaveManualBookUseCase(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(
        title: String,
        author: String,
    ): Result<Book, SaveManualBookError> {
        val book = NewBook(title = title, authors = listOf(author))
        return when (val result = bookRepository.addBook(book)) {
            is Result.Success -> Result.Success(result.data)
            is Result.Failure -> Result.Failure(SaveManualBookError.SaveFailed)
        }
    }
}
