## Project Overview

Books-KMP is a cross-platform mobile book library app built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**, backed by **Supabase** (auth, database, RLS). Users scan or enter ISBNs to populate book metadata from the Google Books API, then organize their collection with a flexible tagging system.

**Target platforms:** Android and iOS from a single shared codebase.

See `docs/overview.md` for tech-choice rationale, schema notes, and planned features. Schema, models, screens, and navigation are authoritative in code.

When planning a feature or starting implementation, read `.claude/decisions.md` first to account for past decisions that may affect the approach. When a notable decision is made during a session — a spec that evolved, a code approach chosen over an alternative, or a trade-off accepted — log it in `.claude/decisions.md` using the format defined there.

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