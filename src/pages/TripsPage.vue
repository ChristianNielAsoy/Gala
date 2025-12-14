<template>
  <q-page class="bg-surface">
    <!-- Hero Section -->
    <div
      class="hero-section q-pa-lg text-center text-white"
      :style="{ backgroundImage: `url(${heroImage})` }"
    >
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="text-h3 text-weight-bold q-mb-md">Your Adventures Await</h1>
        <p class="text-subtitle1 q-mb-lg">Plan, track, and cherish every trip</p>
        <q-btn
          unelevated
          rounded
          color="accent"
          icon="add"
          label="Plan New Trip"
          size="lg"
          @click="openNewTripModal"
          class="q-mr-sm"
        />
        <q-btn
          outline
          rounded
          color="white"
          icon="map"
          label="Explore Destinations"
          size="lg"
          @click="toggleMapView"
        />
      </div>
    </div>

    <!-- Search and Filters -->
    <div class="q-pa-md">
      <q-input
        v-model="searchQuery"
        outlined
        rounded
        dense
        placeholder="Search trips..."
        class="q-mb-md"
      >
        <template v-slot:prepend>
          <q-icon name="search" />
        </template>
      </q-input>

      <q-tabs v-model="activeFilter" class="q-mb-md">
        <q-tab name="all" label="All Trips" />
        <q-tab name="upcoming" label="Upcoming" />
        <q-tab name="active" label="Active" />
        <q-tab name="past" label="Past" />
      </q-tabs>
    </div>

    <!-- Trip Cards Grid -->
    <div class="q-pa-md">
      <div v-if="loading" class="text-center q-pa-xl">
        <q-spinner color="primary" size="lg" />
        <p class="text-grey-6 q-mt-md">Loading your trips...</p>
      </div>

      <div v-else-if="filteredTrips.length === 0" class="text-center q-pa-xl">
        <q-icon name="flight_takeoff" size="xl" color="grey-4" class="q-mb-md" />
        <h3 class="text-h6 text-grey-7 q-mb-sm">No trips found</h3>
        <p class="text-grey-6 q-mb-lg">Start your journey by planning your first trip!</p>
        <q-btn
          unelevated
          rounded
          color="primary"
          icon="add"
          label="Create Your First Trip"
          @click="openNewTripModal"
        />
      </div>

      <div v-else class="row q-gutter-md">
        <div v-for="trip in paginatedTrips" :key="trip.id" class="col-12 col-sm-6 col-md-4">
          <q-card flat bordered class="trip-card cursor-pointer" @click="goToTripDetails(trip.id)">
            <q-img :src="getTripImage(trip)" :alt="trip.name" height="150px" class="trip-image">
              <div class="absolute-top-right q-ma-sm">
                <q-badge :color="getStatusColor(trip)" :label="getStatusLabel(trip)" rounded />
              </div>
              <div class="trip-overlay"></div>
              <div class="trip-info">
                <div class="text-body2 text-white text-weight-bold">{{ trip.name }}</div>
                <div class="text-caption text-white">
                  {{ formatDateRange(trip.start_date, trip.end_date) }}
                </div>
              </div>
            </q-img>
            <q-card-section class="q-pa-sm">
              <div class="row items-center justify-between">
                <div>
                  <div class="text-subtitle2 text-weight-bold">{{ trip.name }}</div>
                  <div class="text-caption text-grey-6">
                    {{ trip.destination || 'Destination TBD' }}
                  </div>
                </div>
                <q-icon name="arrow_forward_ios" color="grey-5" size="sm" />
              </div>
              <div class="row q-gutter-xs q-mt-sm">
                <q-chip
                  dense
                  color="primary"
                  text-color="white"
                  icon="group"
                  :label="trip.member_count || 1"
                  size="sm"
                />
                <q-chip
                  dense
                  color="accent"
                  text-color="white"
                  icon="attach_money"
                  :label="formatCurrency(trip.total_expenses || 0)"
                  size="sm"
                />
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Load More -->
      <div v-if="hasMoreTrips" class="text-center q-mt-lg">
        <q-btn
          flat
          color="primary"
          label="Load More Trips"
          @click="loadMoreTrips"
          :loading="loadingMore"
        />
      </div>
    </div>

    <!-- Map View Toggle (Placeholder) -->
    <q-dialog v-model="showMapView">
      <q-card>
        <q-card-section>
          <div class="text-h6">Map View</div>
          <p>Interactive map of all your trips coming soon!</p>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';

const router = useRouter();
const $q = useQuasar();

// State
const loading = ref(true);
const loadingMore = ref(false);
const error = ref<string | null>(null);
const trips = ref<Trip[]>([]);
const showNewTripModal = ref(false);
const activeFilter = ref('all');
const creating = ref(false);
const searchQuery = ref('');
const showMapView = ref(false);
const pageSize = ref(12);
const currentPage = ref(1);

// Computed
const heroImage = computed(() => {
  const latestTrip = trips.value.find((t) => t.start_date && new Date(t.start_date) > new Date());
  return latestTrip ? getTripImage(latestTrip) : 'https://source.unsplash.com/featured/?travel';
});

const filteredTrips = computed(() => {
  let filtered = trips.value;

  // Filter by status
  if (activeFilter.value !== 'all') {
    const now = new Date().toISOString().split('T')[0] ?? '';
    filtered = filtered.filter((trip) => {
      switch (activeFilter.value) {
        case 'upcoming':
          return trip.start_date && trip.start_date > now;
        case 'active':
          return trip.start_date && trip.end_date && trip.start_date <= now && trip.end_date >= now;
        case 'past':
          return trip.end_date && trip.end_date < now;
        default:
          return true;
      }
    });
  }

  // Filter by search
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase();
    filtered = filtered.filter(
      (trip) =>
        trip.name.toLowerCase().includes(query) ||
        (trip.destination && trip.destination.toLowerCase().includes(query)),
    );
  }

  return filtered;
});

const paginatedTrips = computed(() => {
  const start = 0;
  const end = currentPage.value * pageSize.value;
  return filteredTrips.value.slice(start, end);
});

const hasMoreTrips = computed(() => {
  return paginatedTrips.value.length < filteredTrips.value.length;
});

// New Trip Form
const newTrip = ref({
  name: '',
  start_date: new Date().toISOString().split('T')[0] ?? '',
  end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
  currency_code: 'PHP',
});

// Date range for combined picker
const dateRange = ref<{ from: string; to: string }>({
  from: newTrip.value.start_date,
  to: newTrip.value.end_date,
});

// Watch for changes and sync with newTrip
import { watch } from 'vue';
watch(
  dateRange,
  (val) => {
    if (val && val.from && val.to) {
      newTrip.value.start_date = val.from;
      newTrip.value.end_date = val.to;
    }
  },
  { immediate: false },
);

// Currency Options
const currencyOptions = ref<{ label: string; value: string }[]>([]);

// Fetch currencies from database
async function fetchCurrencies(): Promise<void> {
  const { data, error } = await supabase
    .from('currencies')
    .select('code, name')
    .eq('is_active', true)
    .order('display_order');

  if (error) {
    console.warn('Failed to load currencies:', error);
    // Fallback to hardcoded currencies
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

// Computed
const isFormValid = computed(() => {
  return (
    newTrip.value.name &&
    newTrip.value.start_date &&
    newTrip.value.end_date &&
    new Date(newTrip.value.end_date) >= new Date(newTrip.value.start_date)
  );
});

// Collapsible section trip lists
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const today = computed(() => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
});

// Methods
async function fetchTrips() {
  loading.value = true;
  error.value = null;

  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();

    if (!user) {
      error.value = 'Please log in to view your trips';
      return;
    }

    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .select('*')
      .eq('user_id', user.id)
      .order('start_date', { ascending: false });

    if (tripError) throw tripError;

    trips.value = (tripData as Trip[]) || [];
  } catch (err) {
    console.error('Error fetching trips:', err);
    const errorMessage = err instanceof Error ? err.message : 'Failed to load trips';
    error.value = errorMessage;
    $q.notify({
      type: 'negative',
      message: 'Could not load trips: ' + errorMessage,
      position: 'top',
    });
  } finally {
    loading.value = false;
  }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
async function createTrip() {
  if (!isFormValid.value) {
    $q.notify({ type: 'warning', message: 'Please fill all required fields' });
    return;
  }

  creating.value = true;

  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();

    if (!user) {
      throw new Error('You must be logged in to create a trip');
    }

    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .insert({
        user_id: user.id,
        name: newTrip.value.name,
        start_date: newTrip.value.start_date,
        end_date: newTrip.value.end_date,
        currency_code: newTrip.value.currency_code,
      })
      .select()
      .single();

    if (tripError) throw tripError;

    const membersToInsert = [
      {
        trip_id: tripData.id,
        user_id: user.id,
        name: user.email?.split('@')[0] || 'You',
        is_owner: true,
      },
      ...newTripMembers.value.map((name) => ({
        trip_id: tripData.id,
        name,
        is_owner: false,
      })),
    ];
    const { error: memberError } = await supabase.from('members').insert(membersToInsert);
    if (memberError) throw memberError;

    $q.notify({
      type: 'positive',
      message: 'Trip created successfully!',
      position: 'top',
    });

    newTrip.value = {
      name: '',
      start_date: new Date().toISOString().split('T')[0] ?? '',
      end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
      currency_code: 'PHP',
    };
    dateRange.value = { from: newTrip.value.start_date, to: newTrip.value.end_date };
    showNewTripModal.value = false;
    newTripMembers.value = [];
    newMemberName.value = '';

    await fetchTrips();
    void router.push(`/trips/${tripData.id}`);
  } catch (err) {
    console.error('Error creating trip:', err);
    const errorMessage = err instanceof Error ? err.message : 'Unknown error';
    $q.notify({
      type: 'negative',
      message: 'Failed to create trip: ' + errorMessage,
      position: 'top',
    });
  } finally {
    creating.value = false;
  }
}

function openNewTripModal() {
  showNewTripModal.value = true;
}

function goToTripDetails(tripId: string) {
  void router.push(`/trips/${tripId}`);
}

function getTripImage(trip: Trip): string {
  const images = [
    'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=400&fit=crop',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=400&fit=crop',
    'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&h=400&fit=crop',
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=400&fit=crop',
  ];

  const index = Math.abs(trip.name.charCodeAt(0) % images.length);
  return images[index]!;
}

function getStatus(trip: Trip): string {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const start = new Date(trip.start_date);
  const end = new Date(trip.end_date);

  if (start <= today && end >= today) return 'Active';
  if (start > today) return 'Upcoming';
  return 'Completed';
}

function getStatusColor(trip: Trip): string {
  const status = getStatus(trip);
  switch (status) {
    case 'Active':
      return 'green';
    case 'Upcoming':
      return 'cyan';
    case 'Completed':
      return 'grey';
    default:
      return 'grey';
  }
}

function formatDateRange(start: string, end: string): string {
  const startDate = new Date(start);
  const endDate = new Date(end);

  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  const startFormatted = startDate.toLocaleDateString('en-US', options);
  const endFormatted = endDate.toLocaleDateString('en-US', { ...options, year: 'numeric' });

  return `${startFormatted} - ${endFormatted}`;
}

function getStatusLabel(trip: Trip): string {
  const now = new Date().toISOString().split('T')[0] ?? '';
  if (trip.start_date && trip.start_date > now) return 'Upcoming';
  if (trip.end_date && trip.end_date < now) return 'Completed';
  return 'Active';
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

function toggleMapView() {
  showMapView.value = !showMapView.value;
}

function loadMoreTrips() {
  loadingMore.value = true;
  setTimeout(() => {
    currentPage.value++;
    loadingMore.value = false;
  }, 500);
}

// Lifecycle
onMounted(async () => {
  const {
    data: { session },
  } = await supabase.auth.getSession();

  if (!session) {
    $q.notify({
      type: 'warning',
      message: 'Please log in to view your trips',
      position: 'top',
    });
    void router.push('/login');
    return;
  }

  await Promise.all([fetchCurrencies(), fetchTrips()]);

  const channel = supabase
    .channel('trips_changes')
    .on(
      'postgres_changes',
      {
        event: '*',
        schema: 'public',
        table: 'trips',
      },
      () => {
        void fetchTrips();
      },
    )
    .subscribe();

  return () => {
    void supabase.removeChannel(channel);
  };
});

// Members Input Logic
const newTripMembers = ref<string[]>([]);
const newMemberName = ref('');
// eslint-disable-next-line @typescript-eslint/no-unused-vars
function addMember() {
  const name = newMemberName.value.trim();
  if (name && !newTripMembers.value.includes(name)) {
    newTripMembers.value.push(name);
    newMemberName.value = '';
  }
}
// eslint-disable-next-line @typescript-eslint/no-unused-vars
function removeMember(idx: number) {
  newTripMembers.value.splice(idx, 1);
}
</script>

<style scoped>
.hero-section {
  position: relative;
  height: 300px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.trip-card {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
  overflow: hidden;
  border-radius: 16px;
}

.trip-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.trip-image {
  transition: transform 0.2s ease;
}

.trip-card:hover .trip-image {
  transform: scale(1.05);
}

.trip-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.1), rgba(0, 0, 0, 0.7));
}

.trip-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px;
  z-index: 1;
}

.new-trip-modal {
  width: 100%;
  max-width: 500px;
  border-radius: 24px;
}

.centered-modal {
  margin: 0 auto;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  position: absolute;
}
</style>
