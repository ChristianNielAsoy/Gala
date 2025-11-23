import { boot } from 'quasar/wrappers' // <-- FIX: Changed import from 'quasar' to 'quasar/wrappers'
import { createClient, SupabaseClient } from '@supabase/supabase-js'
import { App } from 'vue'

// 1. Get the keys from environment variables and assert they are strings
// The VITE_ prefix is required for Vite/Quasar to expose it to the client side.
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL as string
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY as string

// 2. Initialize the client, explicitly typing the result as SupabaseClient
const supabase: SupabaseClient = createClient(supabaseUrl, supabaseAnonKey)

// 3. Inject the client into the Vue instance using the Quasar boot wrapper
// FIX: Explicitly type the destructured 'app' parameter as '{ app: App }'
export default boot(({ app }: { app: App }) => {
  // Inject the client into Vue's global properties for use as this.$supabase
  app.config.globalProperties.$supabase = supabase
})

// 4. Also export it directly for use in other TS modules (like Router middleware)
export { supabase }
