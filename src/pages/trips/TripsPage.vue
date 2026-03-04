<template>
  <q-page class="bg-surface">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="text-h4 text-white text-weight-bold q-mb-sm">Your Adventures</h1>
        <p class="text-white q-mb-lg hero-subtitle">
          Plan, split, and cherish every trip with your barkada
        </p>
        <q-btn
          unelevated
          color="white"
          text-color="primary"
          icon="add"
          label="Plan New Trip"
          no-caps
          @click="openNewTripModal"
        />
      </div>
    </div>

    <!-- Search and Filters -->
    <div class="q-pa-md">
      <q-input
        v-model="searchQuery"
        outlined
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
      <div v-if="tripStore.isLoading" class="text-center q-pa-xl">
        <q-spinner color="primary" size="lg" />
        <p class="text-grey-6 q-mt-md">Loading your trips...</p>
      </div>

      <div v-else-if="!tripStore.isLoading && filteredTrips.length === 0" class="text-center q-pa-xl">
        <q-icon name="flight_takeoff" size="xl" color="grey-4" class="q-mb-md" />
        <h3 class="text-h6 text-grey-7 q-mb-sm">No trips found</h3>
        <p class="text-grey-6 q-mb-lg">Start your journey by planning your first trip!</p>
        <q-btn
          unelevated
          color="primary"
          icon="add"
          label="Create Your First Trip"
          no-caps
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
                  :label="formatCurrency(trip.total_expenses || 0, trip.currency_code)"
                  size="sm"
                />
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Load More -->
      <div v-if="hasMoreTrips" class="text-center q-mt-lg">
        <q-btn flat no-caps color="primary" label="Show More" @click="loadMoreTrips" />
      </div>
    </div>

    <!-- New Trip Modal -->
    <q-dialog v-model="showNewTripModal" persistent>
      <q-card class="new-trip-modal">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Plan a New Trip</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section class="q-gutter-y-md q-pt-md">
          <!-- Trip Name -->
          <q-input
            v-model="newTrip.name"
            label="Trip Name"
            placeholder="e.g., Boracay Summer 2025"
            outlined
            dense
            :rules="[(val: string) => !!val || 'Trip name is required']"
          >
            <template v-slot:prepend><q-icon name="flight_takeoff" /></template>
          </q-input>

          <!-- Destination -->
          <q-input
            v-model="newTrip.destination"
            label="Destination (optional)"
            placeholder="e.g., Boracay, Aklan"
            outlined
            dense
          >
            <template v-slot:prepend><q-icon name="place" /></template>
          </q-input>

          <!-- Date Range -->
          <div>
            <div class="text-caption text-grey-7 q-mb-xs">Travel Dates</div>
            <div class="row q-gutter-sm">
              <q-input
                v-model="newTrip.start_date"
                label="Start Date"
                mask="date"
                :rules="['date']"
                outlined
                dense
                class="col"
              >
                <template v-slot:append>
                  <q-icon name="event" class="cursor-pointer">
                    <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                      <q-date v-model="newTrip.start_date" />
                    </q-popup-proxy>
                  </q-icon>
                </template>
              </q-input>
              <q-input
                v-model="newTrip.end_date"
                label="End Date"
                mask="date"
                :rules="['date']"
                outlined
                dense
                class="col"
              >
                <template v-slot:append>
                  <q-icon name="event" class="cursor-pointer">
                    <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                      <q-date v-model="newTrip.end_date" :options="(d) => d >= newTrip.start_date" />
                    </q-popup-proxy>
                  </q-icon>
                </template>
              </q-input>
            </div>
          </div>

          <!-- Currency + Budget -->
          <div class="row q-gutter-sm">
            <q-select
              v-model="newTrip.currency_code"
              :options="tripStore.currencyOptions"
              label="Currency"
              outlined
              dense
              emit-value
              map-options
              class="col"
            >
              <template v-slot:prepend><q-icon name="payments" /></template>
            </q-select>

            <q-input
              v-model.number="newTrip.budget_amount"
              label="Budget (optional)"
              type="number"
              outlined
              dense
              class="col"
              :prefix="newTrip.currency_code"
            />
          </div>

          <!-- Add Members -->
          <div>
            <div class="text-caption text-grey-7 q-mb-xs">Add Barkada Members (optional)</div>
            <div class="row q-gutter-sm">
              <q-input
                v-model="newMemberName"
                label="Member name"
                outlined
                dense
                class="col"
                @keyup.enter="addMember"
              />
              <q-btn
                flat
                round
                icon="person_add"
                color="primary"
                @click="addMember"
                :disable="!newMemberName.trim()"
              />
            </div>
            <div v-if="newTripMembers.length > 0" class="row q-gutter-xs q-mt-sm">
              <q-chip
                v-for="(name, idx) in newTripMembers"
                :key="idx"
                removable
                @remove="removeMember(idx)"
                color="primary"
                text-color="white"
                icon="person"
                dense
              >
                {{ name }}
              </q-chip>
            </div>
          </div>
        </q-card-section>

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat no-caps label="Cancel" v-close-popup />
          <q-btn
            unelevated
            no-caps
            color="primary"
            label="Create Trip"
            :loading="creating"
            :disable="!isFormValid"
            @click="createTrip"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import { useTripStore } from 'src/stores/tripStore';
import type { Trip } from 'src/types/expense';

const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();
const tripStore = useTripStore();

// UI-only state (stays local)
const showNewTripModal = ref(false);
const activeFilter = ref('all');
const creating = ref(false);
const searchQuery = ref('');
const pageSize = ref(12);
const currentPage = ref(1);

const filteredTrips = computed(() => {
  let filtered = tripStore.trips;

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
  return filteredTrips.value.slice(0, currentPage.value * pageSize.value);
});

const hasMoreTrips = computed(() => {
  return paginatedTrips.value.length < filteredTrips.value.length;
});

const newTrip = ref({
  name: '',
  destination: '',
  start_date: new Date().toISOString().split('T')[0] ?? '',
  end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
  currency_code: 'PHP',
  budget_amount: null as number | null,
});

const isFormValid = computed(() => {
  return (
    newTrip.value.name &&
    newTrip.value.start_date &&
    newTrip.value.end_date &&
    new Date(newTrip.value.end_date) >= new Date(newTrip.value.start_date)
  );
});

async function loadTrips() {
  try {
    await tripStore.fetchTrips();
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Failed to load trips';
    $q.notify({ type: 'negative', message: 'Could not load trips: ' + msg, position: 'top' });
  }
}

async function createTrip() {
  if (!isFormValid.value) {
    $q.notify({ type: 'warning', message: 'Please fill all required fields' });
    return;
  }

  creating.value = true;

  try {
    const user = authStore.user;
    if (!user) throw new Error('You must be logged in to create a trip');

    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .insert({
        user_id: user.id,
        name: newTrip.value.name,
        destination: newTrip.value.destination || null,
        start_date: newTrip.value.start_date,
        end_date: newTrip.value.end_date,
        currency_code: newTrip.value.currency_code,
        budget_amount: newTrip.value.budget_amount || null,
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

    // Update store cache directly — no extra DB fetch needed
    tripStore.addTrip(tripData as Trip);

    $q.notify({ type: 'positive', message: 'Trip created successfully!', position: 'top' });

    newTrip.value = {
      name: '',
      destination: '',
      start_date: new Date().toISOString().split('T')[0] ?? '',
      end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
      currency_code: 'PHP',
    };
    showNewTripModal.value = false;
    newTripMembers.value = [];
    newMemberName.value = '';

    void router.push(`/trips/${tripData.id}`);
  } catch (err) {
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

function getStatusLabel(trip: Trip): string {
  const now = new Date().toISOString().split('T')[0] ?? '';
  if (trip.start_date && trip.start_date > now) return 'Upcoming';
  if (trip.end_date && trip.end_date < now) return 'Completed';
  return 'Active';
}

function getStatusColor(trip: Trip): string {
  const label = getStatusLabel(trip);
  if (label === 'Active') return 'primary';
  if (label === 'Upcoming') return 'accent';
  return 'grey-6';
}

function formatDateRange(start: string, end: string): string {
  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  const startFormatted = new Date(start).toLocaleDateString('en-US', options);
  const endFormatted = new Date(end).toLocaleDateString('en-US', { ...options, year: 'numeric' });
  return `${startFormatted} – ${endFormatted}`;
}

function formatCurrency(amount: number, currency = 'PHP'): string {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency }).format(amount);
}

function loadMoreTrips() {
  currentPage.value++;
}

// Real-time subscription — invalidates store cache on remote change
let realtimeChannel: ReturnType<typeof supabase.channel> | null = null;

onMounted(async () => {
  await Promise.all([tripStore.fetchCurrencies(), loadTrips()]);

  realtimeChannel = supabase
    .channel('trips_changes')
    .on('postgres_changes', { event: '*', schema: 'public', table: 'trips' }, () => {
      void tripStore.fetchTrips(true);
    })
    .subscribe();
});

onUnmounted(() => {
  if (realtimeChannel) void supabase.removeChannel(realtimeChannel);
});

const newTripMembers = ref<string[]>([]);
const newMemberName = ref('');

function addMember() {
  const name = newMemberName.value.trim();
  if (name && !newTripMembers.value.includes(name)) {
    newTripMembers.value.push(name);
    newMemberName.value = '';
  }
}

function removeMember(idx: number) {
  newTripMembers.value.splice(idx, 1);
}
</script>

<style scoped lang="scss">
.hero-section {
  background: linear-gradient(135deg, #0d9488 0%, #065f52 100%);
  padding: 56px 32px;
  text-align: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    width: 500px;
    height: 500px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
    top: -250px;
    right: -100px;
  }

  &::after {
    content: '';
    position: absolute;
    width: 300px;
    height: 300px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.03);
    bottom: -150px;
    left: -60px;
  }
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-subtitle {
  font-size: 0.9375rem;
  opacity: 0.88;
}

.trip-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.trip-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--gala-shadow-lg);
}

.trip-card:hover .trip-image {
  transform: scale(1.05);
}

.trip-image {
  transition: transform 0.3s ease;
}

.trip-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.05), rgba(0, 0, 0, 0.65));
}

.trip-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 12px;
  z-index: 1;
}

.new-trip-modal {
  width: 100%;
  max-width: 500px;
}
</style>
