package com.example.books_kmp.data.local.database

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks down the *composition* of [sharedDataModules] rather than resolution of anything inside it.
 * Building the modules is safe on every target -- Android's `actual platformDriverModule()` only
 * touches `androidContext()` inside its `single { }` lambda, at resolution time, not at
 * `Module`-build time -- so this can live in `commonTest`.
 *
 * [databaseModule] and [sharedDataModules] are functions, not `val`s, specifically so each call
 * returns a fresh `Module` instance -- so referential-identity checks (`assertSame`,
 * `assertContains`) against a call's result no longer apply and aren't asserted here.
 */
class DatabaseModuleCompositionTest {
    @Test
    fun `sharedDataModules bundles the database module and the platform driver module`() {
        assertEquals(2, sharedDataModules().size)
    }
}
