export interface Trip {
  id: string;
  user_id: string;
  name: string;
  start_date: string;
  end_date: string;
  currency_code: string;
  created_at?: string;
}
