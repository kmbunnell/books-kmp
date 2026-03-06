# SPECS.md — Books-KMP Feature Specifications

Detailed specs for data models, APIs, screens, and functional requirements. For architecture and dev workflow, see `CLAUDE.md`. For database schema, see `docs/DATABASE.md`. For authentication, see `docs/AUTH.md`.

---

## Data Models

### Book

| Field | Type | Required | Notes |
|---|---|---|---|
| id | String (UUID) | Yes | Auto-generated unique identifier |
| isbn | String | No | ISBN-10 or ISBN-13; null for manual entries |
| title | String | Yes | From API or manual entry |
| authors | List\<String\> | Yes | One or more authors |
| coverImageUrl | String | No | Remote URL from Google Books API |
| tags | List\<String\> | Yes | Tag IDs applied to this book |

### Tag

| Field | Type | Notes |
|---|---|---|
| id | String (UUID) | Auto-generated |
| name | String | Display name |
| isDefault | Boolean | true for system tags, false for user-created |

---

## Google Books API

### Endpoint

```
GET https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}
```

### Response Mapping

| App Field | API Response Path | Fallback |
|---|---|---|
| title | items[0].volumeInfo.title | Required from manual entry |
| authors | items[0].volumeInfo.authors | Required from manual entry |
| coverImageUrl | items[0].volumeInfo.imageLinks.thumbnail | null (use default image) |

### Error Handling

- **No results** (`totalItems == 0`): Prompt user for manual entry.
- **Network error**: Show retry option with clear error message.
- **Rate limit / 429**: Back off and retry with exponential delay.
- **Malformed response**: Log error, treat as no results, offer manual entry.

### API Key

Stored in `local.properties`, injected via `BuildConfig`. Never committed to VCS.

---

## Barcode Scanning

### Platform Implementations

| Platform | Library | Notes |
|---|---|---|
| Android | ML Kit Barcode Scanning | Bundled model, no network needed, EAN-13 and EAN-8 |
| iOS | AVFoundation (AVCaptureMetadataOutput) | Native framework, EAN-13 and EAN-8 |

### Shared Interface (expect/actual)

```kotlin
// commonMain
expect class BarcodeScanner {
    suspend fun scanBarcode(): BarcodeScanResult
}

sealed class BarcodeScanResult {
    data class Success(val isbn: String) : BarcodeScanResult()
    data class Error(val message: String) : BarcodeScanResult()
    object Cancelled : BarcodeScanResult()
}
```

### Permissions

- **Android**: `CAMERA` in AndroidManifest.xml; runtime request before first scan.
- **iOS**: `NSCameraUsageDescription` in Info.plist.

---

## Image Handling

- **Coil 3** for async loading with automatic memory + disk caching.
- **Primary**: Load `coverImageUrl` from Google Books API.
- **Fallback**: Bundled default placeholder PNG from `composeApp/commonMain/composeResources`.

---

## Screens

| Screen | Description |
|---|---|
| Splash / Auth Check | Routes to Sign In or Library based on session |
| Sign In | Email/password form, Google sign-in button, Apple sign-in button (iOS) |
| Sign Up | Email/password registration form |
| Library (Home) | Grid of book covers with tag filter bar, sort options, and search |
| Book Detail | Full book info with tag toggle chips and delete option |
| Add Book | ISBN scan (camera) or manual ISBN text input |
| Manual Entry | Form for title and author when ISBN is not found |
| Tag Management | List of all tags; create, rename, and delete custom tags |

---

## Navigation

```
Auth Flow:
  Splash → Sign In ↔ Sign Up

Main Flow:
  Library (Home)
    ├── → Book Detail → (back to Library)
    ├── → Add Book → Manual Entry (if ISBN not found)
    └── → Tag Management → (back to Library)
```

Successful auth clears the back stack. Uses Compose Navigation for Multiplatform.

### Responsive Layout

- Phone portrait: 3 columns
- Phone landscape / small tablet: 4 columns
- Tablet: 5–6 columns

Uses `LazyVerticalGrid` with adaptive cell sizing.

---

## Functional Requirements

### Book Entry

- Camera-based barcode scanning (ISBN-10 and ISBN-13).
- Manual ISBN entry via text field.
- Google Books API lookup on ISBN submission.
- Manual entry form when ISBN is not found.
- Default placeholder cover image when no cover URL is available.
- Duplicate ISBN prevention: warn user, do not create duplicate.

### Tagging

- Tags act as virtual shelves (many-to-many relationship).
- Six default tags seeded on signup (cannot be renamed/deleted).
- Users can create, rename, and delete custom tags.
- Deleting a tag cascades to remove it from all books.

### Library View

- Scrollable grid showing cover, title, author.
- Tag-based filtering (multiple tags = AND logic).
- Sort by title (A–Z) or author last name (A–Z).
- Search bar filters by title or author text.

### Book Detail

- Shows cover, title, author(s), ISBN, and applied tags.
- Tags can be toggled on/off.
- Book metadata is read-only after import.
- Books can be deleted with a confirmation step.

---

## Open Questions & Future Considerations

| Topic | Notes |
|---|---|
| Series support | Google Books API lacks a dedicated series field |
| Bulk import | Import ISBNs from CSV or text file |
| Export | Export library as CSV or JSON |
| Reading stats | Track books read per month/year, reading goals |
| Wishlist | Separate section for books not yet owned |
| Social features | Share shelves or reading lists |
| Cover image override | Custom photo upload (requires Supabase Storage) |
| Realtime sync | Cross-device syncing via Supabase Realtime |
