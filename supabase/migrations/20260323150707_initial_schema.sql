-- Migration: Create books table (Stories 2-9 shared migration)
-- Idempotent: safe to re-run in CI

-- Create the books table
CREATE TABLE IF NOT EXISTS books (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    isbn            text,
    title           text        NOT NULL,
    authors         text[]      NOT NULL,
    cover_image_url text,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- Partial unique index: enforce one ISBN per user, allow multiple NULL ISBNs
CREATE UNIQUE INDEX IF NOT EXISTS idx_books_user_isbn
    ON books(user_id, isbn)
    WHERE isbn IS NOT NULL;

-- Enable Row Level Security (recommended for Supabase)
ALTER TABLE books ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- Tags table (Story 3)
-- ============================================================

CREATE TABLE IF NOT EXISTS tags (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name        text        NOT NULL,
    is_default  boolean     DEFAULT false,
    created_at  timestamptz DEFAULT now(),
    updated_at  timestamptz DEFAULT now(),

    -- Prevent duplicate tag names per user
    CONSTRAINT uq_tags_user_name UNIQUE (user_id, name)
);

ALTER TABLE tags ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- Book-Tags junction table (Story 4)
-- ============================================================

CREATE TABLE IF NOT EXISTS book_tags (
    book_id uuid NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    tag_id  uuid NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,

    PRIMARY KEY (book_id, tag_id)
);

ALTER TABLE book_tags ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- Performance indexes (Story 5)
-- ============================================================

-- Supports RLS-filtered queries on books by user
CREATE INDEX IF NOT EXISTS idx_books_user_id
    ON books(user_id);

-- Supports RLS-filtered queries on tags by user
CREATE INDEX IF NOT EXISTS idx_tags_user_id
    ON tags(user_id);

-- Supports find-all-books-with-this-tag joins
-- (idx_book_tags_book_id intentionally omitted — covered by composite PK)
CREATE INDEX IF NOT EXISTS idx_book_tags_tag_id
    ON book_tags(tag_id);

-- ============================================================
-- Seed default tags on user signup (Story 6)
-- ============================================================

CREATE OR REPLACE FUNCTION public.seed_default_tags()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.tags (user_id, name, is_default)
    VALUES
        (NEW.id, 'Read',            true),
        (NEW.id, 'Hardback',        true),
        (NEW.id, 'Paperback',       true),
        (NEW.id, 'ARC',             true),
        (NEW.id, 'Special Edition',  true),
        (NEW.id, 'Signed',          true);

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'seed_default_tags failed for user %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$$;

-- Drop and recreate to ensure idempotency (CREATE TRIGGER has no IF NOT EXISTS)
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.seed_default_tags();

-- ============================================================
-- Protect default tags from modification/deletion (Story 7)
-- ============================================================
-- NOTE on CASCADE behaviour: When a user is deleted from auth.users,
-- ON DELETE CASCADE fires this trigger. We allow the delete if the
-- parent user no longer exists (i.e. it's a cascade from user deletion).

CREATE OR REPLACE FUNCTION public.protect_default_tags()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Block DELETE of default tags (unless cascading from user deletion)
    IF TG_OP = 'DELETE' THEN
        IF OLD.is_default = true THEN
            -- Allow if the parent user is being deleted (cascade)
            IF NOT EXISTS (SELECT 1 FROM auth.users WHERE id = OLD.user_id) THEN
                RETURN OLD;
            END IF;
            RAISE EXCEPTION 'Cannot delete a default tag (id=%)', OLD.id;
        END IF;
        RETURN OLD;
    END IF;

    -- Block UPDATE of default tags
    IF TG_OP = 'UPDATE' THEN
        -- Prevent any modification of a default tag
        IF OLD.is_default = true THEN
            RAISE EXCEPTION 'Cannot modify a default tag (id=%)', OLD.id;
        END IF;
        -- Prevent promoting a custom tag to default
        IF NEW.is_default = true AND OLD.is_default = false THEN
            RAISE EXCEPTION 'Cannot promote a custom tag to default (id=%)', OLD.id;
        END IF;
        RETURN NEW;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_default_tags ON tags;

CREATE TRIGGER trg_protect_default_tags
    BEFORE UPDATE OR DELETE ON tags
    FOR EACH ROW
    EXECUTE FUNCTION public.protect_default_tags();

-- ============================================================
-- Auto-update updated_at timestamp (Story 9)
-- ============================================================

CREATE OR REPLACE FUNCTION public.update_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS set_books_updated_at ON books;

CREATE TRIGGER set_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW
    EXECUTE FUNCTION public.update_timestamp();

DROP TRIGGER IF EXISTS set_tags_updated_at ON tags;

CREATE TRIGGER set_tags_updated_at
    BEFORE UPDATE ON tags
    FOR EACH ROW
    EXECUTE FUNCTION public.update_timestamp();
