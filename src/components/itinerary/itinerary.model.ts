// Itinerary data model for phases and items

export type ItineraryPhase = 'before' | 'on' | 'after';

export interface ItineraryChecklistItem {
  text: string;
  checked: boolean;
}

export type ItineraryItemType = 'text' | 'checklist' | 'expense';

export interface ExpenseItem {
  name: string;
  amount: number;
  isLibre: boolean;
  participants: string[];
  designatedTo?: string; // for member designation
}

export interface ItineraryItem {
  id: string;
  phase: ItineraryPhase;
  title: string;
  type: ItineraryItemType;
  date?: string;
  time?: string;
  location?: string;
  notes?: string;
  checklist?: ItineraryChecklistItem[];
  attachments?: string[]; // URLs or file references
  amount?: number; // for expense
  paid_by_id?: string; // for expense
  category?: string; // for expense
  split_type?: 'equal' | 'custom' | 'itemized' | 'gifted' | 'individual_shared' | 'equalized_meals'; // for expense
  involved_members?: string[]; // for expense
  custom_splits?: Record<string, number>; // for expense
  expense_items?: ExpenseItem[]; // for expense
  individual_items?: ExpenseItem[]; // for new split modes
  shared_items?: ExpenseItem[]; // for new split modes
  gifted_item_gifter?: string; // for gifted mode
  receipt_url?: string; // for expense
  weatherForecast?: string; // e.g., "Sunny", "Rainy"
  temperature?: number; // e.g., 25 (Celsius)
  weatherIcon?: string; // URL or identifier for weather icon
  lat?: number | undefined; // map pin latitude
  lng?: number | undefined; // map pin longitude
  estimatedCost?: number | undefined; // planned cost for text/checklist items
}
