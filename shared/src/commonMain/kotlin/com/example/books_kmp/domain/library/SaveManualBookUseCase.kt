package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import com.example.books_kmp.util.normalise

class SaveManualBookUseCase(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(
        title: String,
        author: String,
        forceAdd: Boolean = false,
    ): Result<Book, SaveManualBookError> {
        if (!forceAdd) {
            when (val check = bookRepository.findDuplicateTitle(normalise(title).lowercase())) {
                is Result.Failure -> return Result.Failure(SaveManualBookError.SaveFailed)
                is Result.Success -> if (check.data != null) return Result.Failure(SaveManualBookError.DuplicateTitle)
            }
        }
        val book = NewBook(title = title, authors = listOf(author))
        return when (val result = bookRepository.addBook(book)) {
            is Result.Success -> Result.Success(result.data)
            is Result.Failure -> Result.Failure(SaveManualBookError.SaveFailed)
        }
    }
}
