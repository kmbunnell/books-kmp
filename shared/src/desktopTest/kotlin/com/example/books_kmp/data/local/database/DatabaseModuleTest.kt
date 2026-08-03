package com.example.books_kmp.data.local.database

import app.cash.sqldelight.db.SqlDriver
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Doesn't call [platformDriverModule] directly: it resolves the [SqlDriver] against the real
 * OS-conventional app-data directory with no way to redirect it, which would make this test touch
 * the same database file the installed app uses. Binding [createDesktopDriver] against a temp path
 * still exercises the same `databaseModule` + driver-module resolution wiring that
 * [sharedDataModules] -- registered by `BooksApplication.kt`/`Main.kt`/`MainViewController.kt` --
 * depends on.
 */
class DatabaseModuleTest {
    private lateinit var koin: Koin
    private val tempDir = Files.createTempDirectory("database-module-test")

    @BeforeTest
    fun setUp() {
        val dbFile = tempDir.resolve("database-module-test.db").toFile()
        val driverModule =
            module {
                single<SqlDriver> {
                    createDesktopDriver("jdbc:sqlite:${dbFile.absolutePath}")
                } onClose { it?.close() }
            }
        koin = koinApplication { modules(databaseModule(), driverModule) }.koin
    }

    @AfterTest
    fun tearDown() {
        koin.close()
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `databaseModule and the desktop driver module resolve together`() {
        assertNotNull(koin.get<SqlDriver>())
        assertNotNull(koin.get<BooksDatabase>())
    }
}
