# Books-KMP Overview

The non-derivable context for this project: the *why* behind tech choices, design decisions that aren't obvious from the code, and longer-term ideas. 
For the schema itself read `supabase/migrations/`. For data models read `shared/.../domain/model/`. For screens and navigation read `composeApp/.../ui/` and `NavDestination.kt`.

---

## Tech Choices & Rationale

- **Kotlin Multiplatform + Compose Multiplatform** — single codebase for Android and iOS, including UI.
- **Supabase** — auth, Postgres, and Row Level Security in one managed backend; no separate auth service to operate.
- **Open Library** for ISBN lookups — free, no API key, no quota signup. Tradeoff: occasional gaps in metadata.
- **Coil 3** for image loading — works in `commonMain`, no `expect`/`actual` needed.
- **Koin** for DI — KMP-friendly and lightweight.
- **Hand-rolled fakes over mocking libraries** in tests — most mock libraries don't support KMP cleanly.

---

## Schema Design Notes

- **Junction table `book_tags`** instead of a `tags text[]` column on `books`: enables FKs, indexed joins, and cascade deletes.
- **Partial unique index** on `(user_id, isbn) where isbn is not null`: prevents duplicate ISBNs per user while still allowing many manual entries with `null` ISBN.
- **`books.user_id` defaults to `auth.uid()`**: clients can omit the column on insert; RLS still enforces ownership via `with check`.
- **`book_tags` has no `user_id`**: RLS authorizes through the parent `books` row instead.
- **`protect_default_tags` trigger**: blocks renames/deletes of seeded default tags at the DB level, but allows cascade deletes when the parent user is removed.

---

## Default Tags (Glossary)

Seeded server-side on signup via the `seed_default_tags` trigger. Cannot be renamed or deleted.

| Tag | Meaning |
|---|---|
| Read | User has read this book |
| Hardback | Edition is a hardback copy |
| Paperback | Edition is a paperback copy |
| ARC | Advance Reader Copy |
| Special Edition | Special or limited edition |
| Signed | Copy is signed by the author |

---
