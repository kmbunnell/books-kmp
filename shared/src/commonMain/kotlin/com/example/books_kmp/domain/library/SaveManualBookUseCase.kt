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
        isbn: String? = null,
        forceAdd: Boolean = false,
    ): Result<Book, SaveManualBookError> {
        val normalisedIsbn = isbn?.takeUnless { it.isBlank() }
        if (normalisedIsbn != null && !isValidIsbn(normalisedIsbn)) {
            return Result.Failure(SaveManualBookError.InvalidIsbn)
        }
        if (!forceAdd) {
            val check =
                bookRepository.findDuplicate(
                    isbn = null,
                    normalisedTitle = normalise(title).lowercase(),
                    normalisedAuthors = listOf(normalise(author).lowercase()),
                )
            when (check) {
                is Result.Failure -> return Result.Failure(SaveManualBookError.SaveFailed)
                is Result.Success -> if (check.data != null) return Result.Failure(SaveManualBookError.DuplicateTitle)
            }
        }
        val book = NewBook(title = title, authors = listOf(author), isbn = normalisedIsbn)
        return when (val result = bookRepository.addBook(book)) {
            is Result.Success -> Result.Success(result.data)
            is Result.Failure -> Result.Failure(SaveManualBookError.SaveFailed)
        }
    }

    // Length-only, matching the ISBN search bar — no checksum, no character restriction (ISBN-10 allows trailing X)
    private fun isValidIsbn(isbn: String): Boolean = isbn.length in setOf(10, 13)
}
