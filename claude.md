# CLAUDE.md — Books-KMP (Kotlin Multiplatform)

## Project Overview

Books-KMP is a cross-platform mobile book library app built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**, backed by **Supabase** (auth, database, RLS). Users scan or enter ISBNs to populate book metadata from the Google Books API, then organize their collection with a flexible tagging system.

**Target platforms:** Android and iOS from a single shared codebase.

For detailed specs, see:
- `docs/SPECS.md` — data models, API details, screens, functional requirements
- `docs/DATABASE.md` — Supabase schema, RLS policies, triggers, data flow
- `docs/AUTH.md` — authentication providers, session flow, Supabase auth modules

---

## Core Principles

### Test-Driven Development (TDD)

- **Red → Green → Refactor.** Write a failing test first, implement the minimum code to pass it, then refactor. No production code without a corresponding test.
- Tests are not an afterthought — they are the **first artifact** of every feature or bug fix.
- Every public function in a use case or repository must have at least one unit test.
- When fixing a bug, write a test that reproduces the bug **before** writing the fix.
- Keep tests fast, isolated, and deterministic. Mock external dependencies at the repository boundary.
- Prefer hand-rolled fakes over mocking libraries for KMP compatibility.
- Use descriptive test names: `` `invoke returns error when ISBN is duplicate` ``.
- Each test should test one behavior. Prefer many small tests over few large ones.
- Test files mirror production structure: `domain/usecase/AddBookUseCaseTest.kt` alongside `domain/usecase/AddBookUseCase.kt`.
- Frameworks: `kotlin.test` + `kotlinx-coroutines-test` for domain/data; `Turbine` for Flow testing in ViewModels; `supabase start` for integration tests.

### Clean Architecture

Strict layer separation with a **unidirectional dependency rule**: outer layers depend on inner layers, never the reverse.

```
Presentation → Domain ← Data
```

- **Domain layer** (`shared/commonMain/.../domain/`): Pure Kotlin. Domain models, repository interfaces, and use cases. Zero platform or framework imports.
- **Data layer** (`shared/commonMain/.../data/` + platform source sets): Repository implementations, data sources, DTOs, and mappers. Depends on domain interfaces.
- **Presentation layer** (`composeApp/commonMain/`): Compose screens, ViewModels, and UI state. ViewModels depend on use cases only — no business logic in ViewModels.

**Rules:**
- Domain models and DTOs are always separate classes. Map between them explicitly. Do not leak DTO annotations (`@Serializable`, column names) into domain models.
- Repository interfaces live in the domain layer. Implementations live in the data layer.
- Use cases are single-responsibility: one public `operator fun invoke(...)` or `suspend operator fun invoke(...)`.
- ViewModels expose UI state via `StateFlow` and accept user intents as function calls.
- Koin modules are the single place for dependency wiring. Never manually construct dependencies outside DI.
- Shared UI elements belong in a common `components` package. Centralize constants in a single config object.

---

## Project Structure

Create packages and directories incrementally as features are built — do not scaffold empty directories upfront. Always confirm the package name, location, and purpose before adding. What matters is layer separation (`domain/`, `data/`, `ui/`), not exact paths.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Shared logic | Kotlin Multiplatform (KMP) |
| Shared UI | Compose Multiplatform (CMP) |
| Backend / Auth | Supabase (`supabase-kt` SDK) |
| Image loading | Coil 3 (Multiplatform) |
| Barcode scanning | ML Kit (Android) / AVFoundation (iOS) via `expect`/`actual` |
| Networking | Ktor Client |
| Serialization | kotlinx.serialization |
| DI | Koin |
| Navigation | Compose Navigation (Multiplatform) |

---

## Code Style & Conventions

- Naming: `AddBookUseCase`, `BookRepository`, `LibraryViewModel`, `LibraryUiState`.
- `sealed class` / `sealed interface` for finite states (auth status, scan results, UI state).
- `data class` for models and DTOs.
- Keep functions short (~30 lines max). Prefer explicit return types on public APIs.
- Avoid `!!` — prefer `?.let`, `?:`, or explicit null checks with descriptive errors.

---

## Secrets

Secrets (`SUPABASE_URL`, `SUPABASE_ANON_KEY`) go in `local.properties`, injected via `BuildConfig`. **Never commit secrets.**

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
