// Itinerary data model for phases and items

export type ItineraryPhase = 'before' | 'on' | 'after';

export interface ItineraryChecklistItem {
  text: string;
  checked: boolean;
}

export type ItineraryItemType = 'text' | 'checklist' | 'expense';

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
}
