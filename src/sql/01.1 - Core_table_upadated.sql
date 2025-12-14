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

-- 7. Create expense_drafts table for saving work-in-progress
CREATE TABLE expense_drafts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  trip_id UUID REFERENCES trips(id) ON DELETE CASCADE,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  draft_key TEXT NOT NULL, -- unique identifier for the draft
  expense_data JSONB NOT NULL, -- all expense form data
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(trip_id, user_id, draft_key)
);

-- Index for faster queries
CREATE INDEX idx_expense_drafts_trip_user ON expense_drafts(trip_id, user_id);
CREATE INDEX idx_expense_drafts_updated ON expense_drafts(updated_at);

-- 8. Create currencies table for dynamic currency management
CREATE TABLE currencies (
  code VARCHAR(3) PRIMARY KEY,
  name TEXT NOT NULL,
  symbol TEXT,
  is_active BOOLEAN DEFAULT TRUE,
  display_order INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Insert default currencies
INSERT INTO currencies (code, name, symbol, display_order) VALUES
  ('PHP', 'Philippine Peso', '₱', 1),
  ('USD', 'US Dollar', '$', 2),
  ('EUR', 'Euro', '€', 3),
  ('JPY', 'Japanese Yen', '¥', 4),
  ('SGD', 'Singapore Dollar', 'S$', 5),
  ('GBP', 'British Pound', '£', 6),
  ('AUD', 'Australian Dollar', 'A$', 7),
  ('CAD', 'Canadian Dollar', 'C$', 8),
  ('CHF', 'Swiss Franc', 'CHF', 9),
  ('CNY', 'Chinese Yuan', '¥', 10);

-- 9. Create expense_categories table for dynamic category management
CREATE TABLE expense_categories (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL UNIQUE,
  icon TEXT,
  color TEXT,
  is_active BOOLEAN DEFAULT TRUE,
  display_order INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Insert default categories
INSERT INTO expense_categories (name, icon, color, display_order) VALUES
  ('Food & Drinks', 'restaurant', 'orange', 1),
  ('Accommodation', 'hotel', 'blue', 2),
  ('Transportation', 'commute', 'green', 3),
  ('Activities', 'attractions', 'purple', 4),
  ('Groceries', 'local_grocery_store', 'brown', 5),
  ('Shopping', 'shopping_cart', 'pink', 6),
  ('Entertainment', 'movie', 'red', 7),
  ('Health & Medical', 'local_hospital', 'teal', 8),
  ('Other', 'more_horiz', 'grey', 9);

-- 10. Create documents table for document vault
CREATE TABLE documents (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  trip_id UUID REFERENCES trips(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  file_path TEXT NOT NULL, -- Supabase storage path
  file_size INTEGER,
  mime_type TEXT,
  category TEXT DEFAULT 'general', -- 'passport', 'visa', 'insurance', 'tickets', 'receipts', 'general'
  uploaded_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(user_id, trip_id, name)
);

-- Index for faster queries
CREATE INDEX idx_documents_user_trip ON documents(user_id, trip_id);
CREATE INDEX idx_documents_category ON documents(category);

-- 11. Create packing_items table for packing lists
CREATE TABLE packing_items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  trip_id UUID REFERENCES trips(id) ON DELETE CASCADE,
  item_name TEXT NOT NULL,
  category TEXT DEFAULT 'other', -- 'clothes', 'toiletries', 'electronics', 'documents', 'other'
  is_packed BOOLEAN DEFAULT FALSE,
  quantity INTEGER DEFAULT 1,
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Index for faster queries
CREATE INDEX idx_packing_items_trip ON packing_items(trip_id);
CREATE INDEX idx_packing_items_category ON packing_items(category);

-- 12. Create itinerary_templates table for reusable itinerary templates
CREATE TABLE itinerary_templates (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  items JSONB NOT NULL, -- Array of template items
  items_count INTEGER GENERATED ALWAYS AS (jsonb_array_length(items)) STORED,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Index for faster queries
CREATE INDEX idx_itinerary_templates_user ON itinerary_templates(user_id);