-- =============================================
-- GALA APP - Migration v9
-- Personal budget per member per trip
-- Safe to run multiple times
-- =============================================

ALTER TABLE members ADD COLUMN IF NOT EXISTS personal_budget NUMERIC(12,2);

-- =============================================
-- SUMMARY
-- members : added personal_budget column
-- =============================================
