-- ============================================================
-- 11 - user_profiles.sql
-- Extended profile data for each authenticated user.
-- Run in Supabase SQL Editor.
-- ============================================================

-- ─── Table ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS profiles (
  id          uuid        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  first_name  text,
  last_name   text,
  nickname    text,
  gender      text        CHECK (gender IN ('male', 'female', 'non-binary', 'prefer_not_to_say')),
  birthday    date,
  phone       text,
  city        text,
  country     text,
  bio         text,
  updated_at  timestamptz NOT NULL DEFAULT now()
);

-- ─── Auto-update updated_at ───────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION update_profiles_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER profiles_updated_at
  BEFORE UPDATE ON profiles
  FOR EACH ROW
  EXECUTE FUNCTION update_profiles_updated_at();

-- ─── Row Level Security ───────────────────────────────────────────────────────

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own profile"
  ON profiles FOR SELECT
  USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile"
  ON profiles FOR INSERT
  WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own profile"
  ON profiles FOR UPDATE
  USING (auth.uid() = id);

-- ─── Migration: add columns if table already exists ───────────────────────────
-- Run these if you created profiles before this version.

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS nickname text;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS gender   text CHECK (gender IN ('male', 'female', 'non-binary', 'prefer_not_to_say'));
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS phone    text;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS city     text;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS country  text;
