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

-- =============================================
-- VERIFICATION QUERIES
-- =============================================
-- After running, test these queries:

-- 1. Check if policies exist
SELECT schemaname, tablename, policyname 
FROM pg_policies 
WHERE schemaname = 'public' 
ORDER BY tablename, policyname;

-- 2. Check RLS status
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename IN ('trips', 'members', 'expenses', 'expense_splits');

-- =============================================
-- DONE!
-- =============================================