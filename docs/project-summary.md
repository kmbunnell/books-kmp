# Shelvd — Project Summary

A cross-platform mobile app for book collectors to build and manage a personal digital library. Scan a barcode or enter an ISBN, and Shelvd pulls in the cover, title, and author from the Open Library API. Organize your collection with a flexible tagging system that acts as virtual shelves.

Built with **Kotlin Multiplatform** and **Compose Multiplatform**, targeting Android and iOS from a single shared codebase.

**Status:** Core flows functional end-to-end on Android. Shipped: email/password auth, library grid (tag filter, sort, search), barcode scanning (ML Kit / AVFoundation), Add Book by ISBN or title, Book Detail with tag toggling, Tag Management, and Manual Entry.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Shared Logic | Kotlin Multiplatform (KMP) |
| Shared UI | Compose Multiplatform (CMP) |
| Backend & Auth | Supabase (Auth, PostgreSQL, Row Level Security) |
| Networking | Ktor Client |
| Serialization | kotlinx.serialization |
| Image Loading | Coil 3 (Multiplatform) |
| Dependency Injection | Koin |
| Navigation | Compose Navigation (Multiplatform) |
| Barcode Scanning | ML Kit (Android) / AVFoundation (iOS) |
| Testing | kotlin.test, kotlinx-coroutines-test, Turbine, ComposeUiTest |

---

## Gradle Module Structure

```
books-kmp/
├── shared/               # Domain + Data layers (pure KMP)
├── shared-testing/       # Hand-rolled fakes shared across test source sets
└── composeApp/           # Presentation layer (Compose screens + ViewModels)
```

- `shared` — domain and data layers. No platform or UI dependencies.
- `shared-testing` — fake repository/service implementations used by both `commonTest` and `androidUnitTest` source sets.
- `composeApp` — all Compose UI, ViewModels, navigation, DI wiring, and platform entry points.

---

## Architecture

Clean Architecture with MVI. Strict unidirectional dependency rule:

```
Presentation (MVI) → Domain ← Data
```

### Domain layer (`shared/commonMain/.../domain/`)

Pure Kotlin. Contains:
- Domain models (`model/Book.kt`, `model/Tag.kt`, etc.)
- Repository interfaces (`BookRepository`, `TagRepository`, `AuthRepository`)
- Use cases — single `operator fun invoke`, return `Result<T, XxxError>`
- Domain error types — one sealed interface per use case/repo, each in its own file

### Data layer (`shared/commonMain/.../data/`)

Repository implementations, DTOs, mappers, API clients. Depends on domain interfaces only.

- `data/library/` — `SupabaseBookRepository`, `BookDto`
- `data/tags/` — `SupabaseTagRepository`, `TagDto`, `BookTagDto`
- `data/remote/` — `OpenLibraryApiClient`, Open Library DTOs

### Presentation layer (`composeApp/commonMain/.../ui/`)

Feature-organized. Each feature folder contains its screen(s), ViewModel, and MVI types together. ViewModels are co-located with their screens — there is no separate `viewmodel/` package.

```
ui/
├── auth/
│   ├── AuthViewModel.kt
│   ├── SignInViewModel.kt + SignInScreen.kt
│   ├── SignUpViewModel.kt + SignUpScreen.kt
│   └── SplashScreen.kt
├── library/
│   ├── LibraryViewModel.kt + LibraryScreen.kt
│   └── TagFilterBottomSheet.kt
├── addbook/
│   └── AddBookViewModel.kt + AddBookScreen.kt
├── bookdetail/
│   └── BookDetailViewModel.kt + BookDetailScreen.kt
├── manualentry/
│   └── ManualEntryViewModel.kt + ManualEntryScreen.kt
├── tags/
│   └── TagManagementViewModel.kt + TagManagementScreen.kt
├── scan/
│   └── BarcodeScannerView.kt       # expect/actual composable
└── permission/
    └── CameraPermissionDialogs.kt
```

Platform-specific source sets:
- `androidMain/ui/scan/BarcodeScannerView.android.kt` — CameraX + ML Kit
- `iosMain/ui/scan/BarcodeScannerView.ios.kt` — AVCaptureSession via UIKitView

---

## MVI Pattern

Each screen follows this contract (all artifacts live in the ViewModel file):

| Artifact | Type |
|---|---|
| UI state | `data class XxxUiState` |
| User actions | `sealed interface XxxIntent` |
| Side effects | `sealed interface XxxEffect` |
| Presentation errors | `sealed interface XxxError` |

- ViewModel exposes `StateFlow<XxxUiState>` and accepts `onIntent(XxxIntent)`.
- Sync intents call `_uiState.update {}` directly; suspend intents use `viewModelScope.launch`.
- Side effects emit via `Channel<XxxEffect>`, consumed in `LaunchedEffect` in the screen.
- String resolution happens exclusively in Compose via `stringResource()` — ViewModels emit typed errors, never localized strings.

---

## Result Type

All repository, use case, and platform gateway functions that perform I/O return `Result<T, E>`:

```kotlin
sealed interface Result<out T, out E> {
    data class Success<T>(val data: T) : Result<T, Nothing>
    data class Failure<E>(val error: E) : Result<Nothing, E>
}
```

- Callers pattern-match on `Success`/`Failure` — never wrap a `Result`-returning call in `try/catch`.
- `try/catch` is only used at the I/O boundary inside repository implementations.
- Applies even when the natural return type would be `Unit`, `Boolean`, or `T?`.

---

## Dependency Injection

Koin. All wiring lives in `di/AppModule.kt`. No manual construction of dependencies outside Koin.

- Supabase client, repositories, and use cases are `single` or `factory`.
- ViewModels use the `viewModel { }` DSL.

---

## Testing Conventions

- **TDD** — red/green/refactor. Skip tests for pure data classes and interfaces with no logic.
- **Hand-rolled fakes** over mock libraries (KMP compatibility). Fakes live in `shared-testing/`.
- **Unit tests** — `commonTest` for ViewModels and use cases; `androidUnitTest` for Compose UI tests.
- **Compose UI tests** — `ComposeUiTest` with semantic matchers, one test class per screen covering loading, empty, error, and populated states.
- Test files mirror production structure by feature: `ui/addbook/AddBookViewModelTest.kt` lives alongside `AddBookViewModel.kt`.

---

## Database Schema (Supabase / PostgreSQL)

| Table | Purpose |
|---|---|
| `books` | User's library entries. `user_id` defaults to `auth.uid()`. Partial unique index on `(user_id, isbn) where isbn is not null`. |
| `tags` | User-created tags. Seeded default tags protected by a DB trigger from rename/delete. |
| `book_tags` | Junction table linking books to tags. RLS authorized through the parent `books` row — no `user_id` column. |
| `book_metadata_cache` | Cache for Open Library lookups. `lookup_count` defaults to 1 (representing the lookup that caused the cache miss). |

RLS enforces ownership on all tables. The `protect_default_tags` trigger blocks renames/deletes of the seeded default tags, but allows cascade deletes when a user account is removed.

**Default tags** (seeded on signup, cannot be renamed/deleted): Read, Hardback, Paperback, ARC, Special Edition, Signed.
