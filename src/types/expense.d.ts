// src/types/expense.d.ts - FIXED VERSION

// ============================================
// TRIP TYPES
// ============================================

export type TripStatus = 'planning' | 'active' | 'completed' | 'archived';

export interface Trip {
  id: string;
  user_id: string;
  name: string;
  start_date: string;
  end_date: string;
  currency_code: string;
  currencyCode?: string;
  status?: TripStatus;
  location?: string;
  destination?: string;
  description?: string;
  cover_image_url?: string;
  budget_amount?: number;
  member_count?: number;
  total_expenses?: number;
  created_at: string;
  updated_at?: string;
}

export interface TripTag {
  id: string;
  trip_id: string;
  tag_name: string;
  created_at: string;
}

// ============================================
// MEMBER TYPES
// ============================================

export interface TripMember {
  id: string;
  trip_id: string;
  user_id: string;
  name: string;
  is_owner: boolean;
  avatar_url?: string;
  created_at: string;
}

// ============================================
// EXPENSE TYPES
// ============================================

export type SplitType =
  | 'equal'
  | 'custom'
  | 'itemized'
  | 'gifted'
  | 'individual_shared'
  | 'equalized_meals';

export interface Expense {
  id: string;
  trip_id: string;
  paid_by_id: string;
  paid_by_name?: string;
  amount: number;
  currency_code: string;
  description: string;
  date: string;
  category: string;
  receipt_url?: string;
  has_receipt: boolean;
  created_at: string;
  updated_at?: string;
}

export interface ExpenseSplit {
  id?: string;
  expense_id: string;
  member_id: string;
  share_amount: number;
  share_percentage?: number;
  share_ratio?: number;
}

export interface ExpenseItem {
  id?: string;
  expense_id: string;
  item_name: string;
  item_amount: number;
  quantity: number;
  display_order: number;
  consumers: string[]; // Array of member IDs who consumed this item
  is_libre?: boolean; // Whether this item is free (no splitting needed)
}

// ============================================
// SETTLEMENT TYPES
// ============================================

export type SettlementStatus = 'pending' | 'paid' | 'verified' | 'cancelled';
export type PaymentMethod = 'cash' | 'gcash' | 'card' | 'bank_transfer';

export interface Settlement {
  id: string;
  trip_id: string;
  from_member_id: string;
  to_member_id: string;
  amount: number;
  currency_code: string;
  status: SettlementStatus;
  payment_method?: PaymentMethod;
  payment_proof_url?: string;
  notes?: string;
  paid_at?: string;
  verified_at?: string;
  created_at: string;
  updated_at?: string;
}

// Settlement suggestion (calculated, not from DB)
export interface SettlementSuggestion {
  from_member_id: string;
  to_member_id: string;
  amount: number;
  currency_code: string;
}

// Helper type for displaying settlements with member names
export interface SettlementWithMembers extends Settlement {
  from_member_name: string;
  to_member_name: string;
  from_member_avatar?: string;
  to_member_avatar?: string;
}

// ============================================
// ITINERARY TYPES
// ============================================

export interface ItineraryEvent {
  id?: string;
  trip_id: string;
  event_date: string;
  event_time?: string;
  title: string;
  location?: string;
  description?: string;
  icon: string;
  display_order: number;
  created_at?: string;
  updated_at?: string;
}

// ============================================
// USER PREFERENCES TYPES
// ============================================

export interface UserPreferences {
  user_id: string;
  dark_mode: boolean;
  default_currency: string;
  notifications_enabled: boolean;
  expense_updates: boolean;
  trip_reminders: boolean;
  settlement_alerts: boolean;
  language: string;
  created_at?: string;
  updated_at?: string;
}

// ============================================
// ACTIVITY LOG TYPES
// ============================================

export type ActivityActionType =
  | 'expense_added'
  | 'expense_updated'
  | 'expense_deleted'
  | 'member_joined'
  | 'member_left'
  | 'settlement_created'
  | 'settlement_paid'
  | 'settlement_verified'
  | 'itinerary_event_added'
  | 'itinerary_event_updated'
  | 'trip_created'
  | 'trip_updated'
  | 'trip_completed';

export type ActivityEntityType = 'expense' | 'member' | 'settlement' | 'itinerary' | 'trip';

export interface ActivityLog {
  id: string;
  trip_id: string;
  user_id?: string;
  action_type: ActivityActionType;
  entity_type?: ActivityEntityType;
  entity_id?: string;
  description: string;
  metadata?: Record<string, unknown>;
  created_at: string;
}

// ============================================
// UTILITY TYPES & CONSTANTS
// ============================================

export const EXPENSE_CATEGORIES = [
  'Food & Drinks',
  'Accommodation',
  'Transportation',
  'Activities',
  'Groceries',
  'Other',
] as const;

export type ExpenseCategory = (typeof EXPENSE_CATEGORIES)[number];

export const CURRENCY_CODES = [
  'PHP',
  'USD',
  'EUR',
  'JPY',
  'GBP',
  'AUD',
  'CAD',
  'SGD',
  'THB',
] as const;

export type CurrencyCode = (typeof CURRENCY_CODES)[number];

// ============================================
// CALCULATED/DERIVED TYPES
// ============================================

// For displaying expense with enriched data
export interface ExpenseWithDetails extends Expense {
  paid_by_name: string;
  paid_by_avatar?: string;
  splits?: ExpenseSplit[];
  items?: ExpenseItem[];
}

// For trip summary cards
export interface TripSummary {
  trip: Trip;
  total_expenses: number;
  member_count: number;
  pending_settlements: number;
  tags: string[];
}

// For member balance calculations
export interface MemberBalance {
  member_id: string;
  member_name: string;
  member_avatar?: string;
  total_paid: number;
  total_owed: number;
  netBalance: number; // positive = owed money, negative = owes money
}
