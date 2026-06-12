CREATE TABLE IF NOT EXISTS public.profiles (
    id          uuid        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    is_premium  boolean     NOT NULL DEFAULT false,
    created_at  timestamptz DEFAULT now(),
    updated_at  timestamptz DEFAULT now()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Seed profile row on user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, is_premium)
    VALUES (NEW.id, false)
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'handle_new_user failed for user %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created_profile ON auth.users;

CREATE TRIGGER on_auth_user_created_profile
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

DROP TRIGGER IF EXISTS set_profiles_updated_at ON public.profiles;

CREATE TRIGGER set_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.update_timestamp();
