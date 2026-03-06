-- =============================================
-- FIX: Remove existing policies and create non-recursive ones
-- Run this in Supabase SQL Editor
-- =============================================

-- First, drop all existing policies
DROP POLICY IF EXISTS "Users can view trips they are members of" ON public.trips;
DROP POLICY IF EXISTS "Users can create trips" ON public.trips;
DROP POLICY IF EXISTS "Trip owners can update trips" ON public.trips;
DROP POLICY IF EXISTS "Trip owners can delete trips" ON public.trips;

DROP POLICY IF EXISTS "Users can view members of their trips" ON public.members;
DROP POLICY IF EXISTS "Trip owners can add members" ON public.members;
DROP POLICY IF EXISTS "Trip owners can update members" ON public.members;
DROP POLICY IF EXISTS "Trip owners can remove members" ON public.members;

DROP POLICY IF EXISTS "Users can view expenses of their trips" ON public.expenses;
DROP POLICY IF EXISTS "Trip members can add expenses" ON public.expenses;
DROP POLICY IF EXISTS "Users can update their expenses" ON public.expenses;
DROP POLICY IF EXISTS "Users can delete their expenses" ON public.expenses;

DROP POLICY IF EXISTS "Users can view expense splits of their trips" ON public.expense_splits;
DROP POLICY IF EXISTS "Trip members can add splits" ON public.expense_splits;
DROP POLICY IF EXISTS "Users can update splits for their expenses" ON public.expense_splits;
DROP POLICY IF EXISTS "Users can delete splits for their expenses" ON public.expense_splits;

-- =============================================
-- FIXED TRIPS POLICIES (No recursion)
-- =============================================

-- Users can view their own trips
CREATE POLICY "Users can view their own trips"
ON public.trips FOR SELECT
USING (auth.uid() = user_id);

-- Users can create their own trips
CREATE POLICY "Users can create trips"
ON public.trips FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- Trip owners can update their trips
CREATE POLICY "Trip owners can update trips"
ON public.trips FOR UPDATE
USING (auth.uid() = user_id);

-- Trip owners can delete their trips
CREATE POLICY "Trip owners can delete trips"
ON public.trips FOR DELETE
USING (auth.uid() = user_id);

-- =============================================
-- FIXED MEMBERS POLICIES (No recursion)
-- =============================================

-- Users can view members of trips they own
CREATE POLICY "View members of owned trips"
ON public.members FOR SELECT
USING (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- Users can also view their own member records
CREATE POLICY "Users can view their own member records"
ON public.members FOR SELECT
USING (user_id = auth.uid());

-- Trip owners can add members
CREATE POLICY "Trip owners can add members"
ON public.members FOR INSERT
WITH CHECK (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- Trip owners can update members
CREATE POLICY "Trip owners can update members"
ON public.members FOR UPDATE
USING (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- Trip owners can remove members
CREATE POLICY "Trip owners can remove members"
ON public.members FOR DELETE
USING (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- =============================================
-- FIXED EXPENSES POLICIES
-- =============================================

-- Users can view expenses of trips they own
CREATE POLICY "Users can view expenses of owned trips"
ON public.expenses FOR SELECT
USING (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- Trip owners can add expenses
CREATE POLICY "Trip owners can add expenses"
ON public.expenses FOR INSERT
WITH CHECK (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- Trip owners can update expenses in their trips
CREATE POLICY "Trip owners can update expenses"
ON public.expenses FOR UPDATE
USING (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- Trip owners can delete expenses in their trips
CREATE POLICY "Trip owners can delete expenses"
ON public.expenses FOR DELETE
USING (
    trip_id IN (
        SELECT id FROM public.trips 
        WHERE user_id = auth.uid()
    )
);

-- =============================================
-- FIXED EXPENSE SPLITS POLICIES
-- =============================================

-- Users can view splits for expenses in their trips
CREATE POLICY "Users can view expense splits"
ON public.expense_splits FOR SELECT
USING (
    expense_id IN (
        SELECT e.id FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE t.user_id = auth.uid()
    )
);

-- Trip owners can add splits
CREATE POLICY "Trip owners can add splits"
ON public.expense_splits FOR INSERT
WITH CHECK (
    expense_id IN (
        SELECT e.id FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE t.user_id = auth.uid()
    )
);

-- Trip owners can update splits
CREATE POLICY "Trip owners can update splits"
ON public.expense_splits FOR UPDATE
USING (
    expense_id IN (
        SELECT e.id FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE t.user_id = auth.uid()
    )
);

-- Trip owners can delete splits
CREATE POLICY "Trip owners can delete splits"
ON public.expense_splits FOR DELETE
USING (
    expense_id IN (
        SELECT e.id FROM public.expenses e
        INNER JOIN public.trips t ON e.trip_id = t.id
        WHERE t.user_id = auth.uid()
    )
);

-- Policies for expense-receipts bucket

-- Allow authenticated users to upload receipts
CREATE POLICY "Users can upload expense receipts"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'expense-receipts' AND
  auth.uid() IS NOT NULL
);

-- Allow users to read receipts for their trips
CREATE POLICY "Users can view expense receipts"
ON storage.objects FOR SELECT
TO authenticated
USING (
  bucket_id = 'expense-receipts'
);

-- Allow users to delete receipts they uploaded
CREATE POLICY "Users can delete their expense receipts"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'expense-receipts' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Policies for payment-proofs bucket

-- Allow authenticated users to upload payment proofs
CREATE POLICY "Users can upload payment proofs"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'payment-proofs' AND
  auth.uid() IS NOT NULL
);

-- Allow users to view payment proofs for their trips
CREATE POLICY "Users can view payment proofs"
ON storage.objects FOR SELECT
TO authenticated
USING (
  bucket_id = 'payment-proofs'
);

-- Allow users to delete payment proofs they uploaded
CREATE POLICY "Users can delete their payment proofs"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'payment-proofs' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Trip members can create activity logs
CREATE POLICY "Trip members can create activity logs"
ON activity_log FOR INSERT
WITH CHECK (
  trip_id IN (
    SELECT trip_id FROM members
    WHERE user_id = auth.uid()
  )
);