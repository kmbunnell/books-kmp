package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError

interface BookLookupService {
    suspend fun lookupByIsbn(isbn: String): Result<BookLookupData, BookLookupError>
}
