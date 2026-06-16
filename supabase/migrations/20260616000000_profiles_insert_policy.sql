-- Allow users to insert their own profile row (needed for upsert from the app)
CREATE POLICY "Users can insert their own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);
