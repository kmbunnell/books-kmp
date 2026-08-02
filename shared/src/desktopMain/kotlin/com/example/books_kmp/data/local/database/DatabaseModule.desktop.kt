package com.example.books_kmp.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.sqlite.SQLiteConfig

private const val APP_DIR_NAME = "Books"

/**
 * OS-conventional per-user application data directory: `Library/Application Support/Books` on
 * macOS, `%APPDATA%\Books` on Windows, `$XDG_DATA_HOME/Books` (falling back to
 * `~/.local/share/Books`) on Linux. Using the platform's real convention -- rather than a bare
 * `~/.books-kmp` dotfile -- means the OS backs it up, cleans it up, and displays it correctly
 * wherever per-app data is surfaced.
 */
private fun absoluteEnvPath(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() && File(it).isAbsolute }

private fun platformDataDirectory(): File {
    val userHome = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("mac") ->
            File(userHome, "Library/Application Support/$APP_DIR_NAME")
        osName.contains("win") ->
            File(absoluteEnvPath("APPDATA") ?: File(userHome, "AppData/Roaming").path, APP_DIR_NAME)
        else ->
            File(absoluteEnvPath("XDG_DATA_HOME") ?: File(userHome, ".local/share").path, APP_DIR_NAME)
    }
}

/** Desktop database location, e.g. `~/Library/Application Support/Books/books.db` on macOS. */
private fun databaseFile(): File {
    val directory = platformDataDirectory()
    check(directory.mkdirs() || directory.isDirectory) {
        "Failed to create application data directory: ${directory.absolutePath}"
    }
    return File(directory, DATABASE_NAME)
}

// For a file-backed URL, JdbcSqliteDriver opens a new connection per thread rather than reusing
// one, so a one-shot `PRAGMA foreign_keys=ON` only ever applies to the connecting thread. Setting
// it in the connection properties instead applies it to every connection the driver opens.
private val sqliteProperties =
    SQLiteConfig()
        .apply { enforceForeignKeys(true) }
        .toProperties()

/**
 * Builds the desktop [SqlDriver] against [url]. Extracted from [platformDriverModule] so
 * `desktopTest` can exercise this exact construction (properties, schema) against a temp-file
 * URL instead of re-implementing it -- see `DesktopDriverForeignKeyTest`.
 */
internal fun createDesktopDriver(url: String): SqlDriver =
    // Unlike the Android and native drivers, JdbcSqliteDriver only creates/migrates the schema
    // when it is handed one explicitly.
    JdbcSqliteDriver(
        url = url,
        properties = sqliteProperties,
        schema = BooksDatabase.Schema,
    )

actual fun platformDriverModule(): Module =
    module {
        single<SqlDriver> {
            createDesktopDriver("jdbc:sqlite:${databaseFile().absolutePath}")
        } onClose { it?.close() }
    }
