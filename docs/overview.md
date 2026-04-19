# Books-KMP Overview

The non-derivable context for this project: the *why* behind tech choices, what's planned but not yet built, and design decisions that aren't obvious from the code. For the schema itself read `supabase/migrations/`. For data models read `shared/.../domain/model/`. For screens and navigation read `composeApp/.../ui/` and `NavDestination.kt`.

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

## Planned & Not Yet Built

The codebase currently ships email/password auth, a Library skeleton (top bar + Add FAB only), an Add Book flow (manual ISBN → Open Library lookup → confirm), and a Manual Entry fallback. Everything below is specified but not implemented:

- **Barcode scanning** — camera-based ISBN scan. Android: ML Kit Barcode Scanning (bundled model). iOS: AVFoundation `AVCaptureMetadataOutput`. Will use `expect`/`actual` `BarcodeScanner` in `commonMain`. Permissions: Android `CAMERA`, iOS `NSCameraUsageDescription`.
- **Library grid** — `LazyVerticalGrid` of book covers (3 cols phone portrait → 5–6 cols tablet) with tag filter (multi-select AND), sort (title or author A–Z), and search by title/author.
- **Book Detail screen** — cover, metadata, tag toggle chips, delete with confirmation.
- **Tag Management screen** — create, rename, delete custom tags. Default-tag protection already enforced at DB level.
- **Google Sign-In and Apple Sign-In** — `compose-auth` dependency is declared but not wired. Apple Sign-In becomes mandatory once any third-party social login ships (App Store rule).
- **Data flow plan**: once the Library grid lands, fetch all books + all tags on launch (2 queries) and do filter/sort/search client-side. Mutations are 1 query each with optimistic local updates.

### Longer-Term Ideas

Series support (Open Library lacks a dedicated field), CSV/JSON import/export, reading stats, wishlist, social sharing, custom cover upload (needs Supabase Storage), Realtime cross-device sync (`realtime-kt`).
