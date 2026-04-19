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

Apply **SOLID** and **DRY** principles throughout. The sections below call out where they matter most for this project.

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
- Every screen must have tests for its states (loading, error, empty, populated) and interactions (button clicks, navigation, form input).
- Use `ComposeUiTest` (Multiplatform) with semantic matchers. UI tests live alongside screen code. Inject fake ViewModels or use cases to isolate UI behavior.

**Integration tests:**
- Use `supabase start` (local Docker instance) for Supabase integration tests.

### 2. Clean Architecture + MVI

Strict layer separation with a **unidirectional dependency rule**: outer layers depend on inner layers, never the reverse.

```
Presentation (MVI) → Domain ← Data
```

**Layer definitions:**

- **Domain:** Pure Kotlin. Domain models, repository interfaces, use cases. Zero platform or framework imports.
- **Data:** Repository implementations, data sources, DTOs, mappers. Depends on domain interfaces.
- **Presentation:** Compose screens, ViewModels, UI state. ViewModels depend on use cases only — no business logic in ViewModels.

**MVI pattern (Model-View-Intent):**

Each screen has three components:
- **State** (`data class XxxUiState`): Immutable snapshot of everything the UI needs to render. Exposed from ViewModel via `StateFlow<XxxUiState>`.
- **Intent** (`sealed interface XxxIntent`): Every user action is an explicit intent object (e.g., `SearchIntent.QueryChanged(text)`, `SearchIntent.Submit`). Screens call `viewModel.onIntent(intent)` — no ad-hoc ViewModel methods per action.
- **ViewModel** processes intents, delegates to use cases, and emits new state. Side effects (navigation, toasts) flow through a `SharedFlow<XxxEffect>` or `Channel<XxxEffect>`.

**Architecture rules:**
- Domain models and DTOs are always separate classes. Map between them explicitly. Do not leak DTO annotations (`@Serializable`, column names) into domain models.
- Repository interfaces live in the domain layer. Implementations live in the data layer.
- Use cases are single-responsibility: one public `operator fun invoke(...)` or `suspend operator fun invoke(...)`.
- Use cases return `Result<T, XxxError>` — never a custom `XxxResult` sealed interface. The error type is a feature-specific sealed interface (e.g., `AddBookError`, `SignInError`) defined in its own file alongside the use case. Success data goes in `Result.Success(data)`; all failure variants go in `Result.Failure(XxxError.Variant)`.
- Koin modules are the single place for dependency wiring. Never manually construct dependencies outside DI.
- ViewModels emit typed errors via a feature-specific `XxxError` sealed interface — never localized strings. `UiState` field errors and `XxxEffect.ShowError` carry `XxxError` values. String resolution happens exclusively in Compose screens via `stringResource()`, resolved before any `LaunchedEffect`/`collect` block.

---

## Technology Stack

KMP + CMP, Supabase (`supabase-kt`, `compose-auth`), Ktor, kotlinx.serialization, Koin, Compose Navigation (Multiplatform), Coil 3, ML Kit (Android) / AVFoundation (iOS) for barcode scanning.

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

## Common Pitfalls

- **Do not** create new packages or directories without asking first.
- **Do not** skip writing the failing test first.
- **Do not** commit API keys, Supabase URLs, or secrets.
- **Do not** manually construct class instances in Compose screens — always inject via Koin.
- **Do not** use `expect`/`actual` unless truly necessary (barcode scanning, platform permissions).
- **Do not** write `catch (e: Exception)` in a `suspend` function without rethrowing `CancellationException` first — swallowing it breaks structured concurrency. Pattern: `catch (e: CancellationException) { throw e }` before the broad catch.
