# DATABASE.md — Books-KMP Database Reference

Supabase PostgreSQL schema, Row Level Security policies, triggers, and data flow patterns.

---

## Tables

```sql
-- Books table
create table books (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid references auth.users(id) on delete cascade not null,
  isbn            text,
  title           text not null,
  authors         text[] not null,
  cover_image_url text,
  created_at      timestamptz default now()
);

-- Tags table
create table tags (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid references auth.users(id) on delete cascade not null,
  name            text not null,
  is_default      boolean default false,
  created_at      timestamptz default now(),
  unique(user_id, name)
);

-- Junction table for book-tag relationships
create table book_tags (
  book_id         uuid references books(id) on delete cascade not null,
  tag_id          uuid references tags(id) on delete cascade not null,
  primary key (book_id, tag_id)
);

-- Indexes
create index idx_books_user_id on books(user_id);
create index idx_tags_user_id on tags(user_id);
create index idx_book_tags_book_id on book_tags(book_id);
create index idx_book_tags_tag_id on book_tags(tag_id);
```

---

## Row Level Security (RLS)

RLS ensures each user can only access their own data. Supabase Auth automatically injects the authenticated user's ID into every request.

```sql
alter table books enable row level security;
alter table tags enable row level security;
alter table book_tags enable row level security;

-- Books: users can only CRUD their own books
create policy "Users manage own books"
  on books for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Tags: users can only CRUD their own tags
create policy "Users manage own tags"
  on tags for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Book_tags: users can only manage tags on their own books
create policy "Users manage own book tags"
  on book_tags for all
  using (
    exists (
      select 1 from books
      where books.id = book_tags.book_id
      and books.user_id = auth.uid()
    )
  )
  with check (
    exists (
      select 1 from books
      where books.id = book_tags.book_id
      and books.user_id = auth.uid()
    )
  );
```

---

## Tag Seeding Trigger

When a new user signs up, this trigger automatically seeds the six default tags. Runs server-side on the `auth.users` insert event — the client does not need to handle initialization.

```sql
create or replace function seed_default_tags()
returns trigger as $$
begin
  insert into tags (user_id, name, is_default) values
    (new.id, 'Read', true),
    (new.id, 'Hardback', true),
    (new.id, 'Paperback', true),
    (new.id, 'ARC', true),
    (new.id, 'Special Edition', true),
    (new.id, 'Signed', true);
  return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function seed_default_tags();
```

---

## Default Tags

| Tag Name | Purpose |
|---|---|
| Read | Marks a book as read by the user |
| Hardback | Edition is a hardback copy |
| Paperback | Edition is a paperback copy |
| ARC | Advance Reader Copy |
| Special Edition | A special or limited edition |
| Signed | The copy is signed by the author |

Default tags (`is_default = true`) cannot be renamed or deleted by the user.

---

## Data Flow & Efficiency

### Fetch Once, Filter Locally

On app launch, fetch all books and all tags for the current user in two queries. All filtering (by tag, search text) and sorting (title A–Z, author A–Z) happens client-side in memory.

### Queries Per Action

| User Action | API Calls | Notes |
|---|---|---|
| App launch | 2 | One for books, one for tags (with book_tags joined) |
| Add a book | 1 | Single insert into books |
| Toggle tag on a book | 1 | Single insert or delete on book_tags |
| Create/rename/delete a tag | 1 | Single operation; cascade handles cleanup |
| Delete a book | 1 | Single delete; cascade removes book_tags |
| Change filter/sort | 0 | Client-side only |
| Search by text | 0 | Client-side only |

Optimistic local updates after mutations — no full refetch needed.

---

## Design Decisions

- **Junction table (`book_tags`)** over tag ID arrays: enables foreign keys, indexing, clean joins, and automatic cascade deletes.
- **Realtime subscriptions** (via `realtime-kt`) are available for future cross-device sync but are not used in v1. The app relies on fetch-on-launch and optimistic local updates.
