package com.example.books_kmp.di

import com.example.books_kmp.auth.AuthRepository
import com.example.books_kmp.auth.SignInUseCase
import com.example.books_kmp.auth.SupabaseAuthRepository
import com.example.books_kmp.viewmodel.AuthViewModel
import com.example.books_kmp.viewmodel.SignInViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(
    supabaseUrl: String,
    supabaseKey: String,
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
        single<AuthRepository> { SupabaseAuthRepository(get()) }
        factory { SignInUseCase(get()) }
        viewModel { AuthViewModel(get()) }
        viewModel { SignInViewModel(get()) }
    }
