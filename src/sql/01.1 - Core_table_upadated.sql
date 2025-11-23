-- 1. Add to trips table
ALTER TABLE trips ADD COLUMN cover_image_url TEXT;
ALTER TABLE trips ADD COLUMN status TEXT DEFAULT 'active'; -- 'planning', 'active', 'completed'
ALTER TABLE trips ADD COLUMN budget_amount DECIMAL(10,2);

-- 2. Create itinerary_events table
CREATE TABLE itinerary_events (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  trip_id UUID REFERENCES trips(id) ON DELETE CASCADE,
  event_date DATE NOT NULL,
  event_time TIME,
  title TEXT NOT NULL,
  location TEXT,
  description TEXT,
  icon TEXT,
  display_order INTEGER,
  created_at TIMESTAMP DEFAULT NOW()
);

-- 3. Create settlements table
CREATE TABLE settlements (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  trip_id UUID REFERENCES trips(id) ON DELETE CASCADE,
  from_member_id UUID REFERENCES members(id),
  to_member_id UUID REFERENCES members(id),
  amount DECIMAL(10,2) NOT NULL,
  currency_code VARCHAR(3),
  status TEXT DEFAULT 'pending', -- 'pending', 'paid', 'verified'
  payment_method TEXT, -- 'cash', 'gcash', 'card'
  payment_proof_url TEXT,
  paid_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- 4. Create expense_items table (for itemized expenses)
CREATE TABLE expense_items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  expense_id UUID REFERENCES expenses(id) ON DELETE CASCADE,
  item_name TEXT NOT NULL,
  item_amount DECIMAL(10,2) NOT NULL,
  quantity INTEGER DEFAULT 1,
  display_order INTEGER
);

-- 5. Create user_preferences table
CREATE TABLE user_preferences (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id),
  dark_mode BOOLEAN DEFAULT FALSE,
  default_currency VARCHAR(3) DEFAULT 'PHP',
  notifications_enabled BOOLEAN DEFAULT TRUE,
  expense_updates BOOLEAN DEFAULT TRUE,
  trip_reminders BOOLEAN DEFAULT TRUE,
  settlement_alerts BOOLEAN DEFAULT TRUE,
  updated_at TIMESTAMP DEFAULT NOW()
);

-- 6. Add receipt fields to expenses
ALTER TABLE expenses ADD COLUMN has_receipt BOOLEAN DEFAULT FALSE;