package com.example.books_kmp.domain.sync

/**
 * Sync-engine-level failures.
 *
 * Deliberately distinct from [com.example.books_kmp.domain.library.BookRepositoryError] and
 * [com.example.books_kmp.domain.tags.TagError]: those describe a *single* repository call failing,
 * whereas these describe the background engine — draining the pending-operation queue, pulling a
 * remote snapshot, holding a Realtime subscription — failing as a whole.
 *
 * The variants here are a best-current guess. `SyncEngine` itself does not exist yet, so expect this
 * set to be revised once queue-drain and Realtime failure modes are implemented for real.
 */
sealed interface SyncError {
    /** The device is offline, or the request never reached Supabase. */
    data object NetworkUnavailable : SyncError

    /** No authenticated user, or the session expired mid-sync. */
    data object NotAuthenticated : SyncError

    /**
     * A queued operation was replayed against the server and rejected in a way that retrying
     * unchanged cannot fix (validation failure, row already deleted, RLS denial).
     *
     * [operationId] identifies the offending row in the local `pending_operations` table so the
     * engine can quarantine or drop just that entry instead of stalling the whole queue.
     */
    data class OperationRejected(
        val operationId: String,
        val cause: Throwable?,
    ) : SyncError

    /** The Realtime subscription dropped and could not be re-established. */
    data object RealtimeDisconnected : SyncError

    /** Anything not covered above — kept so the engine never has to swallow an exception. */
    data class Unknown(val cause: Throwable?) : SyncError
}
