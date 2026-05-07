-- Allow multiple copies of the same book (same ISBN) per user.
-- Each row is a distinct copy; tags attach to the copy via book_tags.
DROP INDEX IF EXISTS idx_books_user_isbn;
