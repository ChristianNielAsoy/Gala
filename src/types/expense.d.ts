// expense.d.ts - Type definitions for expense-related entities

export interface Trip {
  id: string;          // Unique ID of the trip
  user_id: string;     // ID of the user who created it (owner)
  name: string;        // Name of the trip (e.g., "Palawan Adventure 2025")
  start_date: string;  // Trip start date (ISO string, e.g., 'YYYY-MM-DD')
  end_date: string;    // Trip end date (ISO string)
  currency_code: string; // Base currency for the trip (e.g., 'PHP', 'USD')
  created_at?: string;  // Timestamp
}

export interface TripMember {
  id: string;           // Unique ID for the member within this trip
  trip_id: string;
  user_id: string;      // The Supabase user ID if they are a registered user
  name: string;
  is_owner: boolean;
  avatar_url?: string | null;
}

export interface Expense {
  id: string;               // Unique ID of the expense
  trip_id: string;          // Foreign key to the trip
  paid_by_id: string;       // Foreign key to the TripMember who paid
  amount: number;           // Total amount of the expense
  currency_code: string;    // E.g., 'PHP', 'USD'
  description: string;      // Short description of the expense
  date: string;             // Date of the expense (ISO 8601 string)
  category: string;         // E.g., 'Food', 'Transport', 'Lodging'
  created_at?: string;
}

export type SplitType = 'equal' | 'custom' | 'percentages';

export interface ExpenseSplit {
  expense_id: string;
  member_id: string;        // ID of the member who owes/is responsible for a share
  share_amount: number;     // The amount this specific member owes
  share_percentage?: number; // Optional: If using percentage split
  share_ratio?: number;      // Optional: If using ratio/weight split
}

export const EXPENSE_CATEGORIES = [
  'Food',
  'Lodging',
  'Transport',
  'Activity',
  'Groceries',
  'Miscellaneous',
] as const;

export type ExpenseCategory = typeof EXPENSE_CATEGORIES[number];
