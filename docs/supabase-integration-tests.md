# Running Supabase Integration Tests Locally

Do these steps in order. Steps 1–4 only need to be done once per terminal session
(or after `supabase stop`). Steps 5–6 only need to be repeated when function code changes.

---

## 1. Start the local Supabase stack

```bash
supabase start
```

Takes ~30 seconds. Starts Postgres, GoTrue, and the edge-function gateway on `localhost:54321`.

---

## 2. Export the publishable key

```bash
export SUPABASE_PUBLISHABLE_KEY=$(supabase status --output env | grep "PUBLISHABLE_KEY=" | sed 's/.*PUBLISHABLE_KEY=//' | tr -d '"')
```

---

## 3. Generate and export the service-role JWT

```bash
export SUPABASE_SERVICE_ROLE_JWT=$(deno run --allow-run scripts/generate-service-jwt.ts supabase_auth_books-kmp service_role)
```

---

## 4. Generate and export the anon JWT

```bash
export SUPABASE_ANON_JWT=$(deno run --allow-run scripts/generate-service-jwt.ts supabase_auth_books-kmp anon)
```

---

## 5. Serve the edge functions (background)

```bash
supabase functions serve lookup-book &
supabase functions serve evict-cache &
```

Wait a few seconds for both to be ready. You can check with:

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:54321/functions/v1/evict-cache \
  -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_JWT" \
  -H "Content-Type: application/json" -d '{}'
# Should print 200 when ready
```

---

## 6. Run the tests

```bash
deno test --allow-net --allow-env --node-modules-dir=auto supabase/functions/lookup-book/index.test.ts
deno test --allow-net --allow-env --node-modules-dir=auto supabase/functions/evict-cache/index.test.ts
```

---

## Teardown

```bash
supabase stop
```

This stops all containers. Run `supabase start` again next time.

---

## Troubleshooting

**`docker inspect` fails in step 3/4** — Supabase isn't running or the container name is wrong.
Run `docker ps | grep supabase_auth` to find the actual container name and pass it as the first
argument to the script.

**`SUPABASE_SERVICE_ROLE_JWT must be set` at test startup** — You opened a new terminal window
and lost the exported env vars. Re-run steps 2–4.

**Function returns 500 / "Service misconfigured"** — The edge runtime didn't inject
`SUPABASE_SERVICE_ROLE_KEY`. Stop and restart the function serve (`supabase functions serve …`).

**Tests pass locally but fail in CI** — Check that the CI container name
(`supabase_auth_books-kmp`) matches your project name in `supabase/config.toml`.
