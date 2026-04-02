package com.example.books_kmp.di

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppModuleTest {
    @Test
    fun `auth plugin is registered on supabase client`() {
        val client =
            createSupabaseClient(
                supabaseUrl = "https://placeholder.supabase.co",
                supabaseKey = "placeholder-key",
            ) {
                install(Auth) {
                    minimalSettings()
                }
            }
        assertNotNull(client.auth)
    }
}
