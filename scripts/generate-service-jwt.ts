// Generates an ES256 JWT signed with the Supabase auth container's JWK.
// Used in CI to produce typed-role JWTs without hardcoded secrets.
// LOCAL DEV / CI ONLY — DO NOT RUN AGAINST A HOSTED SUPABASE PROJECT.
//
// Usage:
//   deno run --allow-run scripts/generate-service-jwt.ts [container-name] [role]
//
// Arguments:
//   container-name  Docker container name to inspect (default: supabase_auth_books-kmp)
//   role            JWT role claim value (default: service_role)
//
// Prints the JWT to stdout so the caller can capture it:
//   SERVICE_ROLE_JWT=$(deno run --allow-run scripts/generate-service-jwt.ts)
//   ANON_JWT=$(deno run --allow-run scripts/generate-service-jwt.ts supabase_auth_books-kmp anon)

const containerName = Deno.args[0] ?? "supabase_auth_books-kmp";
const role = Deno.args[1] ?? "service_role";

const { stdout, success, stderr } = await new Deno.Command("docker", {
  args: ["inspect", containerName],
  stdout: "piped",
  stderr: "piped",
}).output();

if (!success) {
  const errText = new TextDecoder().decode(stderr);
  throw new Error(`docker inspect failed for "${containerName}": ${errText}`);
}

const inspectData = JSON.parse(new TextDecoder().decode(stdout));
const envVars: string[] = inspectData[0]?.Config?.Env ?? [];
const jwkKeysEntry = envVars.find((e: string) => e.startsWith("GOTRUE_JWT_KEYS="));
if (!jwkKeysEntry) {
  throw new Error(`GOTRUE_JWT_KEYS not found in env of container "${containerName}"`);
}

const jwkKeys = JSON.parse(jwkKeysEntry.split("=").slice(1).join("="));
const jwk = jwkKeys[0];

const key = await crypto.subtle.importKey(
  "jwk",
  jwk,
  { name: "ECDSA", namedCurve: "P-256" },
  false,
  ["sign"],
);

function enc(s: string): string {
  return btoa(String.fromCharCode(...new TextEncoder().encode(s)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

function encB(b: ArrayBuffer): string {
  return btoa(String.fromCharCode(...new Uint8Array(b)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

const header = enc(JSON.stringify({ alg: "ES256", typ: "JWT", kid: jwk.kid }));
const payload = enc(
  JSON.stringify({ iss: "supabase", role, aud: "authenticated", exp: Math.floor(Date.now() / 1000) + 3600 }),
);
const signature = encB(
  await crypto.subtle.sign(
    { name: "ECDSA", hash: "SHA-256" },
    key,
    new TextEncoder().encode(`${header}.${payload}`),
  ),
);

console.log(`${header}.${payload}.${signature}`);
