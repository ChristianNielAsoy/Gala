<template>
  <q-page class="bg-grey-1">
    <!-- Header -->
    <div class="q-pa-md bg-white">
      <div class="row items-center justify-between">
        <div>
          <div class="text-overline text-grey-6">DASHBOARD</div>
          <div class="text-h5 text-weight-bold">Gala</div>
        </div>
        <q-btn round flat icon="add" size="md" @click="goToTrips" />
      </div>
    </div>

    <!-- My Trips Section -->
    <div class="q-pa-md">
      <div class="text-subtitle1 text-weight-bold q-mb-md">My Trips</div>

      <!-- Horizontal Scrollable Trip Cards -->
      <div class="row no-wrap" style="overflow-x: auto; overflow-y: hidden;">
        <q-card
          v-for="trip in recentTrips"
          :key="trip.id"
          flat
          class="trip-card-small q-mr-md cursor-pointer"
          @click="goToTripDetails(trip.id)"
        >
          <div class="trip-card-image-container">
            <img :src="getTripImage(trip)" class="trip-card-image" :alt="trip.name" />
            <div class="trip-card-overlay"></div>

            <!-- Status Badge -->
            <q-badge
              :color="getStatusColor(trip)"
              :label="getStatus(trip)"
              class="absolute-top-right q-ma-sm"
              rounded
            />

            <!-- Trip Info -->
            <div class="trip-card-info">
              <div class="text-body2 text-white text-weight-bold">
                {{ truncate(trip.name, 15) }}
              </div>
              <div class="text-caption text-white">
                {{ formatDateShort(trip.start_date, trip.end_date) }}
              </div>
            </div>
          </div>
        </q-card>

        <!-- Empty State -->
        <q-card
          v-if="recentTrips.length === 0"
          flat
          class="trip-card-small q-mr-md cursor-pointer bg-grey-3"
          @click="goToTrips"
        >
          <div class="trip-card-image-container flex flex-center">
            <div class="text-center">
              <q-icon name="add_circle_outline" size="32px" color="grey-6" />
              <div class="text-caption text-grey-7 q-mt-sm">Add Trip</div>
            </div>
          </div>
        </q-card>
      </div>
    </div>

    <!-- Calendar Section -->
    <div class="q-pa-md">
      <q-card flat class="rounded-borders">
        <q-card-section>
          <q-date
            v-model="selectedDate"
            :events="tripDates"
            event-color="deep-orange"
            flat
            minimal
            class="full-width"
          />
        </q-card-section>
      </q-card>
    </div>

    <!-- Trip Statistics -->
    <div class="q-pa-md">
      <div class="row items-center q-mb-md">
        <div class="text-subtitle1 text-weight-bold">Trip Statistics</div>
        <q-space />
        <q-icon name="grid_view" size="sm" color="grey-6" />
      </div>

      <div class="row q-col-gutter-md">
        <!-- Total Trips -->
        <div class="col-6">
          <q-card flat bordered class="stat-card">
            <q-card-section class="text-center">
              <div class="text-h4 text-weight-bold">{{ totalTrips }}</div>
              <div class="text-caption text-grey-7">Total Trips</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Upcoming Trips -->
        <div class="col-6">
          <q-card flat bordered class="stat-card stat-card-highlighted">
            <q-card-section class="text-center">
              <div class="text-h4 text-weight-bold text-deep-orange">{{ upcomingTrips }}</div>
              <div class="text-caption text-grey-7">Upcoming Trips</div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';

const router = useRouter();

// State
const selectedDate = ref(new Date().toISOString().split('T')[0].replace(/-/g, '/'));
const trips = ref<Trip[]>([]);

// Computed
const recentTrips = computed(() => {
  return trips.value.slice(0, 5);
});

const totalTrips = computed(() => trips.value.length);

const upcomingTrips = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return trips.value.filter(trip => new Date(trip.start_date) > today).length;
});

const tripDates = computed(() => {
  const dates: string[] = [];
  trips.value.forEach(trip => {
    const start = new Date(trip.start_date);
    const end = new Date(trip.end_date);

    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      dates.push(d.toISOString().split('T')[0].replace(/-/g, '/'));
    }
  });
  return dates;
});

// Methods
async function fetchTrips() {
  try {
    const { data: { user } } = await supabase.auth.getUser();

    if (!user) return;

    const { data: tripData } = await supabase
      .from('trips')
      .select('*')
      .eq('user_id', user.id)
      .order('start_date', { ascending: false });

    trips.value = (tripData as Trip[]) || [];
  } catch (err) {
    console.error('Error fetching trips:', err);
  }
}

function goToTrips() {
  void router.push('/trips');
}

function goToTripDetails(tripId: string) {
  void router.push(`/trips/${tripId}`);
}

function getTripImage(trip: Trip): string {
  const images = [
    'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&h=300&fit=crop',
  ];

  const index = Math.abs(trip.name.charCodeAt(0) % images.length);
  return images[index];
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
    default: return 'grey';
  }
}

function formatDateShort(start: string, end: string): string {
  const startDate = new Date(start);
  const endDate = new Date(end);

  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  return `${startDate.toLocaleDateString('en-US', options)} - ${endDate.toLocaleDateString('en-US', options)}`;
}

function truncate(text: string, length: number): string {
  return text.length > length ? text.substring(0, length) + '...' : text;
}

// Lifecycle
onMounted(() => {
  void fetchTrips();
});
</script>

<style scoped>
.trip-card-small {
  min-width: 160px;
  max-width: 160px;
  border-radius: 12px;
  overflow: hidden;
}

.trip-card-image-container {
  position: relative;
  height: 120px;
  overflow: hidden;
}

.trip-card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.trip-card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.1), rgba(0, 0, 0, 0.7));
}

.trip-card-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  z-index: 1;
}

.stat-card {
  border-radius: 12px;
}

.stat-card-highlighted {
  border: 2px solid #FF5722;
}
</style>
