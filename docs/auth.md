# AUTH.md — Books-KMP Authentication Reference

Supabase Auth configuration, providers, session management, and integration details.

---

## Providers

- **Email/Password** — standard registration and login.
- **Google Sign-In** — native on both platforms via `compose-auth` plugin.
- **Apple Sign-In** — native on iOS via AuthenticationServices, OAuth redirect on Android. Required by App Store for apps offering third-party social login.

---

## Supabase Auth Modules

| Module | Purpose |
|---|---|
| auth-kt | Core auth: email/password, OAuth flows, session management, token refresh, persistence |
| compose-auth | Composable functions for native Google and Apple sign-in buttons |

Both modules work entirely from `commonMain` — no `expect`/`actual` wiring needed for sign-in logic.

---

## Session Flow

```
On launch:
  Initializing → Authenticated (valid session found)
               → NotAuthenticated (no session or refresh failed)

After sign-in:
  NotAuthenticated → Authenticated → navigate to Library

After sign-out:
  Authenticated → NotAuthenticated → navigate to Sign In
```

The app observes the `sessionStatus` Flow, which emits the current authentication state. The SDK handles session persistence across app restarts and automatic token refresh.

---

## Integration Notes

- Supabase Auth is the single identity layer — no separate auth backend.
- Auth integrates automatically with Row Level Security (see `docs/DATABASE.md`). The authenticated user's ID is injected into every PostgREST request.
- On successful authentication, navigate from the auth flow to the main flow and clear the back stack.
- On sign-out, clear local state and navigate back to the Sign In screen.
