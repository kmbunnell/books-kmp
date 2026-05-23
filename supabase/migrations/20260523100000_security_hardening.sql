-- Security hardening: pin search_path on trigger functions and tighten book_tags INSERT policy.

-- ============================================================
-- Pin search_path on trigger functions (Supabase security linter)
-- ============================================================
-- Without SET search_path, a superuser could shadow referenced schemas.
-- protect_default_tags includes `auth` because it queries auth.users.

CREATE OR REPLACE FUNCTION public.protect_default_tags()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public, auth
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.is_default = true THEN
            IF NOT EXISTS (SELECT 1 FROM auth.users WHERE id = OLD.user_id) THEN
                RETURN OLD;
            END IF;
            RAISE EXCEPTION 'Cannot delete a default tag (id=%)', OLD.id;
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.is_default = true THEN
            RAISE EXCEPTION 'Cannot modify a default tag (id=%)', OLD.id;
        END IF;
        IF NEW.is_default = true AND OLD.is_default = false THEN
            RAISE EXCEPTION 'Cannot promote a custom tag to default (id=%)', OLD.id;
        END IF;
        RETURN NEW;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.update_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- ============================================================
-- book_tags INSERT: also verify the tag belongs to the inserting user
-- ============================================================
-- Previous policy only checked book ownership. A user who knew another
-- user's tag UUID could link it to their own book.

DROP POLICY IF EXISTS "Users can insert their own book_tags" ON book_tags;

CREATE POLICY "Users can insert their own book_tags"
    ON book_tags FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM books
            WHERE books.id = book_tags.book_id
              AND books.user_id = auth.uid()
        )
        AND EXISTS (
            SELECT 1 FROM tags
            WHERE tags.id = book_tags.tag_id
              AND tags.user_id = auth.uid()
        )
    );
