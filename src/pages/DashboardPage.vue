<template>
  <q-page class="q-pa-md">
    <!-- Welcome Header -->
    <div class="q-mb-lg">
      <div class="text-h5 text-weight-bold text-primary">Welcome Back!</div>
      <div class="text-caption text-grey-7">{{ greetingMessage }}</div>
    </div>

    <!-- Quick Stats -->
    <div class="row q-col-gutter-md q-mb-lg">
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="text-center q-pa-md">
          <div class="text-h4 text-primary text-weight-bold">{{ stats.totalTrips }}</div>
          <div class="text-caption text-grey-7">Total Trips</div>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="text-center q-pa-md">
          <div class="text-h4 text-orange text-weight-bold">{{ stats.upcomingTrips }}</div>
          <div class="text-caption text-grey-7">Upcoming</div>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="text-center q-pa-md">
          <div class="text-h4 text-green text-weight-bold">{{ stats.activeTrips }}</div>
          <div class="text-caption text-grey-7">Active</div>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="text-center q-pa-md">
          <div class="text-h4 text-grey-7 text-weight-bold">{{ stats.pastTrips }}</div>
          <div class="text-caption text-grey-7">Completed</div>
        </q-card>
      </div>
    </div>

    <!-- Recent Trips -->
    <q-card flat bordered class="q-mb-lg">
      <q-card-section>
        <div class="row items-center justify-between q-mb-md">
          <div class="text-h6 text-weight-bold">Recent Trips</div>
          <q-btn flat dense color="primary" label="View All" @click="goToTrips" />
        </div>

        <div v-if="loading" class="flex flex-center q-pa-lg">
          <q-spinner color="primary" size="40px" />
        </div>

        <div v-else-if="recentTrips.length === 0" class="text-center q-pa-lg text-grey-6">
          <q-icon name="beach_access" size="48px" class="q-mb-sm" />
          <div>No trips yet. Start planning your adventure!</div>
          <q-btn
            color="primary"
            label="Create Trip"
            icon="add"
            @click="goToTrips"
            class="q-mt-md"
            rounded
            unelevated
          />
        </div>

        <q-list v-else separator>
          <q-item
            v-for="trip in recentTrips"
            :key="trip.id"
            clickable
            @click="goToTripDetails(trip.id)"
            class="q-pa-md"
          >
            <q-item-section avatar>
              <q-avatar rounded size="48px">
                <img :src="getTripImage(trip.name)" />
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-bold">{{ trip.name }}</q-item-label>
              <q-item-label caption>
                {{ formatDateRange(trip.start_date, trip.end_date) }}
              </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-badge
                :color="getStatusColor(trip)"
                :label="getStatusLabel(trip)"
                class="text-capitalize"
              />
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
    </q-card>

    <!-- Quick Actions -->
    <q-card flat bordered class="q-mb-lg">
      <q-card-section>
        <div class="text-h6 text-weight-bold q-mb-md">Quick Actions</div>
        <div class="row q-col-gutter-sm">
          <div class="col-6">
            <q-btn
              flat
              color="primary"
              icon="flight_takeoff"
              label="New Trip"
              @click="goToTrips"
              class="full-width"
              align="left"
            />
          </div>
          <div class="col-6">
            <q-btn
              flat
              color="primary"
              icon="people"
              label="View Members"
              class="full-width"
              align="left"
            />
          </div>
        </div>
      </q-card-section>
    </q-card>

    <!-- Logout -->
    <q-btn
      label="Logout"
      @click="handleLogout"
      color="negative"
      flat
      icon="logout"
      class="full-width"
    />
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';

const $q = useQuasar();
const router = useRouter();

const loading = ref(true);
const trips = ref<Trip[]>([]);

const stats = ref({
  totalTrips: 0,
  upcomingTrips: 0,
  activeTrips: 0,
  pastTrips: 0,
});

const greetingMessage = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning! Ready for an adventure?';
  if (hour < 18) return 'Good afternoon! Plan your next trip';
  return 'Good evening! Dream about your next getaway';
});

const recentTrips = computed(() => {
  return trips.value.slice(0, 5);
});

async function fetchDashboardData(): Promise<void> {
  loading.value = true;
  try {
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Not authenticated');

    const { data: memberData } = await supabase
      .from('members')
      .select('trip_id')
      .eq('user_id', user.id);

    const tripIds = memberData?.map(m => m.trip_id) || [];

    if (tripIds.length === 0) {
      trips.value = [];
      loading.value = false;
      return;
    }

    const { data: tripsData } = await supabase
      .from('trips')
      .select('*')
      .in('id', tripIds)
      .order('start_date', { ascending: false });

    trips.value = (tripsData as Trip[]) || [];

    // Calculate stats
    const now = new Date().toISOString().split('T')[0] ?? '';
    stats.value = {
      totalTrips: trips.value.length,
      upcomingTrips: trips.value.filter(t => t.start_date && t.start_date > now).length,
      activeTrips: trips.value.filter(t => t.start_date && t.end_date && t.start_date <= now && t.end_date >= now).length,
      pastTrips: trips.value.filter(t => t.end_date && t.end_date < now).length,
    };

  } catch (error) {
    console.error('Error fetching dashboard data:', error);
  } finally {
    loading.value = false;
  }
}

function getTripImage(tripName: string): string {
  const seed = tripName.toLowerCase().replace(/\s+/g, '-');
  return `https://source.unsplash.com/100x100/?travel,${seed}`;
}

function formatDateRange(startDate: string, endDate: string): string {
  const start = new Date(startDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  const end = new Date(endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  return `${start} - ${end}`;
}

function getStatusLabel(trip: Trip): string {
  const now = new Date().toISOString().split('T')[0] ?? '';
  if (trip.start_date && trip.start_date > now) return 'upcoming';
  if (trip.end_date && trip.end_date < now) return 'completed';
  return 'active';
}

function getStatusColor(trip: Trip): string {
  const now = new Date().toISOString().split('T')[0] ?? '';
  if (trip.start_date && trip.start_date > now) return 'blue';
  if (trip.end_date && trip.end_date < now) return 'grey';
  return 'orange';
}

function goToTrips(): void {
  void router.push('/trips');
}

function goToTripDetails(tripId: string): void {
  void router.push(`/trips/${tripId}`);
}

async function handleLogout(): Promise<void> {
  const { error } = await supabase.auth.signOut();
  if (error) {
    $q.notify({ type: 'negative', message: error.message });
  } else {
    void router.push('/login');
  }
}

onMounted(() => {
  void fetchDashboardData();
});
</script>
