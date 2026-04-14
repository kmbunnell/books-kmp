package com.example.books_kmp.di

import com.example.books_kmp.data.library.SupabaseBookRepository
import com.example.books_kmp.domain.library.BookRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AppModuleTest {
    @Test
    fun `appModule wires BookRepository to SupabaseBookRepository`() {
        // Override the production SupabaseClient with a minimal-settings instance so Auth
        // does not require platform session storage when running in commonTest.
        val testClient =
            createSupabaseClient(
                supabaseUrl = "https://placeholder.supabase.co",
                supabaseKey = "placeholder-key",
            ) {
                install(Auth) { minimalSettings() }
                install(Postgrest)
            }
        val koin =
            koinApplication {
                allowOverride(true)
                modules(
                    appModule("https://placeholder.supabase.co", "placeholder-key"),
                    module { single { testClient } },
                )
            }.koin
        try {
            val repo = koin.get<BookRepository>()
            assertNotNull(repo)
            assertTrue(repo is SupabaseBookRepository)
        } finally {
            koin.close()
        }
    }
}
