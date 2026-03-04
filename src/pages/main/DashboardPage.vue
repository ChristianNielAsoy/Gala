<template>
  <q-page class="bg-surface">
    <!-- Welcome Header -->
    <div class="q-px-lg q-pt-lg q-pb-sm">
      <div class="text-h5 text-weight-bold" style="color: #1e293b">
        Welcome back{{ displayName ? ', ' + displayName : '' }}
      </div>
      <div class="text-body2 text-grey-6 q-mt-xs">Here's an overview of your trips</div>
    </div>

    <!-- Stats -->
    <div class="q-pa-md q-pt-sm">
      <div class="row q-col-gutter-md">
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card q-pa-md">
            <div class="row items-center no-wrap">
              <div class="stat-icon-wrap stat-icon-wrap--primary q-mr-md">
                <q-icon name="flight_takeoff" size="22px" color="primary" />
              </div>
              <div>
                <div class="text-h4 text-weight-bold stat-number">{{ stats.totalTrips }}</div>
                <div class="text-caption text-grey-6">Total Trips</div>
              </div>
            </div>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card q-pa-md">
            <div class="row items-center no-wrap">
              <div class="stat-icon-wrap stat-icon-wrap--accent q-mr-md">
                <q-icon name="event" size="22px" color="accent" />
              </div>
              <div>
                <div class="text-h4 text-weight-bold stat-number">{{ stats.upcomingTrips }}</div>
                <div class="text-caption text-grey-6">Upcoming</div>
              </div>
            </div>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card q-pa-md">
            <div class="row items-center no-wrap">
              <div class="stat-icon-wrap stat-icon-wrap--positive q-mr-md">
                <q-icon name="explore" size="22px" color="positive" />
              </div>
              <div>
                <div class="text-h4 text-weight-bold stat-number">{{ stats.activeTrips }}</div>
                <div class="text-caption text-grey-6">Active</div>
              </div>
            </div>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered class="stat-card q-pa-md">
            <div class="row items-center no-wrap">
              <div class="stat-icon-wrap stat-icon-wrap--secondary q-mr-md">
                <q-icon name="check_circle" size="22px" color="secondary" />
              </div>
              <div>
                <div class="text-h4 text-weight-bold stat-number">{{ stats.pastTrips }}</div>
                <div class="text-caption text-grey-6">Completed</div>
              </div>
            </div>
          </q-card>
        </div>
      </div>
    </div>

    <!-- Activity Feed -->
    <div class="q-pa-md">
      <h2 class="text-h6 text-weight-bold q-mb-md">Recent Activity</h2>
      <q-card flat bordered>
        <q-card-section v-if="loadingActivity" class="text-center q-py-lg">
          <q-spinner color="primary" />
        </q-card-section>
        <q-card-section v-else-if="recentActivities.length === 0" class="text-center q-py-lg">
          <q-icon name="history" size="40px" color="grey-4" />
          <div class="text-grey-6 q-mt-sm">No activity yet. Start planning a trip!</div>
        </q-card-section>
        <q-card-section v-else>
          <q-timeline color="primary">
            <q-timeline-entry
              v-for="activity in recentActivities"
              :key="activity.id"
              :title="activity.title"
              :subtitle="activity.subtitle"
              :caption="activity.time"
              :icon="activity.icon"
              :color="activity.color"
            />
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
            <q-icon name="add" size="28px" color="primary" class="q-mb-xs" />
            <div class="text-body2 text-weight-medium">New Trip</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToExpenseAnalytics"
          >
            <q-icon name="analytics" size="28px" color="accent" class="q-mb-xs" />
            <div class="text-body2 text-weight-medium">Analytics</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToPackingList"
          >
            <q-icon name="checklist" size="28px" color="secondary" class="q-mb-xs" />
            <div class="text-body2 text-weight-medium">Packing</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToDocumentsVault"
          >
            <q-icon name="description" size="28px" color="info" class="q-mb-xs" />
            <div class="text-body2 text-weight-medium">Documents</div>
          </q-card>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <q-card
            flat
            bordered
            class="action-card text-center q-pa-md cursor-pointer"
            @click="goToUserProfile"
          >
            <q-icon name="person" size="28px" color="positive" class="q-mb-xs" />
            <div class="text-body2 text-weight-medium">Profile</div>
          </q-card>
        </div>
      </div>
    </div>

    <!-- Insights Section -->
    <div class="q-pa-md" v-if="!tripStore.isLoading">
      <h2 class="text-h6 text-weight-bold q-mb-md">Travel Insights</h2>
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-card flat bordered class="insight-card q-pa-md">
            <q-card-section>
              <div class="row items-center q-mb-md">
                <q-icon name="trending_up" size="24px" color="positive" class="q-mr-sm" />
                <div class="text-subtitle2 text-weight-bold">Trip Overview</div>
              </div>
              <p class="text-body2 text-grey-7" v-if="stats.upcomingTrips > 0">
                You have <strong>{{ stats.upcomingTrips }} upcoming {{ stats.upcomingTrips === 1 ? 'trip' : 'trips' }}</strong> on the calendar.
                <span v-if="stats.activeTrips > 0"> {{ stats.activeTrips }} {{ stats.activeTrips === 1 ? 'trip is' : 'trips are' }} currently active!</span>
              </p>
              <p class="text-body2 text-grey-7" v-else-if="stats.pastTrips > 0">
                You've completed <strong>{{ stats.pastTrips }} {{ stats.pastTrips === 1 ? 'trip' : 'trips' }}</strong> so far. Time to plan the next one!
              </p>
              <p class="text-body2 text-grey-7" v-else>
                No trips yet. Start planning your first barkada adventure!
              </p>
              <q-btn flat dense color="primary" no-caps label="View Trips" @click="goToTrips" />
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-md-6">
          <q-card flat bordered class="insight-card q-pa-md">
            <q-card-section>
              <div class="row items-center q-mb-md">
                <q-icon name="analytics" size="24px" color="accent" class="q-mr-sm" />
                <div class="text-subtitle2 text-weight-bold">Expense Tracking</div>
              </div>
              <p class="text-body2 text-grey-7" v-if="stats.totalTrips > 0">
                Tracking expenses across <strong>{{ stats.totalTrips }} {{ stats.totalTrips === 1 ? 'trip' : 'trips' }}</strong>.
                Use the analytics page to see spending breakdowns and settle up with your barkada.
              </p>
              <p class="text-body2 text-grey-7" v-else>
                Once you create a trip, you can track shared expenses and split costs with your group.
              </p>
              <q-btn flat dense no-caps color="accent" label="View Analytics" @click="goToExpenseAnalytics" />
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
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import { useTripStore } from 'src/stores/tripStore';
import type { ActivityActionType } from 'src/types/expense';

interface ActivityItem {
  id: string;
  title: string;
  subtitle: string;
  time: string;
  icon: string;
  color: string;
}

const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();
const tripStore = useTripStore();

const loadingActivity = ref(false);
const recentActivities = ref<ActivityItem[]>([]);

// Derived from store — no local trip state needed
const displayName = computed(() => {
  const name = authStore.userName;
  return name.split(' ')[0] || '';
});

const stats = computed(() => {
  const now = new Date().toISOString().split('T')[0] ?? '';
  return {
    totalTrips: tripStore.trips.length,
    upcomingTrips: tripStore.trips.filter((t) => t.start_date && t.start_date > now).length,
    activeTrips: tripStore.trips.filter(
      (t) => t.start_date && t.end_date && t.start_date <= now && t.end_date >= now,
    ).length,
    pastTrips: tripStore.trips.filter((t) => t.end_date && t.end_date < now).length,
  };
});

function getActivityIcon(actionType: ActivityActionType): string {
  if (actionType.startsWith('expense')) return 'receipt';
  if (actionType.startsWith('settlement')) return 'payments';
  if (actionType.startsWith('member')) return 'person';
  if (actionType.startsWith('itinerary')) return 'event_note';
  if (actionType === 'trip_completed') return 'check_circle';
  return 'luggage';
}

function getActivityColor(actionType: ActivityActionType): string {
  if (actionType.startsWith('expense')) return 'info';
  if (actionType.startsWith('settlement')) return 'positive';
  if (actionType.startsWith('member')) return 'accent';
  if (actionType.startsWith('itinerary')) return 'warning';
  return 'primary';
}

function formatRelativeTime(isoDate: string): string {
  const diff = Date.now() - new Date(isoDate).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(isoDate).toLocaleDateString('en-PH', { month: 'short', day: 'numeric' });
}

async function fetchRecentActivities(): Promise<void> {
  const ids = tripStore.tripIds;
  if (ids.length === 0) return;
  loadingActivity.value = true;
  try {
    const { data } = await supabase
      .from('activity_log')
      .select('id, action_type, description, created_at, trips(name)')
      .in('trip_id', ids)
      .order('created_at', { ascending: false })
      .limit(10);

    recentActivities.value = (data || []).map((entry) => {
      const tripName = Array.isArray(entry.trips)
        ? (entry.trips[0] as { name?: string } | undefined)?.name
        : (entry.trips as { name?: string } | null)?.name;
      return {
        id: entry.id as string,
        title: entry.description as string,
        subtitle: tripName ? `Trip: ${tripName}` : '',
        time: formatRelativeTime(entry.created_at as string),
        icon: getActivityIcon(entry.action_type as ActivityActionType),
        color: getActivityColor(entry.action_type as ActivityActionType),
      };
    });
  } finally {
    loadingActivity.value = false;
  }
}

function goToTrips(): void { void router.push('/trips'); }
function goToPackingList() { void router.push('/packing-list'); }
function goToDocumentsVault() { void router.push('/documents-vault'); }
function goToExpenseAnalytics() { void router.push('/expense-analytics'); }
function goToUserProfile() { void router.push('/user-profile'); }

onMounted(async () => {
  try {
    await tripStore.fetchTrips();
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Failed to load dashboard data';
    $q.notify({ type: 'negative', message: msg, position: 'top' });
  }
  await fetchRecentActivities();
});
</script>

<style scoped lang="scss">
.stat-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--gala-shadow-md);
  }
}

.stat-number {
  color: #1e293b;
  line-height: 1;
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--gala-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--primary  { background: rgba(13, 148, 136, 0.1); }
  &--accent   { background: rgba(249, 115, 22, 0.1); }
  &--positive { background: rgba(22, 163, 74, 0.1); }
  &--secondary { background: rgba(100, 116, 139, 0.1); }
}

.action-card {
  transition: background-color 0.15s ease, transform 0.15s ease;

  &:hover {
    background-color: rgba(13, 148, 136, 0.04);
    transform: translateY(-1px);
  }
}

.insight-card {
  border-left: 3px solid $primary;
}
</style>
