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
 * Platform-agnostic database wiring. Resolves the `SqlDriver` supplied by [platformDriverModule],
 * so it is only usable when that module is registered too. Prefer registering [sharedDataModules]
 * over either of these individually -- it keeps the pairing intact by construction.
 *
 * A function rather than a top-level `val`: a `val` would build the `Module` -- and cache the
 * `single { }` instance slot inside it -- exactly once for the process, so every Koin container
 * that loaded it would share the same `BooksDatabase` and could close one another's connection.
 */
fun databaseModule(): Module =
    module {
        single { BooksDatabase(get()) }
    }

/**
 * Every Koin module the `shared` module contributes, in registration order.
 *
 * Application entry points (`BooksApplication.kt`, `MainViewController.kt`, `Main.kt`) call this
 * single function, so adding a module here reaches all three platforms at once instead of needing
 * the same edit repeated per entry point.
 *
 * A function rather than a top-level `val`, for the same reason as [databaseModule]: it must
 * build fresh `Module` instances (with fresh `single { }` cache slots) on every call so that
 * separate Koin containers -- e.g. the production app and a test -- never share a driver.
 */
fun sharedDataModules(): List<Module> = listOf(databaseModule(), platformDriverModule())
