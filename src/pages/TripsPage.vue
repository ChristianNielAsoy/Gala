<template>
  <q-page class="bg-grey-1">
    <!-- Header -->
    <div class="q-pa-md bg-white">
      <div class="row items-center justify-between q-mb-md">
        <div class="text-h5 text-weight-bold">Trips</div>
        <q-btn round flat icon="notifications_none" />
      </div>

      <!-- Create Trip Button -->
      <q-btn
        unelevated
        rounded
        color="deep-orange"
        text-color="white"
        icon="add"
        label="Plan a New Trip"
        class="full-width q-py-sm text-weight-medium"
        @click="openNewTripModal"
      />

      <!-- Filter Chips -->
      <div class="row q-gutter-sm q-mt-md">
        <q-chip
          v-for="filter in filters"
          :key="filter.value"
          clickable
          :color="activeFilter === filter.value ? 'deep-orange' : 'grey-3'"
          :text-color="activeFilter === filter.value ? 'white' : 'grey-7'"
          :label="filter.label"
          @click="activeFilter = filter.value"
        >
          <template v-if="filter.count !== undefined">
            <span class="q-ml-xs">{{ filter.count }}</span>
          </template>
        </q-chip>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="q-pa-xl text-center">
      <q-spinner color="deep-orange" size="lg" />
      <p class="text-grey-6 q-mt-md">Loading trips...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="q-pa-xl text-center">
      <q-icon name="error_outline" size="xl" color="negative" />
      <p class="text-h6 q-mt-md">{{ error }}</p>
      <q-btn flat color="deep-orange" label="Try Again" @click="fetchTrips" class="q-mt-md" />
    </div>

    <!-- Trips Grid -->
    <div v-else class="q-pa-md">
      <!-- Trip Cards -->
      <div class="q-gutter-md">
        <q-card
          v-for="trip in filteredTrips"
          :key="trip.id"
          flat
          class="trip-card cursor-pointer"
          @click="goToTripDetails(trip.id)"
        >
          <div class="trip-image-container">
            <img
              :src="getTripImage(trip)"
              class="trip-image"
              :alt="trip.name"
            />
            <div class="trip-image-overlay"></div>

            <!-- Status Badge -->
            <q-badge
              :color="getStatusColor(trip)"
              :label="getStatus(trip)"
              class="absolute-top-right q-ma-md"
              rounded
            />

            <!-- Trip Info Overlay -->
            <div class="trip-info-overlay">
              <div class="text-h6 text-white text-weight-bold">{{ trip.name }}</div>
              <div class="text-white text-caption q-mt-xs">
                <q-icon name="event" size="xs" class="q-mr-xs" />
                {{ formatDateRange(trip.start_date, trip.end_date) }}
              </div>
            </div>
          </div>
        </q-card>

        <!-- Empty State -->
        <div v-if="filteredTrips.length === 0" class="text-center q-py-xl">
          <q-icon name="flight_takeoff" size="64px" color="grey-4" />
          <p class="text-h6 text-grey-6 q-mt-md">No {{ activeFilter === 'all' ? '' : activeFilter }} trips yet</p>
          <p class="text-grey-5">Start planning your next gala!</p>
          <q-btn
            unelevated
            rounded
            color="deep-orange"
            label="Plan Your First Trip"
            class="q-mt-md"
            @click="openNewTripModal"
          />
        </div>
      </div>
    </div>

    <!-- New Trip Modal -->
    <q-dialog v-model="showNewTripModal" position="bottom">
      <q-card class="new-trip-modal">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6 text-weight-bold">Plan a New Trip</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section>
          <q-form @submit.prevent="createTrip" class="q-gutter-md">
            <!-- Trip Name -->
            <q-input
              v-model="newTrip.name"
              label="Trip Name"
              placeholder="e.g., Palawan Adventure"
              outlined
              rounded
              :rules="[val => !!val || 'Trip name is required']"
            >
              <template v-slot:prepend>
                <q-icon name="location_on" color="deep-orange" />
              </template>
            </q-input>

            <!-- Date Range -->
            <div class="row q-col-gutter-sm">
              <div class="col-6">
                <q-input
                  v-model="newTrip.start_date"
                  label="Start Date"
                  mask="date"
                  :rules="['date']"
                  outlined
                  rounded
                >
                  <template v-slot:prepend>
                    <q-icon name="event" color="deep-orange" />
                  </template>
                  <template v-slot:append>
                    <q-icon name="calendar_today" class="cursor-pointer">
                      <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                        <q-date v-model="newTrip.start_date" minimal />
                      </q-popup-proxy>
                    </q-icon>
                  </template>
                </q-input>
              </div>
              <div class="col-6">
                <q-input
                  v-model="newTrip.end_date"
                  label="End Date"
                  mask="date"
                  :rules="['date']"
                  outlined
                  rounded
                >
                  <template v-slot:append>
                    <q-icon name="calendar_today" class="cursor-pointer">
                      <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                        <q-date v-model="newTrip.end_date" minimal />
                      </q-popup-proxy>
                    </q-icon>
                  </template>
                </q-input>
              </div>
            </div>

            <!-- Currency -->
            <q-select
              v-model="newTrip.currency_code"
              :options="currencyOptions"
              label="Currency"
              outlined
              rounded
              emit-value
              map-options
            >
              <template v-slot:prepend>
                <q-icon name="payments" color="deep-orange" />
              </template>
            </q-select>

            <!-- Action Buttons -->
            <div class="row q-gutter-sm q-pt-md">
              <q-btn
                flat
                rounded
                label="Cancel"
                color="grey-7"
                class="col"
                v-close-popup
              />
              <q-btn
                unelevated
                rounded
                type="submit"
                label="Create Trip"
                color="deep-orange"
                text-color="white"
                class="col"
                :loading="creating"
                :disable="!isFormValid"
              />
            </div>
          </q-form>
        </q-card-section>
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
const creating = ref(false);
const error = ref<string | null>(null);
const trips = ref<Trip[]>([]);
const showNewTripModal = ref(false);
const activeFilter = ref('upcoming');

// Filters
const filters = computed(() => [
  { label: 'Upcoming', value: 'upcoming', count: upcomingCount.value },
  { label: 'Active', value: 'active', count: activeCount.value },
  { label: 'Past', value: 'past', count: pastCount.value },
  { label: 'All', value: 'all', count: trips.value.length },
]);

// New Trip Form
const newTrip = ref({
  name: '',
  start_date: new Date().toISOString().split('T')[0] ?? '',
  end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
  currency_code: 'PHP'
});

// Currency Options
const currencyOptions = [
  { label: 'PHP - Philippine Peso', value: 'PHP' },
  { label: 'USD - US Dollar', value: 'USD' },
  { label: 'EUR - Euro', value: 'EUR' },
  { label: 'JPY - Japanese Yen', value: 'JPY' },
  { label: 'SGD - Singapore Dollar', value: 'SGD' },
];

// Computed
const isFormValid = computed(() => {
  return newTrip.value.name &&
         newTrip.value.start_date &&
         newTrip.value.end_date &&
         new Date(newTrip.value.end_date) >= new Date(newTrip.value.start_date);
});

const upcomingCount = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return trips.value.filter(trip => new Date(trip.start_date) > today).length;
});

const activeCount = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return trips.value.filter(trip => {
    const start = new Date(trip.start_date);
    const end = new Date(trip.end_date);
    return start <= today && end >= today;
  }).length;
});

const pastCount = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return trips.value.filter(trip => new Date(trip.end_date) < today).length;
});

const filteredTrips = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  switch (activeFilter.value) {
    case 'upcoming':
      return trips.value.filter(trip => new Date(trip.start_date) > today);
    case 'active':
      return trips.value.filter(trip => {
        const start = new Date(trip.start_date);
        const end = new Date(trip.end_date);
        return start <= today && end >= today;
      });
    case 'past':
      return trips.value.filter(trip => new Date(trip.end_date) < today);
    default:
      return trips.value;
  }
});

// Methods
async function fetchTrips() {
  loading.value = true;
  error.value = null;

  try {
    const { data: { user } } = await supabase.auth.getUser();

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
      position: 'top'
    });
  } finally {
    loading.value = false;
  }
}

async function createTrip() {
  if (!isFormValid.value) {
    $q.notify({ type: 'warning', message: 'Please fill all required fields' });
    return;
  }

  creating.value = true;

  try {
    const { data: { user } } = await supabase.auth.getUser();

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
        currency_code: newTrip.value.currency_code
      })
      .select()
      .single();

    if (tripError) throw tripError;

    const { error: memberError } = await supabase
      .from('members')
      .insert({
        trip_id: tripData.id,
        user_id: user.id,
        name: user.email?.split('@')[0] || 'You',
        is_owner: true
      });

    if (memberError) throw memberError;

    $q.notify({
      type: 'positive',
      message: 'Trip created successfully!',
      position: 'top'
    });

    newTrip.value = {
      name: '',
      start_date: new Date().toISOString().split('T')[0] ?? '',
      end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
      currency_code: 'PHP'
    };
    showNewTripModal.value = false;

    await fetchTrips();
    void router.push(`/trips/${tripData.id}`);
  } catch (err) {
    console.error('Error creating trip:', err);
    const errorMessage = err instanceof Error ? err.message : 'Unknown error';
    $q.notify({
      type: 'negative',
      message: 'Failed to create trip: ' + errorMessage,
      position: 'top'
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
    case 'Active': return 'green';
    case 'Upcoming': return 'cyan';
    case 'Completed': return 'grey';
    default: return 'grey';
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

// Lifecycle
onMounted(async () => {
  const { data: { session } } = await supabase.auth.getSession();

  if (!session) {
    $q.notify({
      type: 'warning',
      message: 'Please log in to view your trips',
      position: 'top'
    });
    void router.push('/login');
    return;
  }

  void fetchTrips();

  const channel = supabase
    .channel('trips_changes')
    .on('postgres_changes', {
      event: '*',
      schema: 'public',
      table: 'trips'
    }, () => {
      void fetchTrips();
    })
    .subscribe();

  return () => {
    void supabase.removeChannel(channel);
  };
});
</script>

<style scoped>
.trip-card {
  overflow: hidden;
  border-radius: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.trip-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.trip-image-container {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.trip-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.trip-image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.1), rgba(0, 0, 0, 0.6));
}

.trip-info-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  z-index: 1;
}

.new-trip-modal {
  width: 100%;
  max-width: 500px;
  border-radius: 24px 24px 0 0;
}
</style>
