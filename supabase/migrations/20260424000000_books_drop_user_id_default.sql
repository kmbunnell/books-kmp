-- Remove database-level default so the app must explicitly supply user_id on insert.
-- books table previously used DEFAULT auth.uid(); ownership is now set by the client.
ALTER TABLE books ALTER COLUMN user_id DROP DEFAULT;
