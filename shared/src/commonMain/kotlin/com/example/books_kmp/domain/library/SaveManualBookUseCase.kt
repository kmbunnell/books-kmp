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
            val normalisedTitle = normalise(title).lowercase()
            when (val titleResult = bookRepository.findBookByTitle(normalisedTitle)) {
                is Result.Success -> {
                    val existing = titleResult.data
                    if (existing != null) {
                        return Result.Failure(SaveManualBookError.DuplicateTitle)
                    }
                }
                is Result.Failure -> return Result.Failure(SaveManualBookError.SaveFailed)
            }
        }
        val book = NewBook(title = title, authors = listOf(author))
        return when (val result = bookRepository.addBook(book)) {
            is Result.Success -> Result.Success(result.data)
            is Result.Failure -> Result.Failure(SaveManualBookError.SaveFailed)
        }
    }
}
