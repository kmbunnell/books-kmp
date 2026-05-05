package com.example.books_kmp.domain.tags

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookRepository

class DefaultToggleBookTagUseCase(
    private val tagRepository: TagRepository,
    private val bookRepository: BookRepository,
) : ToggleBookTagUseCase {
    override suspend operator fun invoke(
        bookId: String,
        tagId: String,
        wasApplied: Boolean,
    ): Result<Unit, ToggleBookTagError> {
        val result =
            if (wasApplied) {
                tagRepository.removeTagFromBook(bookId, tagId)
            } else {
                tagRepository.addTagToBook(bookId, tagId)
            }
        return when (result) {
            is Result.Success -> {
                bookRepository.applyTagDelta(bookId, tagId, wasApplied)
                Result.Success(Unit)
            }
            is Result.Failure -> Result.Failure(ToggleBookTagError.NetworkError)
        }
    }
}
