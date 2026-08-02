package com.example.books_kmp.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

actual fun platformDriverModule(): Module =
    module {
        single<SqlDriver> {
            NativeSqliteDriver(
                schema = BooksDatabase.Schema,
                name = DATABASE_NAME,
                // NativeSqliteDriver pools connections, so a one-off `PRAGMA foreign_keys=ON`
                // would only cover whichever connection happened to run it. This flag is
                // applied to every connection the pool opens.
                onConfiguration = { configuration ->
                    configuration.copy(
                        extendedConfig =
                            configuration.extendedConfig.copy(foreignKeyConstraints = true),
                    )
                },
            )
        } onClose { it?.close() }
    }
