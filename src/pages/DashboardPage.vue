<template>
  <q-page class="bg-surface">
    <!-- Stats Dashboard -->
    <div class="q-pa-md">
      <h2 class="text-h6 text-weight-bold q-mb-md">Your Travel Stats</h2>
      <div class="row q-col-gutter-md">
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card text-center q-pa-lg">
            <q-icon name="flight_takeoff" size="40px" color="primary" class="q-mb-sm" />
            <div class="text-h4 text-primary text-weight-bold">{{ stats.totalTrips }}</div>
            <div class="text-caption text-grey-7">Total Trips</div>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card text-center q-pa-lg">
            <q-icon name="event" size="40px" color="accent" class="q-mb-sm" />
            <div class="text-h4 text-accent text-weight-bold">{{ stats.upcomingTrips }}</div>
            <div class="text-caption text-grey-7">Upcoming</div>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card text-center q-pa-lg">
            <q-icon name="explore" size="40px" color="secondary" class="q-mb-sm" />
            <div class="text-h4 text-secondary text-weight-bold">{{ stats.activeTrips }}</div>
            <div class="text-caption text-grey-7">Active</div>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card text-center q-pa-lg">
            <q-icon name="check_circle" size="40px" color="positive" class="q-mb-sm" />
            <div class="text-h4 text-positive text-weight-bold">{{ stats.pastTrips }}</div>
            <div class="text-caption text-grey-7">Completed</div>
          </q-card>
        </div>
      </div>
    </div>

    <!-- Activity Feed -->
    <div class="q-pa-md">
      <h2 class="text-h6 text-weight-bold q-mb-md">Recent Activity</h2>
      <q-card flat bordered>
        <q-card-section>
          <q-timeline color="primary">
            <q-timeline-entry
              v-for="activity in recentActivities"
              :key="activity.id"
              :title="activity.title"
              :subtitle="activity.subtitle"
              :caption="activity.time"
              :icon="activity.icon"
              :color="activity.color"
            >
              <div v-if="activity.actions" class="q-mt-sm">
                <q-btn
                  v-for="action in activity.actions"
                  :key="action.label"
                  flat
                  dense
                  :color="action.color"
                  :label="action.label"
                  @click="action.handler"
                  class="q-mr-sm"
                />
              </div>
            </q-timeline-entry>
          </q-timeline>
        </q-card-section>
      </q-card>
    </div>

    <!-- Quick Actions Hub -->
    <div class="q-pa-md">
      <h2 class="text-h6 text-weight-bold q-mb-md">Quick Actions</h2>
      <div class="row q-col-gutter-sm">
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToTrips"
          >
            <q-icon name="add" size="32px" color="primary" class="q-mb-sm" />
            <div class="text-caption text-weight-medium">New Trip</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToExpenseAnalytics"
          >
            <q-icon name="analytics" size="32px" color="accent" class="q-mb-sm" />
            <div class="text-caption text-weight-medium">Analytics</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToPackingList"
          >
            <q-icon name="checklist" size="32px" color="secondary" class="q-mb-sm" />
            <div class="text-caption text-weight-medium">Packing</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToDocumentsVault"
          >
            <q-icon name="description" size="32px" color="info" class="q-mb-sm" />
            <div class="text-caption text-weight-medium">Documents</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToItineraryTemplates"
          >
            <q-icon name="event_note" size="32px" color="warning" class="q-mb-sm" />
            <div class="text-caption text-weight-medium">Templates</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToUserProfile"
          >
            <q-icon name="person" size="32px" color="positive" class="q-mb-sm" />
            <div class="text-caption text-weight-medium">Profile</div>
          </q-card>
        </div>
      </div>
    </div>

    <!-- Insights Section -->
    <div class="q-pa-md">
      <h2 class="text-h6 text-weight-bold q-mb-md">Travel Insights</h2>
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-card flat bordered class="insight-card q-pa-md">
            <q-card-section>
              <div class="row items-center q-mb-md">
                <q-icon name="trending_up" size="24px" color="positive" class="q-mr-sm" />
                <div class="text-subtitle2 text-weight-bold">Spending Trend</div>
              </div>
              <p class="text-body2 text-grey-7">
                Your average trip expense is 15% higher than last month. Consider setting a budget
                for your next adventure!
              </p>
              <q-btn flat dense color="primary" label="Set Budget" @click="goToSettings" />
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-md-6">
          <q-card flat bordered class="insight-card q-pa-md">
            <q-card-section>
              <div class="row items-center q-mb-md">
                <q-icon name="lightbulb" size="24px" color="accent" class="q-mr-sm" />
                <div class="text-subtitle2 text-weight-bold">Smart Suggestion</div>
              </div>
              <p class="text-body2 text-grey-7">
                Based on your travel history, flights to Southeast Asia are 20% cheaper if booked 2
                months in advance.
              </p>
              <q-btn flat dense color="accent" label="Explore Deals" @click="exploreDeals" />
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>

    <!-- Logout -->
    <div class="q-pa-md text-center">
      <q-btn
        label="Logout"
        @click="handleLogout"
        color="negative"
        flat
        icon="logout"
        class="full-width"
      />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';
import type { User } from '@supabase/supabase-js';

const $q = useQuasar();
const router = useRouter();

const loading = ref(true);
const trips = ref<Trip[]>([]);
const user = ref<User | null>(null);

const stats = ref({
  totalTrips: 0,
  upcomingTrips: 0,
  activeTrips: 0,
  pastTrips: 0,
});

const recentActivities = computed(() => [
  {
    id: 1,
    title: 'Trip to Bali completed',
    subtitle: 'Expenses settled with all members',
    time: '2 days ago',
    icon: 'check_circle',
    color: 'positive',
    actions: [{ label: 'View Details', color: 'primary', handler: () => goToTrips() }],
  },
  {
    id: 2,
    title: 'New expense added',
    subtitle: 'Hotel booking for Tokyo trip',
    time: '1 week ago',
    icon: 'receipt',
    color: 'info',
  },
  {
    id: 3,
    title: 'Packing list updated',
    subtitle: 'Added items for Europe trip',
    time: '2 weeks ago',
    icon: 'checklist',
    color: 'accent',
  },
]);

async function fetchDashboardData(): Promise<void> {
  loading.value = true;
  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) throw new Error('Not authenticated');

    const { data: memberData } = await supabase
      .from('members')
      .select('trip_id')
      .eq('user_id', user.id);

    const tripIds = memberData?.map((m) => m.trip_id) || [];

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
      upcomingTrips: trips.value.filter((t) => t.start_date && t.start_date > now).length,
      activeTrips: trips.value.filter(
        (t) => t.start_date && t.end_date && t.start_date <= now && t.end_date >= now,
      ).length,
      pastTrips: trips.value.filter((t) => t.end_date && t.end_date < now).length,
    };
  } catch (error) {
    console.error('Error fetching dashboard data:', error);
  } finally {
    loading.value = false;
  }
}

function goToTrips(): void {
  void router.push('/trips');
}

function goToPackingList() {
  void router.push('/packing-list');
}

function goToDocumentsVault() {
  void router.push('/documents-vault');
}

function goToExpenseAnalytics() {
  void router.push('/expense-analytics');
}

function goToItineraryTemplates() {
  void router.push('/itinerary-templates');
}

function goToUserProfile() {
  void router.push('/user-profile');
}

function goToSettings() {
  void router.push('/settings');
}

function exploreDeals() {
  // Placeholder for deals functionality
  $q.notify({
    type: 'info',
    message: 'Deals feature coming soon!',
    position: 'top',
  });
}

async function handleLogout(): Promise<void> {
  const { error } = await supabase.auth.signOut();
  if (error) {
    $q.notify({ type: 'negative', message: error.message });
  } else {
    void router.push('/login');
  }
}

onMounted(async () => {
  const {
    data: { user: authUser },
  } = await supabase.auth.getUser();
  user.value = authUser;
  await fetchDashboardData();
});
</script>

<style scoped>
.stat-card {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-card {
  transition: background-color 0.2s ease;
}

.action-card:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.insight-card {
  border-left: 4px solid var(--q-primary);
}
</style>
