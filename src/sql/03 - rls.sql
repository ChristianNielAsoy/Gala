-- =============================================
-- COMPLETE RLS FIX - Run this entire script
-- =============================================

-- Step 1: Disable RLS temporarily to clean up
ALTER TABLE public.trips DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.members DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.expense_splits DISABLE ROW LEVEL SECURITY;

-- Step 2: Drop ALL existing policies
DO $$ 
DECLARE
    r RECORD;
BEGIN
    -- Drop all policies on trips
    FOR r IN (SELECT policyname FROM pg_policies WHERE tablename = 'trips' AND schemaname = 'public') LOOP
        EXECUTE 'DROP POLICY IF EXISTS "' || r.policyname || '" ON public.trips';
    END LOOP;
    
    -- Drop all policies on members
    FOR r IN (SELECT policyname FROM pg_policies WHERE tablename = 'members' AND schemaname = 'public') LOOP
        EXECUTE 'DROP POLICY IF EXISTS "' || r.policyname || '" ON public.members';
    END LOOP;
    
    -- Drop all policies on expenses
    FOR r IN (SELECT policyname FROM pg_policies WHERE tablename = 'expenses' AND schemaname = 'public') LOOP
        EXECUTE 'DROP POLICY IF EXISTS "' || r.policyname || '" ON public.expenses';
    END LOOP;
    
    -- Drop all policies on expense_splits
    FOR r IN (SELECT policyname FROM pg_policies WHERE tablename = 'expense_splits' AND schemaname = 'public') LOOP
        EXECUTE 'DROP POLICY IF EXISTS "' || r.policyname || '" ON public.expense_splits';
    END LOOP;
END $$;

-- Step 3: Re-enable RLS
ALTER TABLE public.trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expense_splits ENABLE ROW LEVEL SECURITY;

-- =============================================
-- TRIPS POLICIES
-- =============================================

-- Allow users to SELECT their own trips
CREATE POLICY "select_own_trips"
ON public.trips
FOR SELECT
TO authenticated
USING (user_id = auth.uid());

-- Allow users to INSERT trips
CREATE POLICY "insert_own_trips"
ON public.trips
FOR INSERT
TO authenticated
WITH CHECK (user_id = auth.uid());

-- Allow users to UPDATE their own trips
CREATE POLICY "update_own_trips"
ON public.trips
FOR UPDATE
TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

-- Allow users to DELETE their own trips
CREATE POLICY "delete_own_trips"
ON public.trips
FOR DELETE
TO authenticated
USING (user_id = auth.uid());

-- =============================================
-- MEMBERS POLICIES
-- =============================================

-- Allow viewing members if you own the trip
CREATE POLICY "select_members_of_owned_trips"
ON public.members
FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = members.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- Allow adding members to owned trips
CREATE POLICY "insert_members_to_owned_trips"
ON public.members
FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = members.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- Allow updating members in owned trips
CREATE POLICY "update_members_in_owned_trips"
ON public.members
FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = members.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- Allow deleting members from owned trips
CREATE POLICY "delete_members_from_owned_trips"
ON public.members
FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = members.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- =============================================
-- EXPENSES POLICIES
-- =============================================

-- Allow viewing expenses in owned trips
CREATE POLICY "select_expenses_in_owned_trips"
ON public.expenses
FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = expenses.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- Allow adding expenses to owned trips
CREATE POLICY "insert_expenses_to_owned_trips"
ON public.expenses
FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = expenses.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- Allow updating expenses in owned trips
CREATE POLICY "update_expenses_in_owned_trips"
ON public.expenses
FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = expenses.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- Allow deleting expenses from owned trips
CREATE POLICY "delete_expenses_from_owned_trips"
ON public.expenses
FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.trips 
        WHERE trips.id = expenses.trip_id 
        AND trips.user_id = auth.uid()
    )
);

-- =============================================
-- EXPENSE SPLITS POLICIES
-- =============================================

-- Allow viewing splits in owned trips
CREATE POLICY "select_splits_in_owned_trips"
ON public.expense_splits
FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE e.id = expense_splits.expense_id 
        AND t.user_id = auth.uid()
    )
);

-- Allow adding splits to expenses in owned trips
CREATE POLICY "insert_splits_to_owned_trips"
ON public.expense_splits
FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE e.id = expense_splits.expense_id 
        AND t.user_id = auth.uid()
    )
);

-- Allow updating splits in owned trips
CREATE POLICY "update_splits_in_owned_trips"
ON public.expense_splits
FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE e.id = expense_splits.expense_id 
        AND t.user_id = auth.uid()
    )
);

-- Allow deleting splits from owned trips
CREATE POLICY "delete_splits_from_owned_trips"
ON public.expense_splits
FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE e.id = expense_splits.expense_id 
        AND t.user_id = auth.uid()
    )
);

-- =============================================
-- GRANT PERMISSIONS TO AUTHENTICATED USERS
-- =============================================

-- Grant table permissions to authenticated users
GRANT SELECT, INSERT, UPDATE, DELETE ON public.trips TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.members TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.expenses TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.expense_splits TO authenticated;

-- Grant usage on sequences (for auto-increment IDs)
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO authenticated;


-- ========================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ========================================

-- Enable RLS on new tables
ALTER TABLE itinerary_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE settlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE trip_tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_log ENABLE ROW LEVEL SECURITY;

-- Policies for itinerary_events
CREATE POLICY "Users can view itinerary events for their trips"
ON itinerary_events FOR SELECT
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

CREATE POLICY "Users can insert itinerary events for their trips"
ON itinerary_events FOR INSERT
WITH CHECK (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

CREATE POLICY "Users can update itinerary events for their trips"
ON itinerary_events FOR UPDATE
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

CREATE POLICY "Users can delete itinerary events for their trips"
ON itinerary_events FOR DELETE
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

-- Policies for settlements
CREATE POLICY "Users can view settlements for their trips"
ON settlements FOR SELECT
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

CREATE POLICY "Users can create settlements for their trips"
ON settlements FOR INSERT
WITH CHECK (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

CREATE POLICY "Users can update settlements for their trips"
ON settlements FOR UPDATE
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

-- Policies for expense_items
CREATE POLICY "Users can view expense items for their trip expenses"
ON expense_items FOR SELECT
USING (
  expense_id IN (
    SELECT e.id FROM expenses e
    INNER JOIN members m ON e.trip_id = m.trip_id
    WHERE m.user_id = auth.uid()
  )
);

CREATE POLICY "Users can insert expense items for their trip expenses"
ON expense_items FOR INSERT
WITH CHECK (
  expense_id IN (
    SELECT e.id FROM expenses e
    INNER JOIN members m ON e.trip_id = m.trip_id
    WHERE m.user_id = auth.uid()
  )
);

CREATE POLICY "Users can update expense items for their trip expenses"
ON expense_items FOR UPDATE
USING (
  expense_id IN (
    SELECT e.id FROM expenses e
    INNER JOIN members m ON e.trip_id = m.trip_id
    WHERE m.user_id = auth.uid()
  )
);

CREATE POLICY "Users can delete expense items for their trip expenses"
ON expense_items FOR DELETE
USING (
  expense_id IN (
    SELECT e.id FROM expenses e
    INNER JOIN members m ON e.trip_id = m.trip_id
    WHERE m.user_id = auth.uid()
  )
);

-- Policies for user_preferences
CREATE POLICY "Users can view their own preferences"
ON user_preferences FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own preferences"
ON user_preferences FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own preferences"
ON user_preferences FOR UPDATE
USING (auth.uid() = user_id);

-- Policies for trip_tags
CREATE POLICY "Users can view tags for their trips"
ON trip_tags FOR SELECT
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

CREATE POLICY "Users can manage tags for their trips"
ON trip_tags FOR ALL
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

-- Policies for activity_log
CREATE POLICY "Users can view activity for their trips"
ON activity_log FOR SELECT
USING (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);

CREATE POLICY "Users can create activity logs for their trips"
ON activity_log FOR INSERT
WITH CHECK (
  trip_id IN (
    SELECT trip_id FROM members WHERE user_id = auth.uid()
  )
);