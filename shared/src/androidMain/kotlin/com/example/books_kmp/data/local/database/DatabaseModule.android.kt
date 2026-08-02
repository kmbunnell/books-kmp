package com.example.books_kmp.data.local.database

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

actual fun platformDriverModule(): Module =
    module {
        single<SqlDriver> {
            AndroidSqliteDriver(
                schema = BooksDatabase.Schema,
                context = androidContext(),
                name = DATABASE_NAME,
                callback =
                    object : AndroidSqliteDriver.Callback(BooksDatabase.Schema) {
                        override fun onConfigure(db: SupportSQLiteDatabase) {
                            super.onConfigure(db)
                            // Applies to every connection in the framework's pool, unlike a
                            // one-off `PRAGMA foreign_keys=ON` which is per-connection.
                            db.setForeignKeyConstraintsEnabled(true)
                        }
                    },
            )
        } onClose { it?.close() }
    }
