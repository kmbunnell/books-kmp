// Pure helpers for the lookup-book Edge Function, extracted from index.ts so
// they can be unit-tested without triggering the module-level Deno.serve().

export interface IndustryIdentifier {
  type: string;
  identifier: string;
}

export function extractIsbn(identifiers?: IndustryIdentifier[]): string | null {
  if (!Array.isArray(identifiers)) return null;
  return (
    identifiers.find((id) => id.type === "ISBN_13")?.identifier ??
    identifiers.find((id) => id.type === "ISBN_10")?.identifier ??
    null
  );
}

export interface ParsedIsbnRequest {
  type: "isbn";
  isbn: string;
}

export interface ParsedTitleRequest {
  type: "title";
  query: string;
}

export type ParsedRequest = ParsedIsbnRequest | ParsedTitleRequest;

export interface ParseSuccess {
  ok: true;
  value: ParsedRequest;
}

export interface ParseFailure {
  ok: false;
  error: string;
}

export type ParseResult = ParseSuccess | ParseFailure;

export function parseRequest(bodyText: string): ParseResult {
  let body: unknown;
  try {
    body = JSON.parse(bodyText);
  } catch (_e) {
    return { ok: false, error: "Invalid JSON body" };
  }

  if (typeof body !== "object" || body === null) {
    return { ok: false, error: "Body must be a JSON object" };
  }

  const obj = body as Record<string, unknown>;
  const type = obj["type"];

  if (type === "isbn") {
    const isbn = obj["isbn"];
    if (typeof isbn !== "string" || isbn.trim() === "") {
      return { ok: false, error: "Missing or invalid 'isbn'" };
    }
    const trimmed = isbn.trim();
    if (!/^\d{9}[\dX]$|^\d{13}$/.test(trimmed)) {
      return { ok: false, error: "Invalid ISBN format — expected 10 or 13 digits" };
    }
    return { ok: true, value: { type: "isbn", isbn: trimmed } };
  }

  if (type === "title") {
    const query = obj["query"];
    if (typeof query !== "string" || query.trim() === "") {
      return { ok: false, error: "Missing or invalid 'query'" };
    }
    return { ok: true, value: { type: "title", query: query.trim() } };
  }

  return { ok: false, error: "Missing or invalid 'type' (expected 'isbn' or 'title')" };
}
