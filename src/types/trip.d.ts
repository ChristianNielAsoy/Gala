export interface Trip {
  id: string;          // Unique ID of the trip
  user_id: string;     // ID of the user who created it (owner)
  name: string;        // Name of the trip (e.g., "Palawan Adventure 2025")
  start_date: string;  // Trip start date (ISO string, e.g., 'YYYY-MM-DD')
  end_date: string;    // Trip end date (ISO string)
  currency_code: string; // Base currency for the trip (e.g., 'PHP', 'USD')
  created_at: string;  // Timestamp
}

// Model for how the expense is split among members
export type SplitType = 'equal' | 'custom' | 'percentages';

export interface ExpenseSplit {
  expense_id: string;
  member_id: string;    // ID of the member who owes/is responsible for a share
  share_amount: number; // The amount this specific member owes
  share_percentage?: number; // Optional: If using percentage split
  share_ratio?: number; // Optional: If using ratio/weight split
}

// A simplified model for a Trip Member (needed for expense splitting)
export interface TripMember {
  id: string;           // Unique ID for the member within this trip
  trip_id: string;
  user_id: string;      // The Supabase user ID if they are a registered user
  name: string;
  is_owner: boolean;
}

// Define the structure for a Trip Member
export interface TripMember {
  id: string;        // The unique ID of the member (UUID in 'members' table)
  trip_id: string;   // Foreign key to the trip
  user_id: string;   // The Supabase Auth user ID
  name: string;      // Display name of the member
  is_owner: boolean; // True if this member is the trip creator
  avatar_url: string | null;
  // Note: Balances and settlements are calculated, not stored here.
}

// Define the structure for an Expense
export interface Expense {
  id: string;               // Unique ID of the expense (UUID in 'expenses' table)
  trip_id: string;          // Foreign key to the trip
  paid_by_id: string;       // Foreign key to the TripMember who paid
  amount: number;           // Total amount of the expense
  currency_code: string;    // E.g., 'PHP', 'USD'
  description: string;      // Short description of the expense
  date: string;             // Date of the expense (ISO 8601 string)
  category: string;         // E.g., 'Food', 'Transport', 'Lodging'
  // split_type: 'equal' | 'custom' | 'percentages'; // Future field
  // split_details: any; // JSON object detailing who owes whom
  created_at: string;
}

// Optional: Define possible categories for easier management later
export const EXPENSE_CATEGORIES = [
  'Food',
  'Lodging',
  'Transport',
  'Activity',
  'Groceries',
  'Miscellaneous',
];
