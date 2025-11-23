-- ========================================
-- SUPABASE STORAGE BUCKETS SETUP (FIXED)
-- Run this in Supabase SQL Editor OR use the Dashboard UI
-- ========================================

-- Note: Storage buckets are typically created via the Supabase Dashboard UI
-- Go to: Storage → Create new bucket

-- However, you can also create them via SQL:

-- 1. Create expense-receipts bucket
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'expense-receipts',
  'expense-receipts',
  true,
  5242880, -- 5MB limit
  ARRAY['image/jpeg', 'image/png', 'image/jpg', 'image/gif', 'image/webp']
)
ON CONFLICT (id) DO NOTHING;

-- 2. Create payment-proofs bucket
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'payment-proofs',
  'payment-proofs',
  true,
  5242880, -- 5MB limit
  ARRAY['image/jpeg', 'image/png', 'image/jpg', 'image/gif', 'image/webp']
)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- STORAGE RLS POLICIES (FIXED TYPE CASTING)
-- ========================================

