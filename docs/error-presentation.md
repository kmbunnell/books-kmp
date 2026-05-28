# Error Presentation Standard

This document is the canonical reference for how Books-KMP presents errors and other
non-fatal failure feedback to the user. It is referenced from `AGENTS.md` and is the
source of truth for the audit tasks tracked in SHELVD-100.

The standard defines **three tiers**. Every user-visible error must map to exactly one.

| Tier | Component | When to use |
|---|---|---|
| 1. Transient | `AppSnackbarHost` (Material 3 `Snackbar`) | Operation-level failures that the user can ignore or retry, where blocking UI is overkill. |
| 2. Inline | `InlineErrorText` | Form-field validation — the message must sit next to the invalid input. |
| 3. Confirmation | `ConfirmationDialog` (Material 3 `AlertDialog`) | Destructive or irreversible actions that require an explicit "yes / cancel" decision **before** the action runs. |

> The three reference composables live in
> `composeApp/src/commonMain/kotlin/com/example/books_kmp/ui/components/`.

---

## Tier 1 — Snackbar (transient)

**Component:** `AppSnackbarHost`

**Use for:**
- Network or server failures from a one-shot user action (e.g. "Failed to delete book").
- Background operations whose failure does not block subsequent navigation.
- Success notices for fire-and-forget actions when no inline confirmation exists.

**Do not use for:**
- Field-level validation (use Tier 2).
- Destructive confirmations (use Tier 3).
- Errors that require the user to fix something before continuing — use an inline
  error banner with a Retry button on the screen body instead.
- Retryable operation errors that block the current user flow (e.g. network
  failure mid-lookup, rate limit). Snackbars disappear before the user can act
  on them; use an inline error banner with a Retry button on the screen body
  instead.

**Wiring pattern:**

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(Res.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyEffect.ShowError -> snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        // screen content
    }
}
```

String resolution happens in the composable, **before** the `collect` block, per the
ViewModel-error rule in `AGENTS.md`.

---

## Tier 2 — Inline error (form validation)

**Component:** `InlineErrorText`

**Use for:**
- Synchronous form validation messages tied to a single field
  (empty, malformed email, password mismatch, etc.).
- Per-row error states in lists, where the message must sit with the row.

**Do not use for:**
- Whole-screen failures (use Tier 1 snackbar or a body-level error state with Retry).

**Usage pattern:**

```kotlin
Column {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        isError = emailError != null,
    )
    if (emailError != null) {
        InlineErrorText(message = stringResource(emailError.messageRes))
    }
}
```

The `XxxError` value lives in `UiState`; the composable resolves it to a string via
`stringResource()`.

---

## Tier 3 — Confirmation dialog (destructive)

**Component:** `ConfirmationDialog`

**Use for:**
- Deleting user data (a book, a tag, an account).
- Discarding unsaved changes when navigating away.
- Any action whose failure mode is "the user did not mean to do that".

**Do not use for:**
- Surfacing the *result* of an operation — use Tier 1 snackbar.
- Routine confirmations where a snackbar with an Undo action would suffice.

**Usage pattern:**

```kotlin
if (uiState.showDeleteConfirmation) {
    ConfirmationDialog(
        title = stringResource(Res.string.title_delete_book),
        message = stringResource(Res.string.message_delete_book),
        confirmLabel = stringResource(Res.string.button_confirm_delete),
        dismissLabel = stringResource(Res.string.button_cancel),
        onConfirm = { viewModel.onIntent(MyIntent.ConfirmDelete) },
        onDismiss = { viewModel.onIntent(MyIntent.CancelDelete) },
    )
}
```

`onDismiss` is wired to both the cancel button and `onDismissRequest` (back press /
scrim tap) — a single dismissal callback covers both.

---

## Existing screen-specific dialogs

`DuplicateBookDialog` predates this standard. It remains in place for now and may be
migrated to `ConfirmationDialog` in a future refactor. New destructive confirmations
should use `ConfirmationDialog` directly.

---

## Testing

Each tier has a Compose UI test in
`composeApp/src/androidUnitTest/kotlin/com/example/books_kmp/ui/components/ErrorPresentationComponentsTest.kt`.
Test tags for all three components live under `TestTags.ErrorPresentation`.
