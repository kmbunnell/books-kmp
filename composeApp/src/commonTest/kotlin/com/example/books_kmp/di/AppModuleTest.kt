package com.example.books_kmp.di

import com.example.books_kmp.config.SupabaseConfig
import com.example.books_kmp.data.library.SupabaseBookRepository
import com.example.books_kmp.data.remote.GoogleBooksApiClient
import com.example.books_kmp.domain.auth.AuthRepository
import com.example.books_kmp.domain.auth.SignInUseCase
import com.example.books_kmp.domain.auth.SignUpUseCase
import com.example.books_kmp.domain.library.AddBookUseCase
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.library.LookupBookUseCase
import com.example.books_kmp.domain.library.SaveManualBookUseCase
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AppModuleTest {
    private lateinit var koin: Koin

    @BeforeTest
    fun setUp() {
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
        koin =
            koinApplication {
                allowOverride(true)
                modules(
                    appModule(
                        SupabaseConfig(
                            supabaseUrl = "https://placeholder.supabase.co",
                            supabaseAnonKey = "placeholder-key",
                        ),
                    ),
                    module { single { testClient } },
                )
            }.koin
    }

    @AfterTest
    fun tearDown() {
        koin.close()
    }

    @Test
    fun `appModule wires BookRepository to SupabaseBookRepository`() {
        val repo = koin.get<BookRepository>()
        assertNotNull(repo)
        assertTrue(repo is SupabaseBookRepository)
    }

    @Test
    fun `appModule wires BookLookupService to GoogleBooksApiClient`() {
        val service = koin.get<BookLookupService>()
        assertNotNull(service)
        assertTrue(service is GoogleBooksApiClient)
    }

    @Test
    fun `appModule resolves all singleton bindings`() {
        assertNotNull(koin.get<AuthRepository>())
        assertNotNull(koin.get<BookRepository>())
        assertNotNull(koin.get<HttpClient>())
        assertNotNull(koin.get<BookLookupService>())
    }

    @Test
    fun `appModule resolves all use case bindings`() {
        assertNotNull(koin.get<SignInUseCase>())
        assertNotNull(koin.get<SignUpUseCase>())
        assertNotNull(koin.get<LookupBookUseCase>())
        assertNotNull(koin.get<AddBookUseCase>())
        assertNotNull(koin.get<SaveManualBookUseCase>())
    }
}
