package com.example.books_kmp.domain.sync

import com.example.books_kmp.domain.model.NewBook

/**
 * A local mutation that has been applied optimistically and is waiting to be replayed against
 * Supabase.
 *
 * One variant per mutating `BookRepository`/`TagRepository` call, each carrying exactly the payload
 * that replay needs — nothing derived, nothing that could go stale between queueing and drain.
 *
 * Deliberately **not** `@Serializable`. This is a domain type; the `pending_operations.payload`
 * TEXT column is a data-layer concern, so the DTO/serialization strategy for it lives with
 * `PendingOperationQueue` rather than leaking annotations onto this interface.
 *
 * Ids for newly created rows ([AddBook.bookId], [CreateTag.tagId]) are generated on the client
 * rather than by the server. An optimistic local insert has to have a primary key immediately, and
 * reusing that same id on replay is what lets the drained result be reconciled with the local row
 * instead of creating a duplicate.
 */
sealed interface PendingOperation {
    data class AddBook(
        val bookId: String,
        val book: NewBook,
    ) : PendingOperation

    data class DeleteBook(val bookId: String) : PendingOperation

    /**
     * Applies or removes one book/tag association. [isApplied] is the desired *final* state after
     * replay — the opposite polarity of `BookRepository.applyTagDelta`'s `wasApplied` parameter,
     * which instead describes the state *before* the toggle. A single variant (rather than separate
     * add/remove ones) keeps the queue collapsible: repeated toggles of the same pair reduce to the
     * final [isApplied] value.
     */
    data class ToggleTag(
        val bookId: String,
        val tagId: String,
        val isApplied: Boolean,
    ) : PendingOperation

    data class CreateTag(
        val tagId: String,
        val name: String,
    ) : PendingOperation

    data class RenameTag(
        val tagId: String,
        val newName: String,
    ) : PendingOperation

    data class DeleteTag(val tagId: String) : PendingOperation
}
