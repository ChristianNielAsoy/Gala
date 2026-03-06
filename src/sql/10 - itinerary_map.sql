-- =============================================
-- GALA APP - Migration v10
-- Itinerary: map coordinates + planned costs
-- Safe to run multiple times
-- =============================================

ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS latitude      DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS longitude     DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS estimated_cost DECIMAL(10,2);

-- =============================================
-- SUMMARY
-- itinerary_events : added latitude, longitude, estimated_cost
-- =============================================
