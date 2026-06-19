package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.util.normalise

fun matchesDuplicate(
    book: Book,
    isbn: String?,
    normalisedTitle: String,
    normalisedAuthors: List<String>,
): Boolean =
    if (book.isbn != null && isbn != null) {
        book.isbn == isbn
    } else {
        normalise(book.title).lowercase() == normalisedTitle &&
            book.authors.any { normalise(it).lowercase() in normalisedAuthors }
    }
