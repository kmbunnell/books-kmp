# Staging Environment

Shelvd has a dedicated **staging** environment backed by a separate Supabase project. Staging builds are configured to install side-by-side with production builds — both can be present on the same device at the same time, each with a distinct app icon label so you can tell them apart at a glance.

Use staging to validate changes against a production-like backend without risking production data.

---

## Overview

- Staging is a **separate Supabase project** with its own URL, anon key, database, and users. No data is shared with production.
- Staging builds use a distinct application ID (Android) and bundle ID (iOS) suffix so they install alongside production.
- Staging builds display a different app name on the home screen:
  - **Android staging:** "Books Staging" — production is labelled "Bookskmp"
  - **iOS staging:** "Shelvd Staging"

---

## Credentials

Obtain the staging Supabase project URL and anon key from the team (or from the staging project's Supabase dashboard under *Project Settings → API*).

Add both keys to your `local.properties` at the repo root (alongside the existing production keys):

```properties
SUPABASE_URL=...               # production (existing)
SUPABASE_ANON_KEY=...          # production (existing)
STAGING_SUPABASE_URL=https://<staging-ref>.supabase.co
STAGING_SUPABASE_ANON_KEY=<staging-anon-key>
```

`local.properties` is gitignored — never commit these values. The `STAGING_*` keys are only required when building the staging flavor; production builds work without them.

---

## Android Setup

1. Ensure `STAGING_SUPABASE_URL` and `STAGING_SUPABASE_ANON_KEY` are present in `local.properties` (see [Credentials](#credentials)).
2. In Android Studio, open the **Build Variants** panel (View → Tool Windows → Build Variants).
3. Select **`stagingDebug`** for the `composeApp` module. To return to a production build, select **`productionDebug`** instead.
4. Run as normal (Shift+F10 / the green run arrow), or from the terminal:
   ```bash
   ./gradlew :composeApp:assembleStagingDebug      # staging
   ./gradlew :composeApp:assembleProductionDebug   # production
   ```

> **Note:** The project uses product flavors (`staging` / `production`), so plain `assembleDebug` no longer exists as a standalone task — use the flavored variants above. Existing Android Studio run configurations may need to be re-pointed at `productionDebug` or `stagingDebug` after pulling this change.

The resulting staging app will appear on the device labelled **"Books Staging"** with application ID `com.example.books_kmp.staging`.

---

## iOS Setup

1. Ensure `STAGING_SUPABASE_URL` and `STAGING_SUPABASE_ANON_KEY` are present in `local.properties` (see [Credentials](#credentials)).
2. Generate the staging xcconfig from your `local.properties`:
   ```bash
   ./gradlew :composeApp:generateStagingSecretsXcconfig
   ```
   This produces `iosApp/Configuration/StagingSecrets.xcconfig`. The file is gitignored. This task also runs automatically during every iOS compile, so you only need to run it manually when the staging credentials change.
3. Open `iosApp/iosApp.xcodeproj` (or the workspace) in Xcode.
4. From the scheme selector in the toolbar, choose **`iosApp Staging`**.
5. Pick a simulator or connected device and Run (Cmd+R).

The resulting app will appear labelled **"Shelvd Staging"** with bundle ID `com.example.books_kmp.Bookskmp<TEAM_ID>.staging`.

---

## Side-by-Side Installs

Because staging and production use distinct application/bundle IDs, both can be installed at the same time on the same device or simulator:

- Android: `com.example.books_kmp` (production) and `com.example.books_kmp.staging` (staging).
- iOS: production bundle ID and the same ID with a `.staging` suffix.

This lets you compare behavior across environments without uninstalling either build.

---

## Verification

After installing a staging build:

1. Launch the **"Books Staging"** / **"Shelvd Staging"** app icon (not the production one).
2. Sign in with a staging Supabase account (create one if needed — staging has its own user pool).
3. Confirm your library is empty or contains only staging data — it should be fully isolated from your production account.
4. Add a book; verify the entry appears in the staging Supabase project's `books` table, not production.

If the app connects to the wrong environment, re-check that `local.properties` contains the staging keys and that you ran `generateStagingSecretsXcconfig` (iOS) or selected `stagingDebug` (Android).
