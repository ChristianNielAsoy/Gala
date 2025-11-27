export interface Trip {
  id: string;
  user_id: string;
  name: string;
  start_date: string;
  end_date: string;
  currency_code: string;
  currencyCode?: string;  // Add this line - TypeScript needs it explicitly
  created_at: string;
}
