package com.example.books_kmp.config

/**
 * Environment-specific Supabase credentials.
 *
 * Construct this at the platform entry point (Android `Application`, iOS `initKoin`) from
 * the appropriate build configuration / secrets source, then pass it to [appModule].
 */
data class SupabaseConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
)
