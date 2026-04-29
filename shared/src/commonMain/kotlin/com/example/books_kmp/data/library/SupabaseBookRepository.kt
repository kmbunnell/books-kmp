package com.example.books_kmp.data.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.library.LoadBooksError
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class SupabaseBookRepository(private val supabase: SupabaseClient) : BookRepository {
    override suspend fun addBook(book: NewBook): Book {
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        val dto = book.toDto(userId)
        return supabase.from("books").insert(dto) { select() }.decodeSingle<BookDto>().toBook()
    }

    override suspend fun getBooksByUser(): Result<List<Book>, LoadBooksError> =
        try {
            Result.Success(fetchBooks())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(LoadBooksError.NetworkError(e))
        }

    // TODO: each call here fetches all user books from the network; add a cache layer when needed.
    override suspend fun getBookByIsbn(isbn: String): Book? = fetchBooks().find { it.isbn == isbn }

    // TODO: each call here fetches all user books from the network; add a cache layer when needed.
    override suspend fun isbnExists(isbn: String?): Boolean {
        if (isbn == null) return false
        return fetchBooks().any { it.isbn == isbn }
    }

    private suspend fun fetchBooks(): List<Book> =
        supabase.from("books")
            .select(Columns.raw("*, book_tags(tag_id)"))
            .decodeList<BookDto>()
            .map { it.toBook() }
}
