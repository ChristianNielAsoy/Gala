import { boot } from 'quasar/wrappers';
import { createClient } from '@supabase/supabase-js';
import type { SupabaseClient } from '@supabase/supabase-js';
import type { App } from 'vue';

// Get the keys from environment variables and assert they are strings
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL as string;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY as string;

// Initialize the Supabase client
const supabase: SupabaseClient = createClient(supabaseUrl, supabaseAnonKey);

// Inject the client into the Vue instance using the Quasar boot wrapper
export default boot(({ app }: { app: App }) => {
  // Inject the client into Vue's global properties for use as this.$supabase
  app.config.globalProperties.$supabase = supabase;
});

// Export it directly for use in other TS modules (like Router middleware)
export { supabase };
