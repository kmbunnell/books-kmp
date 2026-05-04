package com.example.books_kmp.di

import com.example.books_kmp.auth.SupabaseAuthRepository
import com.example.books_kmp.data.library.SupabaseBookRepository
import com.example.books_kmp.data.remote.OpenLibraryApiClient
import com.example.books_kmp.data.tags.SupabaseTagRepository
import com.example.books_kmp.domain.auth.AuthRepository
import com.example.books_kmp.domain.auth.SignInUseCase
import com.example.books_kmp.domain.auth.SignUpUseCase
import com.example.books_kmp.domain.library.AddBookUseCase
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.library.LookupBookUseCase
import com.example.books_kmp.domain.library.SaveManualBookUseCase
import com.example.books_kmp.domain.tags.TagRepository
import com.example.books_kmp.viewmodel.AddBookViewModel
import com.example.books_kmp.viewmodel.AuthViewModel
import com.example.books_kmp.viewmodel.BookDetailViewModel
import com.example.books_kmp.viewmodel.LibraryViewModel
import com.example.books_kmp.viewmodel.ManualEntryViewModel
import com.example.books_kmp.viewmodel.SignInViewModel
import com.example.books_kmp.viewmodel.SignUpViewModel
import com.example.books_kmp.viewmodel.TagManagementViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
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
                install(Postgrest)
            }
        }
        single<AuthRepository> { SupabaseAuthRepository(get()) }
        single<BookRepository> { SupabaseBookRepository(get()) }
        single<TagRepository> { SupabaseTagRepository(get()) }
        single { HttpClient() }
        single<BookLookupService> { OpenLibraryApiClient(get()) }
        factory { SignInUseCase(get()) }
        factory { SignUpUseCase(get()) }
        factory { LookupBookUseCase(get(), get()) }
        factory { AddBookUseCase(get()) }
        factory { SaveManualBookUseCase(get()) }
        viewModel { AuthViewModel(get()) }
        viewModel { LibraryViewModel(get(), get()) }
        viewModel { SignInViewModel(get()) }
        viewModel { SignUpViewModel(get()) }
        viewModel { ManualEntryViewModel(get()) }
        viewModel { AddBookViewModel(get(), get()) }
        viewModel { TagManagementViewModel(get()) }
        viewModel { params -> BookDetailViewModel(params.get(), get(), get()) }
    }
