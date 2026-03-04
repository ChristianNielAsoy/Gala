-- =============================================
-- GALA APP - Feature Migration v6
-- Run this in Supabase SQL Editor
-- Adds all columns required by current UI features
-- Safe to run multiple times (uses IF NOT EXISTS / DO blocks)
-- =============================================


-- =============================================
-- 1. TRIPS - Add destination column
--    (UI: Dashboard hero card shows trip destination)
-- =============================================
ALTER TABLE trips
  ADD COLUMN IF NOT EXISTS destination TEXT;


-- =============================================
-- 2. EXPENSES - Add split_type column
--    (UI: ExpenseEditorPage supports 6 split modes)
-- =============================================
ALTER TABLE expenses
  ADD COLUMN IF NOT EXISTS split_type TEXT DEFAULT 'equal';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'expenses_split_type_check'
      AND conrelid = 'expenses'::regclass
  ) THEN
    ALTER TABLE expenses
      ADD CONSTRAINT expenses_split_type_check
      CHECK (split_type IN ('equal', 'custom', 'itemized', 'gifted', 'individual_shared', 'equalized_meals'));
  END IF;
END $$;


-- =============================================
-- 3. EXPENSE_ITEMS - Add consumers + is_libre
--    (UI: Itemized split tracks who consumed each item;
--         libre items are paid by one person, not split)
-- =============================================
ALTER TABLE expense_items
  ADD COLUMN IF NOT EXISTS consumers    JSONB   DEFAULT '[]'::jsonb,  -- array of member IDs
  ADD COLUMN IF NOT EXISTS is_libre     BOOLEAN DEFAULT FALSE;


-- =============================================
-- 4. ITINERARY_EVENTS - Major schema alignment
--    The ItineraryTimeline component stores items using
--    different column names than the initial schema.
--    Add all missing columns and make event_date nullable
--    since the component uses 'date' instead.
-- =============================================

-- Phase (before / on / after trip)
ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS phase TEXT DEFAULT 'on';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'itinerary_events_phase_check'
      AND conrelid = 'itinerary_events'::regclass
  ) THEN
    ALTER TABLE itinerary_events
      ADD CONSTRAINT itinerary_events_phase_check
      CHECK (phase IN ('before', 'on', 'after'));
  END IF;
END $$;

-- Item type (text note / checklist / expense)
ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS type TEXT DEFAULT 'text';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'itinerary_events_type_check'
      AND conrelid = 'itinerary_events'::regclass
  ) THEN
    ALTER TABLE itinerary_events
      ADD CONSTRAINT itinerary_events_type_check
      CHECK (type IN ('text', 'checklist', 'expense'));
  END IF;
END $$;

-- date / time (component uses 'date'/'time', not 'event_date'/'event_time')
ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS date DATE,
  ADD COLUMN IF NOT EXISTS time TIME;

-- Make event_date nullable (was NOT NULL; component doesn't populate it)
ALTER TABLE itinerary_events
  ALTER COLUMN event_date DROP NOT NULL;

-- notes (component uses 'notes'; DB had 'description' — keep both)
ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS notes TEXT;

-- Checklist items (JSONB array of {text, checked} objects)
ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS checklist JSONB;

-- Expense-type item fields
ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS amount          DECIMAL(10,2),
  ADD COLUMN IF NOT EXISTS paid_by_id      UUID REFERENCES members(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS category        TEXT,
  ADD COLUMN IF NOT EXISTS split_type      TEXT,
  ADD COLUMN IF NOT EXISTS involved_members JSONB,   -- array of member IDs
  ADD COLUMN IF NOT EXISTS custom_splits   JSONB,   -- {memberId: amount}
  ADD COLUMN IF NOT EXISTS expense_items   JSONB,   -- array of line items
  ADD COLUMN IF NOT EXISTS receipt_url     TEXT,
  ADD COLUMN IF NOT EXISTS attachments     JSONB;   -- array of file URLs

-- Add updated_at if missing
ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Index: query by phase within a trip
CREATE INDEX IF NOT EXISTS idx_itinerary_events_phase
  ON itinerary_events(trip_id, phase);


-- =============================================
-- 5. PACKING_ITEMS - Add assignee member support
--    (UI: PackingListPage member picker dropdown)
-- =============================================
ALTER TABLE packing_items
  ADD COLUMN IF NOT EXISTS assigned_to_member_id UUID REFERENCES members(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_packing_items_assignee
  ON packing_items(assigned_to_member_id);


-- =============================================
-- 6. SETTLEMENTS - Ensure notes column exists
--    (Already in some schemas, safe to re-run)
-- =============================================
ALTER TABLE settlements
  ADD COLUMN IF NOT EXISTS notes TEXT;


-- =============================================
-- 7. UPDATED_AT TRIGGERS
--    Ensure all mutable tables auto-update updated_at
-- =============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- itinerary_events
DROP TRIGGER IF EXISTS update_itinerary_events_updated_at ON itinerary_events;
CREATE TRIGGER update_itinerary_events_updated_at
  BEFORE UPDATE ON itinerary_events
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- packing_items
DROP TRIGGER IF EXISTS update_packing_items_updated_at ON packing_items;
CREATE TRIGGER update_packing_items_updated_at
  BEFORE UPDATE ON packing_items
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- settlements
DROP TRIGGER IF EXISTS update_settlements_updated_at ON settlements;
CREATE TRIGGER update_settlements_updated_at
  BEFORE UPDATE ON settlements
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- expense_drafts
DROP TRIGGER IF EXISTS update_expense_drafts_updated_at ON expense_drafts;
CREATE TRIGGER update_expense_drafts_updated_at
  BEFORE UPDATE ON expense_drafts
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- =============================================
-- 8. ROW LEVEL SECURITY
--    Enable RLS + create policies for all feature tables
-- =============================================

ALTER TABLE itinerary_events  ENABLE ROW LEVEL SECURITY;
ALTER TABLE settlements        ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense_items      ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences   ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents          ENABLE ROW LEVEL SECURITY;
ALTER TABLE packing_items      ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_log       ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense_drafts     ENABLE ROW LEVEL SECURITY;
ALTER TABLE currencies         ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE trip_tags          ENABLE ROW LEVEL SECURITY;
ALTER TABLE itinerary_templates ENABLE ROW LEVEL SECURITY;

-- ---- itinerary_events ----
DROP POLICY IF EXISTS "Trip members can view itinerary"    ON itinerary_events;
DROP POLICY IF EXISTS "Trip members can insert itinerary"  ON itinerary_events;
DROP POLICY IF EXISTS "Trip members can update itinerary"  ON itinerary_events;
DROP POLICY IF EXISTS "Trip members can delete itinerary"  ON itinerary_events;

CREATE POLICY "Trip members can view itinerary"
  ON itinerary_events FOR SELECT
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can insert itinerary"
  ON itinerary_events FOR INSERT
  WITH CHECK (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can update itinerary"
  ON itinerary_events FOR UPDATE
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can delete itinerary"
  ON itinerary_events FOR DELETE
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

-- ---- settlements ----
DROP POLICY IF EXISTS "Trip members can view settlements"   ON settlements;
DROP POLICY IF EXISTS "Trip members can insert settlements" ON settlements;
DROP POLICY IF EXISTS "Trip members can update settlements" ON settlements;
DROP POLICY IF EXISTS "Trip members can delete settlements" ON settlements;

CREATE POLICY "Trip members can view settlements"
  ON settlements FOR SELECT
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can insert settlements"
  ON settlements FOR INSERT
  WITH CHECK (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can update settlements"
  ON settlements FOR UPDATE
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can delete settlements"
  ON settlements FOR DELETE
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

-- ---- expense_items ----
DROP POLICY IF EXISTS "Trip members can view expense items"   ON expense_items;
DROP POLICY IF EXISTS "Trip members can insert expense items" ON expense_items;
DROP POLICY IF EXISTS "Trip members can update expense items" ON expense_items;
DROP POLICY IF EXISTS "Trip members can delete expense items" ON expense_items;

CREATE POLICY "Trip members can view expense items"
  ON expense_items FOR SELECT
  USING (
    expense_id IN (
      SELECT id FROM expenses
      WHERE trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid())
    )
  );

CREATE POLICY "Trip members can insert expense items"
  ON expense_items FOR INSERT
  WITH CHECK (
    expense_id IN (
      SELECT id FROM expenses
      WHERE trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid())
    )
  );

CREATE POLICY "Trip members can update expense items"
  ON expense_items FOR UPDATE
  USING (
    expense_id IN (
      SELECT id FROM expenses
      WHERE trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid())
    )
  );

CREATE POLICY "Trip members can delete expense items"
  ON expense_items FOR DELETE
  USING (
    expense_id IN (
      SELECT id FROM expenses
      WHERE trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid())
    )
  );

-- ---- user_preferences ----
DROP POLICY IF EXISTS "Users can view own preferences"   ON user_preferences;
DROP POLICY IF EXISTS "Users can insert own preferences" ON user_preferences;
DROP POLICY IF EXISTS "Users can update own preferences" ON user_preferences;

CREATE POLICY "Users can view own preferences"
  ON user_preferences FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "Users can insert own preferences"
  ON user_preferences FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own preferences"
  ON user_preferences FOR UPDATE
  USING (user_id = auth.uid());

-- ---- packing_items ----
DROP POLICY IF EXISTS "Trip members can view packing items"   ON packing_items;
DROP POLICY IF EXISTS "Trip members can insert packing items" ON packing_items;
DROP POLICY IF EXISTS "Trip members can update packing items" ON packing_items;
DROP POLICY IF EXISTS "Trip members can delete packing items" ON packing_items;

CREATE POLICY "Trip members can view packing items"
  ON packing_items FOR SELECT
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can insert packing items"
  ON packing_items FOR INSERT
  WITH CHECK (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can update packing items"
  ON packing_items FOR UPDATE
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can delete packing items"
  ON packing_items FOR DELETE
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

-- ---- activity_log ----
DROP POLICY IF EXISTS "Trip members can view activity log"    ON activity_log;
DROP POLICY IF EXISTS "Trip members can insert activity log"  ON activity_log;

CREATE POLICY "Trip members can view activity log"
  ON activity_log FOR SELECT
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

CREATE POLICY "Trip members can insert activity log"
  ON activity_log FOR INSERT
  WITH CHECK (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

-- ---- expense_drafts ----
DROP POLICY IF EXISTS "Users can manage own drafts" ON expense_drafts;

CREATE POLICY "Users can manage own drafts"
  ON expense_drafts FOR ALL
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

-- ---- documents ----
DROP POLICY IF EXISTS "Users can view own documents"   ON documents;
DROP POLICY IF EXISTS "Users can insert own documents" ON documents;
DROP POLICY IF EXISTS "Users can update own documents" ON documents;
DROP POLICY IF EXISTS "Users can delete own documents" ON documents;

CREATE POLICY "Users can view own documents"
  ON documents FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "Users can insert own documents"
  ON documents FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own documents"
  ON documents FOR UPDATE
  USING (user_id = auth.uid());

CREATE POLICY "Users can delete own documents"
  ON documents FOR DELETE
  USING (user_id = auth.uid());

-- ---- currencies (read-only for all authenticated users) ----
DROP POLICY IF EXISTS "Authenticated users can view currencies" ON currencies;

CREATE POLICY "Authenticated users can view currencies"
  ON currencies FOR SELECT
  USING (auth.uid() IS NOT NULL);

-- ---- expense_categories (read-only for all authenticated users) ----
DROP POLICY IF EXISTS "Authenticated users can view categories" ON expense_categories;

CREATE POLICY "Authenticated users can view categories"
  ON expense_categories FOR SELECT
  USING (auth.uid() IS NOT NULL);

-- ---- trip_tags ----
DROP POLICY IF EXISTS "Trip members can manage trip tags" ON trip_tags;

CREATE POLICY "Trip members can manage trip tags"
  ON trip_tags FOR ALL
  USING (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()))
  WITH CHECK (trip_id IN (SELECT trip_id FROM members WHERE user_id = auth.uid()));

-- ---- itinerary_templates (owned by user) ----
DROP POLICY IF EXISTS "Users can manage own templates" ON itinerary_templates;

CREATE POLICY "Users can manage own templates"
  ON itinerary_templates FOR ALL
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());


-- =============================================
-- 9. STORAGE BUCKETS
--    Add profile-photos bucket (used by SettingsPage avatar upload)
-- =============================================
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'profile-photos',
  'profile-photos',
  true,
  5242880,  -- 5 MB
  ARRAY['image/jpeg', 'image/png', 'image/gif', 'image/webp']
)
ON CONFLICT (id) DO NOTHING;

-- Storage RLS for profile-photos
DROP POLICY IF EXISTS "Users can upload own profile photo"  ON storage.objects;
DROP POLICY IF EXISTS "Anyone can view profile photos"      ON storage.objects;
DROP POLICY IF EXISTS "Users can update own profile photo"  ON storage.objects;
DROP POLICY IF EXISTS "Users can delete own profile photo"  ON storage.objects;

CREATE POLICY "Users can upload own profile photo"
  ON storage.objects FOR INSERT
  WITH CHECK (
    bucket_id = 'profile-photos'
    AND auth.uid()::text = (storage.foldername(name))[1]
  );

CREATE POLICY "Anyone can view profile photos"
  ON storage.objects FOR SELECT
  USING (bucket_id = 'profile-photos');

CREATE POLICY "Users can update own profile photo"
  ON storage.objects FOR UPDATE
  USING (
    bucket_id = 'profile-photos'
    AND auth.uid()::text = (storage.foldername(name))[1]
  );

CREATE POLICY "Users can delete own profile photo"
  ON storage.objects FOR DELETE
  USING (
    bucket_id = 'profile-photos'
    AND auth.uid()::text = (storage.foldername(name))[1]
  );


-- =============================================
-- SUMMARY OF CHANGES
-- =============================================
-- trips            : +destination
-- expenses         : +split_type (+constraint)
-- expense_items    : +consumers, +is_libre
-- itinerary_events : +phase, +type, +date, +time, +notes, +checklist,
--                    +amount, +paid_by_id, +category, +split_type,
--                    +involved_members, +custom_splits, +expense_items,
--                    +receipt_url, +attachments, +updated_at
--                    event_date is now nullable
-- packing_items    : +assigned_to_member_id
-- settlements      : +notes (if missing)
-- triggers         : updated_at for itinerary_events, packing_items,
--                    settlements, expense_drafts
-- RLS              : full policies for all 12 feature tables
-- storage          : profile-photos bucket + policies
