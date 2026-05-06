package com.example.books_kmp.data.library

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.library.BookRepositoryError
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SupabaseBookRepository(private val supabase: SupabaseClient) : BookRepository {
    private val booksCache = MutableStateFlow<List<Book>?>(null)
    override val booksFlow: StateFlow<List<Book>?> = booksCache.asStateFlow()

    override fun applyTagDelta(
        bookId: String,
        tagId: String,
        wasApplied: Boolean,
    ) {
        booksCache.update { current ->
            current?.map { book ->
                when {
                    book.id != bookId -> book
                    wasApplied -> book.copy(tags = book.tags - tagId)
                    else -> book.copy(tags = book.tags + tagId)
                }
            }
        }
    }

    override suspend fun addBook(book: NewBook): Result<Book, BookRepositoryError> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        return try {
            val dto = book.toDto(userId)
            Result.Success(supabase.from("books").insert(dto) { select() }.decodeSingle<BookDto>().toBook())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(BookRepositoryError.NetworkError)
        }
    }

    override suspend fun getBooksByUser(): Result<List<Book>, BookRepositoryError> =
        try {
            val books = fetchBooks()
            booksCache.value = books
            Result.Success(books)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(BookRepositoryError.NetworkError)
        }

    // User scoping is enforced by RLS — no explicit user_id filter needed in queries below.

    override suspend fun getBookById(id: String): Result<Book?, BookRepositoryError> =
        try {
            val dto =
                supabase
                    .from("books")
                    .select(Columns.raw("*, book_tags(tag_id)")) { filter { eq("id", id) } }
                    .decodeSingleOrNull<BookDto>()
            Result.Success(dto?.toBook())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(BookRepositoryError.NetworkError)
        }

    override suspend fun getBookByIsbn(isbn: String): Result<Book?, BookRepositoryError> =
        try {
            val dto =
                supabase
                    .from("books")
                    .select(Columns.raw("*, book_tags(tag_id)")) { filter { eq("isbn", isbn) } }
                    .decodeSingleOrNull<BookDto>()
            Result.Success(dto?.toBook())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(BookRepositoryError.NetworkError)
        }

    override suspend fun isbnExists(isbn: String?): Result<Boolean, BookRepositoryError> {
        if (isbn == null) return Result.Success(false)
        return try {
            val count =
                supabase
                    .from("books")
                    .select {
                        head = true
                        count(Count.EXACT)
                        filter { eq("isbn", isbn) }
                    }.countOrNull() ?: 0
            Result.Success(count > 0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(BookRepositoryError.NetworkError)
        }
    }

    override suspend fun deleteBook(bookId: String): Result<Unit, BookRepositoryError> =
        try {
            supabase.from("books").delete { filter { eq("id", bookId) } }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(BookRepositoryError.NetworkError)
        }

    // TODO: add a cache layer when needed — each call fetches all user books from the network.
    private suspend fun fetchBooks(): List<Book> =
        supabase.from("books")
            .select(Columns.raw("*, book_tags(tag_id)"))
            .decodeList<BookDto>()
            .map { it.toBook() }
}
