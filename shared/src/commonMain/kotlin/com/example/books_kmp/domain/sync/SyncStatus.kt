package com.example.books_kmp.domain.sync

/**
 * Current state of the background sync engine, surfaced to the UI.
 *
 * A sealed interface rather than an enum so [Error] can carry the actual [SyncError] — the screen
 * needs the typed error to pick a message, and per AGENTS.md that resolution happens in Compose via
 * `stringResource()`, never by passing a pre-localized string through the domain layer.
 */
sealed interface SyncStatus {
    /** Nothing queued, nothing in flight — local and remote are believed to agree. */
    data object Idle : SyncStatus

    /** A queue drain or remote pull is currently running. */
    data object Syncing : SyncStatus

    /**
     * Known-offline: writes still succeed locally and accumulate in the pending-operation queue,
     * they just aren't being replayed yet. Distinct from [Error] because it is expected, not a fault.
     */
    data object Offline : SyncStatus

    /** The last sync attempt failed. */
    data class Error(val error: SyncError) : SyncStatus
}
