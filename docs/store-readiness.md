# Store Readiness — Pre-Launch Checklist

Items that must be resolved before submitting to the App Store / Google Play. Add to this list as blockers are discovered during development.

See Epic 10 in `docs/project-planning.md` for the associated Jira epic.

---

## Email / Auth

- [ ] **Custom sending domain for Resend** — The current Supabase SMTP setup uses a `@resend.dev` sender address, which is for development/testing only. `@resend.dev` can only send to the single email address registered with Resend — all other recipients are blocked. Before launch, register a real domain, add it to Resend (DNS verification), update the Supabase SMTP sender address to `noreply@yourdomain.com` (or equivalent), and update any email templates that reference the sender name/address.
- [ ] **App Links (Android) / Universal Links (iOS) for email verification** — Email confirmation links currently cannot be handled by the app because the Supabase `/auth/v1/verify` endpoint requires an API key header that a browser cannot supply. The production fix is App Links / Universal Links: `https://` deep links that the OS intercepts and opens in the app without going through a browser. This requires a real domain with `.well-known/assetlinks.json` (Android) and an Apple App Site Association file (iOS) hosted on it. **Email confirmation is disabled in Supabase during development** — re-enable it once the domain and App Links are configured.
- [ ] **Re-enable email confirmation in Supabase** — Disabled for development (see above). Must be re-enabled before launch.

---

## Android

_(nothing yet)_

## iOS

_(nothing yet)_

## Backend / Supabase

_(nothing yet)_

## Legal / Compliance

_(nothing yet)_
