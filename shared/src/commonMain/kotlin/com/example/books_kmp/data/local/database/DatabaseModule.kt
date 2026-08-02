package com.example.books_kmp.data.local.database

import org.koin.core.module.Module
import org.koin.dsl.module

/** File name of the on-device SQLite database. Shared by every platform driver. */
internal const val DATABASE_NAME = "books.db"

/**
 * Per-platform Koin module providing the single `SqlDriver` the database is built on.
 *
 * This is an `expect fun` returning a whole [Module] rather than an `expect class DriverFactory`
 * so that each platform's `actual` can pull whatever it needs straight out of the Koin `Scope`
 * receiver (Android needs `androidContext()`; iOS and desktop need nothing) without a shared
 * constructor signature that has to be kept in sync across three targets.
 *
 * Each `actual` is also responsible for enabling foreign-key enforcement, which SQLite leaves
 * OFF by default -- without it the `ON DELETE CASCADE` rules in `BookTags.sq` are inert.
 */
expect fun platformDriverModule(): Module

/**
 * Platform-agnostic database wiring. Must be registered alongside [platformDriverModule], which
 * supplies the `SqlDriver` this resolves.
 */
val databaseModule: Module =
    module {
        single { BooksDatabase(get()) }
    }
