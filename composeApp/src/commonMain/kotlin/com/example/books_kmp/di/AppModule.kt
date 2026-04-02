package com.example.books_kmp.di

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun appModule(
    supabaseUrl: String,
    supabaseKey: String
): Module =
    module {
        single {
            createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey,
            ) {
                install(Auth)
            }
        }
    }
