package com.example.books_kmp.data.library

import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.model.Book
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SupabaseBookRepository(private val supabase: SupabaseClient) : BookRepository {
    override suspend fun addBook(book: Book): Book {
        val dto = book.toDto()
        return supabase.from("books").insert(dto) { select() }.decodeSingle<BookDto>().toBook()
    }

    override suspend fun getBooksByUser(): List<Book> =
        supabase.from("books").select().decodeList<BookDto>().map { it.toBook() }

    // TODO: each call here fetches all user books from the network; add a cache layer when needed.
    override suspend fun getBookByIsbn(isbn: String): Book? = getBooksByUser().find { it.isbn == isbn }

    // TODO: each call here fetches all user books from the network; add a cache layer when needed.
    override suspend fun isbnExists(isbn: String?): Boolean {
        if (isbn == null) return false
        return getBooksByUser().any { it.isbn == isbn }
    }
}
