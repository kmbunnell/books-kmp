package com.example.books_kmp.domain.tags

import com.example.books_kmp.domain.Result

fun interface ToggleBookTagUseCase {
    suspend operator fun invoke(
        bookId: String,
        tagId: String,
        wasApplied: Boolean,
    ): Result<Unit, ToggleBookTagError>
}
