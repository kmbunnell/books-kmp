# CLAUDE.md — Books-KMP (Kotlin Multiplatform)

## Project Overview

Books-KMP is a cross-platform mobile book library app built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**, backed by **Supabase** (auth, database, RLS). Users scan or enter ISBNs to populate book metadata from the Open Library API, then organize their collection with a flexible tagging system.

**Target platforms:** Android and iOS from a single shared codebase.

See `docs/overview.md` for tech-choice rationale, schema notes, and planned features. Schema, models, screens, and navigation are authoritative in code.

---

## Core Principles

### 1. Test-Driven Development (TDD)

**Red → Green → Refactor.** Write a failing test first, implement the minimum to pass, then refactor. Skip tests for pure data classes and interfaces with no logic.

**Unit tests:** Every use case and repository function with logic needs a test. When fixing a bug, write a failing test that reproduces it first. Prefer hand-rolled fakes over mocking libraries (KMP compatibility). Test files mirror production: `domain/usecase/AddBookUseCaseTest.kt` next to `AddBookUseCase.kt`. Frameworks: `kotlin.test` + `kotlinx-coroutines-test`; `Turbine` for Flow/StateFlow.

**Compose UI tests:** Cover each screen's states (loading, error, empty, populated) and interactions. Use `ComposeUiTest` with semantic matchers; inject fake ViewModels or use cases.

**Integration tests:** Use `supabase start` (local Docker) for Supabase integration tests.

### 2. Clean Architecture + MVI

Strict layer separation with a **unidirectional dependency rule**: outer layers depend on inner layers, never the reverse.

```
Presentation (MVI) → Domain ← Data
```

**Layer definitions:**

- **Domain:** Pure Kotlin. Domain models, repository interfaces, use cases. Zero platform or framework imports.
- **Data:** Repository implementations, data sources, DTOs, mappers. Depends on domain interfaces.
- **Presentation:** Compose screens, ViewModels, UI state. ViewModels contain no business logic. They may call use cases **or** repository interfaces directly — use a use case only when there is real orchestration (multiple repos, validation, transformation) that belongs to neither the ViewModel nor the repository.

**MVI pattern:** Each screen exposes `StateFlow<XxxUiState>` (immutable snapshot), accepts user actions as `sealed interface XxxIntent` via `viewModel.onIntent(intent)` — no ad-hoc methods per action — and emits side effects (navigation, toasts) through `SharedFlow<XxxEffect>` or `Channel<XxxEffect>`.

**Architecture rules:**
- Domain models and DTOs are always separate classes. Map between them explicitly. Do not leak DTO annotations (`@Serializable`, column names) into domain models.
- Repository interfaces live in the domain layer. Implementations live in the data layer.
- Use cases are single-responsibility: one public `operator fun invoke(...)` or `suspend operator fun invoke(...)`.
- Use cases, repositories, and platform gateways (including `expect` classes like `BarcodeScanner`) return `Result<T, XxxError>` — never a custom `XxxResult` sealed interface, and never a raw `String` message. Domain-layer error types (use case and repository errors) are feature-specific sealed interfaces (e.g., `AddBookError`, `SignInError`, `BarcodeScanError`), each defined in its own file in the domain layer. ViewModel-level errors co-locate with their ViewModel (see Code Style). Success data goes in `Result.Success(data)`; all failure variants (including user-cancellation, which is a failure outcome from the caller's perspective) go in `Result.Failure(XxxError.Variant)`.
- This `Result` rule applies to **every** `suspend` repository/use-case/gateway function that performs I/O — including those whose natural return type would be `Boolean`, `T?`, or `Unit`. Return `Result<Boolean, E>`, `Result<T?, E>`, or `Result<Unit, E>` accordingly. A non-`Result` return type is only valid for purely in-memory operations that cannot fail (e.g., a local cache lookup with no network path).
- **Never wrap a `Result`-returning call in `try/catch`** to re-introduce exception-based flow. Pattern-match on `Result.Success`/`Result.Failure` instead. A `try/catch` is only appropriate at the I/O boundary itself (inside the repository implementation), not in callers.
- Koin modules are the single place for dependency wiring. Never manually construct dependencies outside DI.
- ViewModels emit typed errors via a feature-specific `XxxError` sealed interface — never localized strings. `UiState` field errors and `XxxEffect.ShowError` carry `XxxError` values. String resolution happens exclusively in Compose screens via `stringResource()`, resolved before any `LaunchedEffect`/`collect` block.

---

## Technology Stack

KMP + CMP, Supabase (`supabase-kt`, `compose-auth`), Ktor, kotlinx.serialization, Koin, Compose Navigation (Multiplatform), Coil 3, ML Kit (Android) / AVFoundation (iOS) for barcode scanning.

### Platform UI in CMP

When a feature requires platform-specific UI (camera, maps, native pickers), use **`expect`/`actual` composables** backed by `AndroidView` (Android) or `UIKitView` (iOS) — not separate Activities or view controllers.

- **Android camera**: bind CameraX to `LocalLifecycleOwner.current` inside an `AndroidView`. Use `DisposableEffect` to unbind and shut down the executor when the composable leaves composition. Use `rememberUpdatedState` for any callback passed into an `AndroidView` factory so it always calls the latest lambda.
- **iOS camera**: use `UIKitView` wrapping an `AVCaptureSession`-based `UIView`. Handle permission via `suspendCancellableCoroutine` + `AVCaptureDevice.requestAccessForMediaType`.
- **One-shot callbacks** (e.g., "barcode detected"): guard with `AtomicBoolean.compareAndSet(false, true)` to prevent duplicate deliveries from concurrent camera frames.
- **Do not** start a separate `Activity` for UI flows that can live in the nav graph or as a composable overlay. The bridge/channel pattern for Activity→coroutine handoff is superseded by composable lifecycle ownership.

---

## Code Style & Conventions

- Naming: `AddBookUseCase`, `BookRepository`, `LibraryViewModel`, `LibraryUiState`, `LibraryIntent`, `LibraryEffect`.
- `sealed class` / `sealed interface` for finite states (auth status, scan results, UI state, intents, effects).
- `data class` for models, DTOs, and UI state.
- Keep functions short (~30 lines max). Prefer explicit return types on public APIs.
- Avoid `!!` — prefer `?.let`, `?:`, or explicit null checks with descriptive errors.
- One file per class for use cases, ViewModels, and repository interfaces. ViewModel-level intents, state, effects, and errors co-locate in the ViewModel file; domain-layer errors (use case/repo) each get their own file.

---

## Strings & Resources

- All user-visible strings must be defined in `composeApp/src/commonMain/composeResources/values/strings.xml` and accessed via `stringResource(Res.string.xxx)` in composables. Never hardcode display strings in Kotlin source.
- Do not add strings to `androidMain/res/values/strings.xml` — that file is Android-only and invisible to iOS. The only exception is `app_name`, which is required by `AndroidManifest.xml`.

---

## Common Pitfalls

- **Present the best architectural solution, not merely one that satisfies acceptance criteria.** Evaluate trade-offs before proposing a plan — if a cleaner design exists, recommend it even if a simpler path would compile and pass tests.
- **Do not** create new packages or directories without asking first.
- **Do not** commit API keys, Supabase URLs, or secrets.
- **Do not** use `expect`/`actual` for business logic unless truly necessary; for platform-specific UI, see the Platform UI section above.
- **Do not** use a raw `Channel` as a bridge between Android SDK callbacks and coroutines — use `suspendCancellableCoroutine` instead. It handles coroutine cancellation correctly and is the idiomatic one-shot conversion pattern.
- **Do not** write `catch (e: Exception)` in a `suspend` function without rethrowing `CancellationException` first — swallowing it breaks structured concurrency. Pattern: `catch (e: CancellationException) { throw e }` before the broad catch.
- **Do not** return `Result.Failure` for a missing authenticated user in a repository — use `error("Not authenticated")` instead. A missing user is a precondition violation (unreachable if auth is working), not a recoverable failure. The `AuthViewModel` handles session expiry before any repository is called.
- **Do not** wrap every `onIntent` branch in a single `viewModelScope.launch`. Pure `_uiState.update` calls are synchronous and need no coroutine. Only launch for intents that call `suspend` functions (repo calls, `_effects.emit()`). Mixing them delays state updates unnecessarily.
