-- =============================================
-- GALA APP - COMPLETE DATABASE SCHEMA
-- Run this in Supabase SQL Editor
-- =============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- 1. TRIPS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS public.trips (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    currency_code TEXT NOT NULL DEFAULT 'PHP',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT valid_dates CHECK (end_date >= start_date)
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_trips_user_id ON public.trips(user_id);
CREATE INDEX IF NOT EXISTS idx_trips_dates ON public.trips(start_date, end_date);

-- =============================================
-- 2. MEMBERS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS public.members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    is_owner BOOLEAN DEFAULT FALSE,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT unique_trip_user UNIQUE(trip_id, user_id)
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_members_trip_id ON public.members(trip_id);
CREATE INDEX IF NOT EXISTS idx_members_user_id ON public.members(user_id);

-- =============================================
-- 3. EXPENSES TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS public.expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    paid_by_id UUID NOT NULL REFERENCES public.members(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency_code TEXT NOT NULL DEFAULT 'PHP',
    category TEXT NOT NULL DEFAULT 'Other',
    date DATE NOT NULL,
    receipt_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_expenses_trip_id ON public.expenses(trip_id);
CREATE INDEX IF NOT EXISTS idx_expenses_date ON public.expenses(date);

-- =============================================
-- 4. EXPENSE SPLITS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS public.expense_splits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    expense_id UUID NOT NULL REFERENCES public.expenses(id) ON DELETE CASCADE,
    member_id UUID NOT NULL REFERENCES public.members(id) ON DELETE CASCADE,
    share_amount NUMERIC(12, 2) NOT NULL CHECK (share_amount >= 0),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT unique_expense_member UNIQUE(expense_id, member_id)
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_expense_splits_expense_id ON public.expense_splits(expense_id);
CREATE INDEX IF NOT EXISTS idx_expense_splits_member_id ON public.expense_splits(member_id);

-- =============================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =============================================

-- Enable RLS on all tables
ALTER TABLE public.trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expense_splits ENABLE ROW LEVEL SECURITY;

-- =============================================
-- TRIPS POLICIES
-- =============================================

-- Users can view trips they are members of
CREATE POLICY "Users can view trips they are members of"
ON public.trips FOR SELECT
USING (
    id IN (
        SELECT trip_id FROM public.members 
        WHERE user_id = auth.uid()
    )
);

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
-- MEMBERS POLICIES
-- =============================================

-- Users can view members of trips they belong to
CREATE POLICY "Users can view members of their trips"
ON public.members FOR SELECT
USING (
    trip_id IN (
        SELECT trip_id FROM public.members 
        WHERE user_id = auth.uid()
    )
);

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
-- EXPENSES POLICIES
-- =============================================

-- Users can view expenses of trips they belong to
CREATE POLICY "Users can view expenses of their trips"
ON public.expenses FOR SELECT
USING (
    trip_id IN (
        SELECT trip_id FROM public.members 
        WHERE user_id = auth.uid()
    )
);

-- Trip members can add expenses
CREATE POLICY "Trip members can add expenses"
ON public.expenses FOR INSERT
WITH CHECK (
    trip_id IN (
        SELECT trip_id FROM public.members 
        WHERE user_id = auth.uid()
    )
);

-- Users can update expenses they created
CREATE POLICY "Users can update their expenses"
ON public.expenses FOR UPDATE
USING (
    paid_by_id IN (
        SELECT id FROM public.members 
        WHERE user_id = auth.uid()
    )
);

-- Users can delete expenses they created
CREATE POLICY "Users can delete their expenses"
ON public.expenses FOR DELETE
USING (
    paid_by_id IN (
        SELECT id FROM public.members 
        WHERE user_id = auth.uid()
    )
);

-- =============================================
-- EXPENSE SPLITS POLICIES
-- =============================================

-- Users can view splits for expenses in their trips
CREATE POLICY "Users can view expense splits of their trips"
ON public.expense_splits FOR SELECT
USING (
    expense_id IN (
        SELECT id FROM public.expenses 
        WHERE trip_id IN (
            SELECT trip_id FROM public.members 
            WHERE user_id = auth.uid()
        )
    )
);

-- Trip members can add splits
CREATE POLICY "Trip members can add splits"
ON public.expense_splits FOR INSERT
WITH CHECK (
    expense_id IN (
        SELECT id FROM public.expenses 
        WHERE trip_id IN (
            SELECT trip_id FROM public.members 
            WHERE user_id = auth.uid()
        )
    )
);

-- Users can update splits for their expenses
CREATE POLICY "Users can update splits for their expenses"
ON public.expense_splits FOR UPDATE
USING (
    expense_id IN (
        SELECT e.id FROM public.expenses e
        INNER JOIN public.members m ON e.paid_by_id = m.id
        WHERE m.user_id = auth.uid()
    )
);

-- Users can delete splits for their expenses
CREATE POLICY "Users can delete splits for their expenses"
ON public.expense_splits FOR DELETE
USING (
    expense_id IN (
        SELECT e.id FROM public.expenses e
        INNER JOIN public.members m ON e.paid_by_id = m.id
        WHERE m.user_id = auth.uid()
    )
);

-- =============================================
-- HELPER FUNCTIONS
-- =============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers to auto-update updated_at
CREATE TRIGGER update_trips_updated_at 
    BEFORE UPDATE ON public.trips 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_expenses_updated_at 
    BEFORE UPDATE ON public.expenses 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- COMPLETED!
-- =============================================
-- Now your database is ready for the Gala app
-- All tables have proper RLS policies for security