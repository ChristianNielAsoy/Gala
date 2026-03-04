import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Trip } from 'src/types/expense';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';

export const useTripStore = defineStore('trips', () => {
  const trips = ref<Trip[]>([]);
  const isLoading = ref(false);
  const isLoaded = ref(false);
  const currencyOptions = ref<{ label: string; value: string }[]>([]);

  const tripById = computed(() => (id: string) => trips.value.find((t) => t.id === id));

  // Derived list of trip IDs — useful for activity log queries and cross-page lookups
  const tripIds = computed(() => trips.value.map((t) => t.id));

  // Throws on error — caller is responsible for catching and showing UI feedback.
  // Fetches trips the user is a member of (covers owned + joined trips).
  async function fetchTrips(force = false) {
    if (isLoaded.value && !force) return;

    const authStore = useAuthStore();
    if (!authStore.user) return;

    isLoading.value = true;
    try {
      // Step 1: Get all trip IDs the user is a member of
      const { data: memberData, error: memberError } = await supabase
        .from('members')
        .select('trip_id')
        .eq('user_id', authStore.user.id);

      if (memberError) throw memberError;

      const ids = memberData?.map((m) => m.trip_id as string) ?? [];

      if (ids.length === 0) {
        trips.value = [];
        isLoaded.value = true;
        return;
      }

      // Step 2: Fetch those trips
      const { data, error } = await supabase
        .from('trips')
        .select('*')
        .in('id', ids)
        .order('start_date', { ascending: false });

      if (error) throw error;
      trips.value = (data as Trip[]) || [];
      isLoaded.value = true;
    } finally {
      isLoading.value = false;
    }
  }

  function addTrip(trip: Trip) {
    trips.value.unshift(trip);
  }

  function removeTrip(id: string) {
    trips.value = trips.value.filter((t) => t.id !== id);
  }

  function updateTrip(updated: Trip) {
    const idx = trips.value.findIndex((t) => t.id === updated.id);
    if (idx !== -1) trips.value[idx] = updated;
  }

  // Cached — only fetches once unless currencies table is empty
  async function fetchCurrencies() {
    if (currencyOptions.value.length > 0) return;

    const { data, error } = await supabase
      .from('currencies')
      .select('code, name')
      .eq('is_active', true)
      .order('display_order');

    if (error || !data) {
      currencyOptions.value = [
        { label: 'PHP - Philippine Peso', value: 'PHP' },
        { label: 'USD - US Dollar', value: 'USD' },
        { label: 'EUR - Euro', value: 'EUR' },
        { label: 'JPY - Japanese Yen', value: 'JPY' },
        { label: 'SGD - Singapore Dollar', value: 'SGD' },
      ];
    } else {
      currencyOptions.value = data.map((curr) => ({
        label: `${curr.code} - ${curr.name}`,
        value: curr.code,
      }));
    }
  }

  // Call on logout to clear cached state
  function reset() {
    trips.value = [];
    isLoaded.value = false;
    currencyOptions.value = [];
  }

  return {
    trips,
    tripIds,
    isLoading,
    isLoaded,
    currencyOptions,
    tripById,
    fetchTrips,
    addTrip,
    removeTrip,
    updateTrip,
    fetchCurrencies,
    reset,
  };
});
