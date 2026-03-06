-- =============================================
-- GALA APP - Migration v7
-- Fixes itinerary_events expense item persistence
-- Safe to run multiple times
-- =============================================

-- Add missing split-data columns for expense-type itinerary items.
-- These are set by ItineraryTimeline when saving an expense item but were
-- never added to the table, causing split data to be lost on page refresh.

ALTER TABLE itinerary_events
  ADD COLUMN IF NOT EXISTS individual_items JSONB,   -- items paid by each member individually
  ADD COLUMN IF NOT EXISTS shared_items     JSONB,   -- items shared equally
  ADD COLUMN IF NOT EXISTS gifted_item_gifter TEXT;  -- member_id who gifted the item

-- Index to help queries that filter expense-type items needing gifter info
CREATE INDEX IF NOT EXISTS idx_itinerary_events_type_phase
  ON itinerary_events(trip_id, type, phase);

-- =============================================
-- SUMMARY
-- itinerary_events : +individual_items, +shared_items, +gifted_item_gifter
-- =============================================
