<template>
  <q-page class="q-pb-xl">
    <!-- Header -->
    <div class="q-pa-md">
      <div class="row items-center justify-between q-mb-md">
        <div class="text-h4 text-weight-bold">Your Trips</div>
        <q-btn
          round
          color="primary"
          icon="add"
          size="md"
          @click="openNewTripDialog"
          class="shadow-2"
          aria-label="Add new trip"
        />
      </div>

      <!-- Filter Tabs -->
      <q-tabs
        v-model="activeFilter"
        dense
        class="text-grey-7 q-mb-md"
        active-color="primary"
        indicator-color="primary"
        align="left"
      >
        <q-tab name="all" label="All" />
        <q-tab name="upcoming" label="Upcoming" />
        <q-tab name="active" label="Active" />
        <q-tab name="past" label="Past" />
      </q-tabs>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex flex-center q-pa-xl">
      <q-spinner color="primary" size="50px" />
    </div>

    <!-- Trips Grid -->
    <div v-else-if="filteredTrips.length > 0" class="q-px-md">
      <div class="row q-col-gutter-md">
        <div
          v-for="trip in filteredTrips"
          :key="trip.id"
          class="col-12 col-sm-6 col-md-4"
        >
          <q-card
            class="trip-card cursor-pointer shadow-2 q-hoverable"
            @click="goToTripDetails(trip.id)"
          >
            <!-- Trip Image/Header -->
            <div class="trip-image-container">
              <img
                :src="getTripImage(trip.name)"
                class="trip-image"
                :alt="trip.name"
              />
              <div class="trip-status-badge" :class="getStatusClass(trip)">
                {{ getStatusLabel(trip) }}
              </div>
            </div>

            <!-- Trip Info -->
            <q-card-section>
              <div class="text-h6 text-weight-bold q-mb-xs ellipsis-2-lines">
                {{ trip.name }}
              </div>

              <div class="text-caption text-grey-7 q-mb-sm">
                <q-icon name="event" size="xs" class="q-mr-xs" />
                {{ formatDateRange(trip.start_date, trip.end_date) }}
              </div>

              <div class="row items-center justify-between q-mt-md">
                <div class="text-caption text-grey-7">
                  <q-icon name="people" size="xs" class="q-mr-xs" />
                  {{ getMemberCount(trip.id) }} Participants
                </div>
                <div class="text-weight-bold text-primary">
                  {{ trip.currency_code }}
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="flex flex-center column q-pa-xl">
      <q-icon name="beach_access" size="80px" color="grey-4" class="q-mb-md" />
      <div class="text-h6 text-grey-6 q-mb-sm">No trips found</div>
      <div class="text-body2 text-grey-5 q-mb-lg text-center">
        {{ getEmptyMessage() }}
      </div>
      <q-btn
        color="primary"
        label="Plan Your First Trip"
        icon="add"
        @click="openNewTripDialog"
        rounded
        unelevated
      />
    </div>

    <!-- New Trip Dialog -->
    <q-dialog v-model="showNewTripDialog" persistent>
      <q-card style="min-width: 400px; max-width: 500px;">
        <q-card-section>
          <div class="text-h6">Plan a New Trip</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <q-form @submit.prevent="createTrip">
            <q-input
              v-model="newTrip.name"
              label="Trip Name"
              placeholder="e.g., Palawan Adventure 2025"
              outlined
              dense
              class="q-mb-md"
              :rules="[(val: string) => !!val || 'Trip name is required']"
            >
              <template v-slot:prepend>
                <q-icon name="flight_takeoff" />
              </template>
            </q-input>

            <q-input
              v-model="newTrip.start_date"
              label="Start Date"
              type="date"
              outlined
              dense
              class="q-mb-md"
              :rules="[(val: string) => !!val || 'Start date is required']"
            >
              <template v-slot:prepend>
                <q-icon name="event" />
              </template>
            </q-input>

            <q-input
              v-model="newTrip.end_date"
              label="End Date"
              type="date"
              outlined
              dense
              class="q-mb-md"
              :rules="[
                (val: string) => !!val || 'End date is required',
                (val: string) => val >= newTrip.start_date || 'End date must be after start date'
              ]"
            >
              <template v-slot:prepend>
                <q-icon name="event" />
              </template>
            </q-input>

            <q-select
              v-model="newTrip.currency_code"
              :options="currencyOptions"
              label="Currency"
              outlined
              dense
              emit-value
              map-options
            >
              <template v-slot:prepend>
                <q-icon name="payments" />
              </template>
            </q-select>
          </q-form>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-7" v-close-popup :disable="creating" />
          <q-btn
            label="Create Trip"
            color="primary"
            @click="createTrip"
            :loading="creating"
            unelevated
          />
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
const creating = ref(false);
const trips = ref<Trip[]>([]);
const activeFilter = ref('all');
const showNewTripDialog = ref(false);
const memberCounts = ref<Record<string, number>>({});

// New Trip Form
const newTrip = ref({
  name: '',
  start_date: '',
  end_date: '',
  currency_code: 'PHP',
});

const currencyOptions = [
  { label: 'PHP - Philippine Peso', value: 'PHP' },
  { label: 'USD - US Dollar', value: 'USD' },
  { label: 'EUR - Euro', value: 'EUR' },
  { label: 'JPY - Japanese Yen', value: 'JPY' },
  { label: 'SGD - Singapore Dollar', value: 'SGD' },
];

// Computed
const filteredTrips = computed(() => {
  const now = new Date().toISOString().split('T')[0] ?? '';

  return trips.value.filter(trip => {
    if (activeFilter.value === 'all') return true;
    if (activeFilter.value === 'upcoming') return trip.start_date && trip.start_date > now;
    if (activeFilter.value === 'active') return trip.start_date && trip.end_date && trip.start_date <= now && trip.end_date >= now;
    if (activeFilter.value === 'past') return trip.end_date && trip.end_date < now;
    return true;
  });
});

// Methods
async function fetchTrips(): Promise<void> {
  loading.value = true;
  try {
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Not authenticated');

    // Get trips where user is a member
    const { data: memberData, error: memberError } = await supabase
      .from('members')
      .select('trip_id')
      .eq('user_id', user.id);

    if (memberError) throw memberError;

    const tripIds = memberData?.map(m => m.trip_id) || [];

    if (tripIds.length === 0) {
      trips.value = [];
      loading.value = false;
      return;
    }

    // Fetch trips
    const { data: tripsData, error: tripsError } = await supabase
      .from('trips')
      .select('*')
      .in('id', tripIds)
      .order('start_date', { ascending: false });

    if (tripsError) throw tripsError;

    trips.value = (tripsData as Trip[]) || [];

    // Fetch member counts for each trip
    await fetchMemberCounts(tripIds);

  } catch (error) {
    console.error('Error fetching trips:', error);
    $q.notify({ type: 'negative', message: 'Failed to load trips' });
  } finally {
    loading.value = false;
  }
}

async function fetchMemberCounts(tripIds: string[]): Promise<void> {
  const counts: Record<string, number> = {};

  for (const tripId of tripIds) {
    const { count } = await supabase
      .from('members')
      .select('*', { count: 'exact', head: true })
      .eq('trip_id', tripId);

    counts[tripId] = count || 0;
  }

  memberCounts.value = counts;
}

function getMemberCount(tripId: string): number {
  return memberCounts.value[tripId] || 0;
}

async function createTrip(): Promise<void> {
  if (!newTrip.value.name || !newTrip.value.start_date || !newTrip.value.end_date) {
    $q.notify({ type: 'warning', message: 'Please fill in all fields' });
    return;
  }

  if (newTrip.value.end_date < newTrip.value.start_date) {
    $q.notify({ type: 'warning', message: 'End date must be after start date' });
    return;
  }

  creating.value = true;

  try {
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Not authenticated');

    // Create trip
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

    // Add creator as member
    const { error: memberError } = await supabase
      .from('members')
      .insert({
        trip_id: tripData.id,
        user_id: user.id,
        name: user.email?.split('@')[0] || 'You',
        is_owner: true,
      });

    if (memberError) throw memberError;

    $q.notify({ type: 'positive', message: 'Trip created successfully!' });

    // Reset form
    newTrip.value = {
      name: '',
      start_date: '',
      end_date: '',
      currency_code: 'PHP',
    };

    showNewTripDialog.value = false;

    // Refresh trips list
    await fetchTrips();

  } catch (error) {
    console.error('Error creating trip:', error);
    const errorMessage = error instanceof Error ? error.message : 'Failed to create trip';
    $q.notify({ type: 'negative', message: errorMessage });
  } finally {
    creating.value = false;
  }
}

function openNewTripDialog(): void {
  showNewTripDialog.value = true;
}

function goToTripDetails(tripId: string): void {
  void router.push(`/trips/${tripId}`);
}

function formatDateRange(startDate: string, endDate: string): string {
  const start = new Date(startDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  const end = new Date(endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  return `${start} - ${end}`;
}

function getStatusLabel(trip: Trip): string {
  const now = new Date().toISOString().split('T')[0] ?? '';
  if (trip.start_date && trip.start_date > now) return 'Upcoming';
  if (trip.end_date && trip.end_date < now) return 'Completed';
  return 'Active';
}

function getStatusClass(trip: Trip): string {
  const now = new Date().toISOString().split('T')[0] ?? '';
  if (trip.start_date && trip.start_date > now) return 'status-upcoming';
  if (trip.end_date && trip.end_date < now) return 'status-past';
  return 'status-active';
}

function getTripImage(tripName: string): string {
  // Use Unsplash for random travel images based on trip name
  const seed = tripName.toLowerCase().replace(/\s+/g, '-');
  return `https://source.unsplash.com/400x200/?travel,${seed}`;
}

function getEmptyMessage(): string {
  if (activeFilter.value === 'upcoming') return 'No upcoming trips planned';
  if (activeFilter.value === 'active') return 'No active trips at the moment';
  if (activeFilter.value === 'past') return 'No past trips to show';
  return 'Start planning your first adventure!';
}

// Lifecycle
onMounted(() => {
  void fetchTrips();
});
</script>

<style scoped>
.trip-card {
  border-radius: 16px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.trip-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
}

.trip-image-container {
  position: relative;
  height: 160px;
  overflow: hidden;
}

.trip-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.trip-status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-upcoming {
  background: rgba(33, 150, 243, 0.9);
  color: white;
}

.status-active {
  background: rgba(255, 87, 34, 0.9);
  color: white;
}

.status-past {
  background: rgba(158, 158, 158, 0.9);
  color: white;
}

.ellipsis-2-lines {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  max-height: 2.8em;
}
</style>
