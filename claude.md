# CLAUDE.md — Books-KMP (Kotlin Multiplatform)

## Project Overview

Books-KMP is a cross-platform mobile book library app built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**, backed by **Supabase** (auth, database, RLS). Users scan or enter ISBNs to populate book metadata from the Open Library API, then organize their collection with a flexible tagging system.

**Target platforms:** Android and iOS from a single shared codebase.

For detailed specs, see:
- `docs/specs.md` — data models, API details, screens, functional requirements
- `docs/database.md` — Supabase schema, RLS policies, triggers, data flow
- `docs/auth.md` — authentication providers, session flow, Supabase auth modules

---

## Core Principles

### 1. Test-Driven Development (TDD)

**Red → Green → Refactor.** Write a failing test first, implement the minimum code to pass it, then refactor. No production code without a corresponding test.

**Unit tests (business logic):**
- Every public function in a use case or repository must have at least one unit test.
- When fixing a bug, write a test that reproduces the bug **before** writing the fix.
- Keep tests fast, isolated, and deterministic. Mock external dependencies at the repository boundary.
- Prefer hand-rolled fakes over mocking libraries for KMP compatibility.
- Use descriptive test names: `` `invoke returns error when ISBN is duplicate` ``.
- Each test should test one behavior. Prefer many small tests over few large ones.
- Test files mirror production structure: `domain/usecase/AddBookUseCaseTest.kt` alongside `domain/usecase/AddBookUseCase.kt`.
- Frameworks: `kotlin.test` + `kotlinx-coroutines-test` for domain/data; `Turbine` for Flow/StateFlow testing in ViewModels.

**Compose UI tests:**
- Every screen must have corresponding Compose UI tests verifying rendering and interaction.
- Test that the correct UI state is displayed (loading indicators, error messages, empty states, populated lists).
- Test user interactions: button clicks dispatch the correct intents, navigation triggers fire, form inputs update state.
- Use `ComposeUiTest` (Multiplatform) with semantic matchers (`onNodeWithText`, `onNodeWithContentDescription`).
- UI tests live alongside screen code: `ui/library/LibraryScreenTest.kt` for `ui/library/LibraryScreen.kt`.
- Inject fake ViewModels or fake use cases to isolate UI behavior from business logic.

**Integration tests:**
- Use `supabase start` (local Docker instance) for Supabase integration tests.

### 2. SOLID Principles

- **Single Responsibility:** Each class has one reason to change. Use cases do one thing. ViewModels manage UI state only. Repositories handle data access only.
- **Open/Closed:** Extend behavior through new use cases or repository implementations, not by modifying existing ones. Use interfaces and sealed types to allow extension without modification.
- **Liskov Substitution:** Fakes and real implementations of repository interfaces must be interchangeable. Tests rely on this.
- **Interface Segregation:** Keep repository interfaces focused. Prefer multiple small interfaces over one large one (e.g., `BookReadRepository` vs. `BookWriteRepository` if they serve different consumers).
- **Dependency Inversion:** Domain layer defines interfaces. Data layer implements them. Presentation depends on domain abstractions, never concrete data-layer classes.

### 3. DRY (Don't Repeat Yourself)

- Extract shared logic into use cases or utility functions. If the same transformation appears in two places, it belongs in a shared function.
- Shared UI elements belong in a common `components` package.
- Centralize constants (API endpoints, table names, error messages) in a single config object.
- Reuse mappers — one DTO-to-domain mapper per entity, called from a single location.

### 4. Clean Architecture + MVI

Strict layer separation with a **unidirectional dependency rule**: outer layers depend on inner layers, never the reverse.

```
Presentation (MVI) → Domain ← Data
```

**Layer definitions:**

- **Domain layer** (`shared/commonMain/.../domain/`): Pure Kotlin. Domain models, repository interfaces, and use cases. Zero platform or framework imports.
- **Data layer** (`shared/commonMain/.../data/` + platform source sets): Repository implementations, data sources, DTOs, and mappers. Depends on domain interfaces.
- **Presentation layer** (`composeApp/commonMain/`): Compose screens, ViewModels, and UI state. ViewModels depend on use cases only — no business logic in ViewModels.

**MVI pattern (Model-View-Intent):**

Each screen has three components:
- **State** (`data class XxxUiState`): Immutable snapshot of everything the UI needs to render. Exposed from ViewModel via `StateFlow<XxxUiState>`.
- **Intent** (`sealed interface XxxIntent`): Every user action is an explicit intent object (e.g., `SearchIntent.QueryChanged(text)`, `SearchIntent.Submit`). Screens call `viewModel.onIntent(intent)` — no ad-hoc ViewModel methods per action.
- **ViewModel** processes intents, delegates to use cases, and emits new state. Side effects (navigation, toasts) flow through a `SharedFlow<XxxEffect>` or `Channel<XxxEffect>`.

```kotlin
// Example pattern
data class LibraryUiState(val books: List<Book> = emptyList(), val isLoading: Boolean = false)

sealed interface LibraryIntent {
    data object LoadBooks : LibraryIntent
    data class DeleteBook(val id: String) : LibraryIntent
}

sealed interface LibraryEffect {
    data class ShowError(val message: String) : LibraryEffect
}
```

**Architecture rules:**
- Domain models and DTOs are always separate classes. Map between them explicitly. Do not leak DTO annotations (`@Serializable`, column names) into domain models.
- Repository interfaces live in the domain layer. Implementations live in the data layer.
- Use cases are single-responsibility: one public `operator fun invoke(...)` or `suspend operator fun invoke(...)`.
- Use cases return `Result<T, XxxError>` — never a custom `XxxResult` sealed interface. The error type is a feature-specific sealed interface (e.g., `AddBookError`, `SignInError`) defined in its own file alongside the use case. Success data goes in `Result.Success(data)`; all failure variants go in `Result.Failure(XxxError.Variant)`.
- Koin modules are the single place for dependency wiring. Never manually construct dependencies outside DI.
- ViewModels emit typed errors via a feature-specific `XxxError` sealed interface — never localized strings. `UiState` field errors and `XxxEffect.ShowError` carry `XxxError` values. String resolution happens exclusively in Compose screens via `stringResource()`, resolved before any `LaunchedEffect`/`collect` block.

---

## Module Structure

**Target architecture** (the codebase will evolve toward this incrementally):

```
books-kmp/
├── shared/                    ← KMP shared module
│   ├── commonMain/            ← Business logic, models, repositories
│   ├── androidMain/           ← Android-specific implementations
│   └── iosMain/               ← iOS-specific implementations
├── composeApp/                ← CMP shared UI module
│   ├── commonMain/            ← All Compose screens and components
│   ├── androidMain/           ← Android Compose entry point
│   └── iosMain/               ← iOS Compose entry point
└── iosApp/                    ← Xcode project / iOS application shell
```

**Current state:** The project is a single `composeApp` module (generated by the CMP wizard). There is no separate `shared/` module yet. The `shared/` module extraction will happen when the data and domain layers are built out (Supabase integration, repositories, use cases). A separate `androidApp/` shell is unnecessary — the Android entry point lives inside `composeApp/androidMain`.

Create packages and directories incrementally as features are built — do not scaffold empty directories upfront. Always confirm the package name, location, and purpose before adding.

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Shared Logic | Kotlin Multiplatform (KMP) | Business logic, data models, repository layer, API clients |
| Shared UI | Compose Multiplatform (CMP) | Declarative UI shared across Android and iOS |
| Backend | Supabase (`supabase-kt` SDK) | Auth, Postgres database, and RLS via a single KMP SDK |
| Auth | Supabase Auth + `compose-auth` plugin | Email/password, native Google and Apple sign-in from shared Compose code |
| Database | Supabase (PostgreSQL via PostgREST) | Cloud-synced relational storage with row-level security |
| Image Loading | Coil 3 (Multiplatform) | Async image loading with caching and placeholder support |
| Barcode Scanning | ML Kit (Android) / AVFoundation (iOS) | Camera-based ISBN barcode detection via `expect`/`actual` |
| Networking | Ktor Client | HTTP client for Open Library API and Supabase SDK transport |
| Serialization | kotlinx.serialization | JSON parsing for API responses and Supabase data |
| DI | Koin | Lightweight dependency injection for KMP |
| Navigation | Compose Navigation (Multiplatform) | Screen routing and back stack management |

---

## Code Style & Conventions

- Naming: `AddBookUseCase`, `BookRepository`, `LibraryViewModel`, `LibraryUiState`, `LibraryIntent`, `LibraryEffect`.
- `sealed class` / `sealed interface` for finite states (auth status, scan results, UI state, intents, effects).
- `data class` for models, DTOs, and UI state.
- Keep functions short (~30 lines max). Prefer explicit return types on public APIs.
- Avoid `!!` — prefer `?.let`, `?:`, or explicit null checks with descriptive errors.
- One file per class for use cases, ViewModels, and repository interfaces. Group related intents/state/effects with their ViewModel.

---

## Strings & Resources

- All user-visible strings must be defined in `composeApp/src/commonMain/composeResources/values/strings.xml` and accessed via `stringResource(Res.string.xxx)` in composables. Never hardcode display strings in Kotlin source.
- Do not add strings to `androidMain/res/values/strings.xml` — that file is Android-only and invisible to iOS. The only exception is `app_name`, which is required by `AndroidManifest.xml`.

---

## Secrets

Secrets go in `local.properties`, injected via `BuildConfig`. **Never commit secrets.**

---

## Common Pitfalls

- **Do not** create new packages or directories without asking first.
- **Do not** put business logic in ViewModels. Delegate to use cases.
- **Do not** skip writing the test first. If you are writing production code without a failing test, stop and write the test.
- **Do not** duplicate logic. If it exists once, reuse it.
- **Do not** commit API keys, Supabase URLs, or secrets.
- **Do not** manually construct class instances in Compose screens — always inject via Koin.
- **Do not** reference data-layer classes (DTOs, Supabase client) from the presentation layer.
- **Do not** use `expect`/`actual` unless truly necessary (barcode scanning, platform permissions). Prefer shared `commonMain` implementations.
- **Do not** add ad-hoc public methods to ViewModels for each user action. All user actions go through `onIntent(intent)`.
- **Do not** put mutable state in UI state classes. `UiState` is always an immutable `data class`.
- **Do not** skip Compose UI tests for new screens. Every screen needs tests for its states and interactions.
