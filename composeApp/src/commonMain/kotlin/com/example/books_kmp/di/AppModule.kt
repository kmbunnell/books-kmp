package com.example.books_kmp.di

import com.example.books_kmp.auth.SupabaseAuthRepository
import com.example.books_kmp.config.SupabaseConfig
import com.example.books_kmp.data.entitlement.EntitlementStore
import com.example.books_kmp.data.entitlement.SupabaseEntitlementRepository
import com.example.books_kmp.data.library.SupabaseBookRepository
import com.example.books_kmp.data.profile.SupabaseProfileRepository
import com.example.books_kmp.data.remote.GoogleBooksApiClient
import com.example.books_kmp.data.tags.SupabaseTagRepository
import com.example.books_kmp.domain.auth.AuthRepository
import com.example.books_kmp.domain.auth.SignInUseCase
import com.example.books_kmp.domain.auth.SignUpUseCase
import com.example.books_kmp.domain.entitlement.EntitlementRepository
import com.example.books_kmp.domain.entitlement.EntitlementState
import com.example.books_kmp.domain.library.AddBookUseCase
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.library.LookupBookUseCase
import com.example.books_kmp.domain.library.LookupByTitleUseCase
import com.example.books_kmp.domain.library.SaveManualBookUseCase
import com.example.books_kmp.domain.profile.ProfileRepository
import com.example.books_kmp.domain.tags.DefaultToggleBookTagUseCase
import com.example.books_kmp.domain.tags.TagRepository
import com.example.books_kmp.domain.tags.ToggleBookTagUseCase
import com.example.books_kmp.ui.addbook.AddBookViewModel
import com.example.books_kmp.ui.auth.AuthViewModel
import com.example.books_kmp.ui.auth.SignInViewModel
import com.example.books_kmp.ui.auth.SignUpViewModel
import com.example.books_kmp.ui.bookdetail.BookDetailViewModel
import com.example.books_kmp.ui.library.LibraryViewModel
import com.example.books_kmp.ui.manualentry.ManualEntryViewModel
import com.example.books_kmp.ui.tags.TagManagementViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(config: SupabaseConfig): Module =
    module {
        single {
            createSupabaseClient(
                supabaseUrl = config.supabaseUrl,
                supabaseKey = config.supabaseAnonKey,
            ) {
                install(Auth)
                install(Postgrest)
            }
        }
        single { AppScope() }
        single<AuthRepository> { SupabaseAuthRepository(get()) }
        single<BookRepository> { SupabaseBookRepository(get()) }
        single<TagRepository> { SupabaseTagRepository(get()) }
        single<ProfileRepository> { SupabaseProfileRepository(get()) }
        single<EntitlementRepository> { SupabaseEntitlementRepository(get()) }
        single<EntitlementState> { EntitlementStore(get(), get(), get<AppScope>().coroutineScope) }
        single { HttpClient() }
        single<BookLookupService> {
            val supabaseClient = get<SupabaseClient>()
            GoogleBooksApiClient(
                httpClient = get(),
                accessTokenProvider = { supabaseClient.auth.currentSessionOrNull()?.accessToken },
                supabaseUrl = config.supabaseUrl,
            )
        }
        factory { SignInUseCase(get()) }
        factory { SignUpUseCase(get()) }
        factory { LookupBookUseCase(get(), get()) }
        factory { LookupByTitleUseCase(get()) }
        factory { AddBookUseCase(get()) }
        factory { SaveManualBookUseCase(get()) }
        factory<ToggleBookTagUseCase> { DefaultToggleBookTagUseCase(get(), get()) }
        viewModel { AuthViewModel(get()) }
        viewModel { LibraryViewModel(get(), get(), get()) }
        viewModel { SignInViewModel(get()) }
        viewModel { SignUpViewModel(get()) }
        viewModel { ManualEntryViewModel(get()) }
        viewModel { AddBookViewModel(get(), get(), get()) }
        viewModel { TagManagementViewModel(get()) }
        viewModel { params -> BookDetailViewModel(params.get(), get(), get(), get()) }
    }
