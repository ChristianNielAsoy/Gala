<template>
  <q-page class="dashboard-page gala-mesh-bg">

    <!-- ═══ Greeting ═══ -->
    <div class="greeting-section q-px-lg q-pt-lg q-pb-sm">
      <p class="greeting-eyebrow text-caption text-weight-bold q-mb-xs">
        {{ timeGreeting }}
      </p>
      <h1 class="gala-display greeting-name q-mb-none">
        {{ displayName || 'Traveler' }}
      </h1>
    </div>

    <!-- ═══ Active Trip Hero Card ═══ -->
    <div class="q-px-lg q-pb-md" v-if="nearestTrip">
      <div class="hero-trip-card" @click="goToActiveTrip">
        <!-- Cover image -->
        <div class="hero-trip-card__image-wrap">
          <img :src="nearestTripImage" :alt="nearestTrip.name" class="hero-trip-card__image" />
          <div class="hero-trip-card__overlay" />
        </div>

        <!-- Glassmorphism content panel -->
        <div class="hero-trip-card__panel">
          <div class="row items-start justify-between no-wrap">
            <div class="col">
              <div class="status-dot-row q-mb-xs">
                <span class="status-dot" :class="nearestTrip.status === 'active' ? 'status-dot--active' : 'status-dot--upcoming'" />
                <span class="text-caption text-weight-bold" style="color: rgba(255,255,255,0.7); text-transform: uppercase; letter-spacing: 0.08em">
                  {{ nearestTrip.status === 'active' ? 'Active' : 'Upcoming' }}
                </span>
              </div>
              <div class="gala-display hero-trip-card__name">{{ nearestTrip.name }}</div>
              <div class="row items-center q-gutter-x-sm q-mt-xs">
                <span v-if="nearestTrip.destination" class="hero-meta">
                  <q-icon name="place" size="11px" />{{ nearestTrip.destination }}
                </span>
                <span class="hero-meta">
                  <q-icon name="calendar_today" size="11px" />{{ formatDateRange(nearestTrip.start_date, nearestTrip.end_date) }}
                </span>
              </div>
            </div>
            <!-- Budget arc -->
            <div class="budget-arc" v-if="nearestTrip.budget_amount">
              <svg width="64" height="64" viewBox="0 0 64 64">
                <circle cx="32" cy="32" r="24" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="4" />
                <circle
                  cx="32" cy="32" r="24"
                  fill="none"
                  :stroke="budgetArcColor"
                  stroke-width="4"
                  stroke-linecap="round"
                  :stroke-dasharray="`${budgetPercent * 150.8} 150.8`"
                  transform="rotate(-90 32 32)"
                />
              </svg>
              <div class="budget-arc__label">
                <span class="budget-arc__pct">{{ Math.round(budgetPercent * 100) }}%</span>
              </div>
            </div>
          </div>

          <!-- Budget bar -->
          <div class="q-mt-sm" v-if="!activeTripLoading">
            <div class="budget-track">
              <div
                class="budget-fill"
                :style="{ width: `${Math.min(budgetPercent * 100, 100)}%`, background: budgetFillGradient }"
              />
            </div>
            <div class="row items-center justify-between q-mt-xs">
              <span class="budget-text">
                <span class="budget-text--spent">{{ formatAmount(activeTripSpent, nearestTrip.currency_code) }}</span>
                <span class="budget-text--sep"> of </span>
                <span class="budget-text--total">
                  {{ nearestTrip.budget_amount ? formatAmount(nearestTrip.budget_amount, nearestTrip.currency_code) : 'no budget' }}
                </span>
              </span>
              <span class="budget-text--action">
                View Trip <q-icon name="arrow_forward" size="12px" />
              </span>
            </div>
          </div>
          <div class="q-mt-sm" v-else>
            <q-skeleton dark height="6px" class="q-mb-xs" style="border-radius: 3px" />
            <q-skeleton dark type="text" width="40%" />
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ Stats Row ═══ -->
    <div class="q-px-lg q-pb-md">
      <div class="stats-grid">
        <div
          v-for="stat in statItems"
          :key="stat.label"
          class="stat-tile"
          @click="stat.action && stat.action()"
          :class="{ 'stat-tile--clickable': stat.action }"
        >
          <div class="stat-tile__icon" :style="{ background: stat.iconBg }">
            <q-icon :name="stat.icon" :color="stat.iconColor" size="18px" />
          </div>
          <div class="gala-display stat-tile__number">{{ stat.value }}</div>
          <div class="stat-tile__label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- ═══ Quick Actions ═══ -->
    <div class="q-px-lg q-pb-md">
      <div class="section-heading q-mb-sm">Quick Actions</div>
      <div class="quick-grid">
        <div
          v-for="action in quickActions"
          :key="action.label"
          class="quick-tile"
          @click="action.fn()"
        >
          <div class="quick-tile__icon" :style="{ background: action.bg }">
            <q-icon :name="action.icon" color="white" size="22px" />
          </div>
          <span class="quick-tile__label">{{ action.label }}</span>
        </div>
      </div>
    </div>

    <!-- ═══ Recent Activity ═══ -->
    <div class="q-px-lg q-pb-xl">
      <div class="section-heading q-mb-sm">Recent Activity</div>

      <div v-if="loadingActivity" class="text-center q-py-lg">
        <q-spinner color="primary" size="28px" />
      </div>

      <div v-else-if="recentActivities.length === 0" class="activity-empty q-pa-lg text-center">
        <q-icon name="history" size="40px" style="color: var(--border)" />
        <div class="text-caption q-mt-sm" style="color: var(--muted)">No activity yet. Start planning a trip!</div>
      </div>

      <div v-else class="activity-list">
        <div
          v-for="(activity, idx) in recentActivities"
          :key="activity.id"
          class="activity-item"
        >
          <div class="activity-item__icon" :style="{ background: activityIconBg(activity.color) }">
            <q-icon :name="activity.icon" :color="activity.color" size="16px" />
          </div>
          <div class="activity-item__body col">
            <div class="activity-item__title">{{ activity.title }}</div>
            <div class="activity-item__sub">{{ activity.subtitle }}</div>
          </div>
          <div class="activity-item__time">{{ activity.time }}</div>
          <!-- Connector line -->
          <div v-if="idx < recentActivities.length - 1" class="activity-item__line" />
        </div>
      </div>
    </div>

    <!-- ═══ Trip Picker Dialog ═══ -->
    <q-dialog v-model="showTripPicker">
      <q-card style="min-width: 300px; border-radius: var(--gala-radius-xl)">
        <q-card-section>
          <div class="text-h6 text-weight-bold">Which trip?</div>
          <div class="text-caption" style="color: var(--muted)">Select a trip to log this expense under</div>
        </q-card-section>
        <q-list separator>
          <q-item
            v-for="trip in activeTrips"
            :key="trip.id"
            clickable v-ripple
            @click="pickTripForExpense(trip.id)"
          >
            <q-item-section avatar>
              <q-icon name="luggage" color="primary" />
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ trip.name }}</q-item-label>
              <q-item-label caption>{{ formatDateRange(trip.start_date, trip.end_date) }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-icon name="chevron_right" color="grey-5" />
            </q-item-section>
          </q-item>
        </q-list>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat no-caps label="Cancel" style="color: var(--muted)" v-close-popup />
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
import { useAuthStore } from 'src/stores/authStore';
import { useTripStore } from 'src/stores/tripStore';
import type { ActivityActionType, Trip } from 'src/types/expense';

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
const activeTripSpent = ref(0);
const activeTripLoading = ref(false);
const showTripPicker = ref(false);

// ─── Greeting ────────────────────────────────────────────────────────────────
const displayName = computed(() => (authStore.userName || '').split(' ')[0] || '');

const timeGreeting = computed(() => {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
});

// ─── Stats ───────────────────────────────────────────────────────────────────
const now = new Date().toISOString().split('T')[0] ?? '';

const stats = computed(() => ({
  totalTrips:    tripStore.trips.length,
  upcomingTrips: tripStore.trips.filter((t) => t.start_date && t.start_date > now).length,
  activeTrips:   tripStore.trips.filter((t) => t.start_date && t.end_date && t.start_date <= now && t.end_date >= now).length,
  pastTrips:     tripStore.trips.filter((t) => t.end_date && t.end_date < now).length,
}));

const statItems = computed(() => [
  {
    label: 'Total',
    value: stats.value.totalTrips,
    icon: 'flight_takeoff',
    iconColor: 'primary',
    iconBg: 'rgba(13, 148, 136, 0.1)',
    action: () => void router.push('/trips'),
  },
  {
    label: 'Upcoming',
    value: stats.value.upcomingTrips,
    icon: 'event',
    iconColor: 'warning',
    iconBg: 'rgba(245, 158, 11, 0.1)',
    action: null,
  },
  {
    label: 'Active',
    value: stats.value.activeTrips,
    icon: 'explore',
    iconColor: 'positive',
    iconBg: 'rgba(16, 185, 129, 0.1)',
    action: null,
  },
  {
    label: 'Done',
    value: stats.value.pastTrips,
    icon: 'check_circle',
    iconColor: 'secondary',
    iconBg: 'rgba(100, 116, 139, 0.1)',
    action: null,
  },
]);

// ─── Nearest Trip ────────────────────────────────────────────────────────────
const nearestTrip = computed<Trip | null>(() => {
  const active = tripStore.trips.find((t) => t.status === 'active');
  if (active) return active;
  const today = new Date().toISOString().split('T')[0] ?? '';
  return tripStore.trips.find((t) => t.status === 'planning' && !!t.start_date && t.start_date >= today) ?? null;
});

const activeTrips = computed(() => tripStore.trips.filter((t) => t.status === 'active'));

const nearestTripImages = [
  'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=400&fit=crop',
  'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=400&fit=crop',
  'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&h=400&fit=crop',
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=400&fit=crop',
];

const nearestTripImage = computed(() => {
  const trip = nearestTrip.value;
  if (!trip) return nearestTripImages[0]!;
  return nearestTripImages[Math.abs(trip.name.charCodeAt(0) % nearestTripImages.length)]!;
});

// ─── Budget ──────────────────────────────────────────────────────────────────
const budgetPercent = computed(() => {
  const trip = nearestTrip.value;
  if (!trip?.budget_amount) return 0;
  return Math.min(activeTripSpent.value / trip.budget_amount, 1);
});

const budgetArcColor = computed(() => {
  const p = budgetPercent.value;
  if (p >= 1) return '#EF4444';
  if (p >= 0.75) return '#F59E0B';
  return 'white';
});

const budgetFillGradient = computed(() => {
  const p = budgetPercent.value;
  if (p >= 1)    return 'linear-gradient(90deg, #EF4444, #DC2626)';
  if (p >= 0.75) return 'linear-gradient(90deg, #F59E0B, #D97706)';
  return 'linear-gradient(90deg, rgba(255,255,255,0.5), rgba(255,255,255,0.9))';
});

// ─── Quick actions ────────────────────────────────────────────────────────────
const quickActions = [
  { label: 'New Trip', icon: 'flight_takeoff', bg: 'linear-gradient(135deg, #0D9488, #134E4A)', fn: () => void router.push('/trips') },
  { label: 'Analytics', icon: 'analytics', bg: 'linear-gradient(135deg, #F59E0B, #D97706)', fn: () => void router.push('/expense-analytics') },
  { label: 'Packing', icon: 'checklist', bg: 'linear-gradient(135deg, #10B981, #059669)', fn: () => void router.push('/packing-list') },
  { label: 'Documents', icon: 'description', bg: 'linear-gradient(135deg, #3B82F6, #6366F1)', fn: () => void router.push('/documents-vault') },
  { label: 'Profile', icon: 'person', bg: 'linear-gradient(135deg, #FB7185, #E11D48)', fn: () => void router.push('/user-profile') },
];

// ─── Activity ────────────────────────────────────────────────────────────────
function activityIconBg(color: string): string {
  const map: Record<string, string> = {
    primary:  'rgba(13, 148, 136, 0.1)',
    positive: 'rgba(16, 185, 129, 0.1)',
    accent:   'rgba(245, 158, 11, 0.1)',
    warning:  'rgba(245, 158, 11, 0.1)',
    info:     'rgba(59, 130, 246, 0.1)',
    negative: 'rgba(239, 68, 68, 0.1)',
  };
  return map[color] ?? 'rgba(13, 148, 136, 0.1)';
}

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
      .limit(8);

    recentActivities.value = (data || []).map((entry) => {
      const tripName = Array.isArray(entry.trips)
        ? (entry.trips[0] as { name?: string } | undefined)?.name
        : (entry.trips as { name?: string } | null)?.name;
      return {
        id: entry.id as string,
        title: entry.description as string,
        subtitle: tripName ? tripName : '',
        time: formatRelativeTime(entry.created_at as string),
        icon: getActivityIcon(entry.action_type as ActivityActionType),
        color: getActivityColor(entry.action_type as ActivityActionType),
      };
    });
  } finally {
    loadingActivity.value = false;
  }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function formatDateRange(start: string, end: string): string {
  const opts: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  const s = new Date(start + 'T00:00:00').toLocaleDateString('en-PH', opts);
  const e = new Date(end + 'T00:00:00').toLocaleDateString('en-PH', { ...opts, year: 'numeric' });
  return `${s} – ${e}`;
}

function formatAmount(amount: number, currencyCode: string): string {
  const symbols: Record<string, string> = {
    PHP: '₱', USD: '$', EUR: '€', JPY: '¥', GBP: '£',
    AUD: 'A$', CAD: 'C$', SGD: 'S$', THB: '฿',
  };
  const sym = symbols[currencyCode] ?? currencyCode + ' ';
  return `${sym}${amount.toLocaleString('en-PH', { maximumFractionDigits: 0 })}`;
}

async function fetchActiveTripSpent(tripId: string): Promise<void> {
  activeTripLoading.value = true;
  try {
    const { data } = await supabase.from('expenses').select('amount').eq('trip_id', tripId);
    activeTripSpent.value = (data ?? []).reduce((sum, e) => sum + ((e.amount as number) ?? 0), 0);
  } finally {
    activeTripLoading.value = false;
  }
}

function goToActiveTrip(): void {
  if (nearestTrip.value) void router.push(`/trips/${nearestTrip.value.id}`);
}

function pickTripForExpense(tripId: string): void {
  showTripPicker.value = false;
  void router.push(`/trips/${tripId}/expenses/new`);
}

onMounted(async () => {
  try {
    await tripStore.fetchTrips();
    if (nearestTrip.value) {
      await fetchActiveTripSpent(nearestTrip.value.id);
    }
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Failed to load dashboard', position: 'top' });
  }
  await fetchRecentActivities();
});
</script>

<style scoped lang="scss">
// ─── Page ────────────────────────────────────────────────────────────────────
.dashboard-page {
  min-height: 100vh;
  background-color: var(--surface);
}

// ─── Greeting ────────────────────────────────────────────────────────────────
.greeting-eyebrow {
  color: $primary;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.greeting-name {
  font-size: clamp(2.25rem, 8vw, 3.5rem);
  line-height: 1.05;
  color: var(--on-background);
}

// ─── Hero Trip Card ──────────────────────────────────────────────────────────
.hero-trip-card {
  border-radius: var(--gala-radius-xl);
  overflow: hidden;
  cursor: pointer;
  position: relative;
  box-shadow: var(--gala-elevation-2);
  transition: transform var(--duration-normal) var(--ease-spring),
              box-shadow var(--duration-normal) var(--ease-out-expo);

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--gala-elevation-float);

    .hero-trip-card__image {
      transform: scale(1.04);
    }
  }

  &__image-wrap {
    position: relative;
    height: clamp(160px, 42vw, 240px);
    overflow: hidden;
  }

  &__image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform var(--duration-slow) var(--ease-out-expo);
  }

  &__overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(to bottom, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.7) 100%);
  }

  &__panel {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
  }

  &__name {
    font-size: clamp(1.25rem, 4.5vw, 1.75rem);
    color: white;
    line-height: 1.1;
    text-shadow: 0 1px 6px rgba(0,0,0,0.2);
  }
}

.status-dot-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;

  &--active   { background: $positive; box-shadow: 0 0 6px rgba($positive, 0.8); }
  &--upcoming { background: $accent; }
}

.hero-meta {
  font-size: 0.78rem;
  color: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  gap: 3px;
}

// Budget arc
.budget-arc {
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;

  svg { position: absolute; inset: 0; }

  &__label {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__pct {
    font-size: 0.6875rem;
    font-weight: 700;
    color: white;
    font-variant-numeric: tabular-nums;
  }
}

// Budget bar
.budget-track {
  height: 4px;
  border-radius: 2px;
  background: rgba(255,255,255,0.2);
  overflow: hidden;
}

.budget-fill {
  height: 100%;
  border-radius: 2px;
  transition: width var(--duration-slow) var(--ease-out-expo);
}

.budget-text {
  font-size: 0.78rem;

  &--spent { color: white; font-weight: 700; font-variant-numeric: tabular-nums; }
  &--sep   { color: rgba(255,255,255,0.5); }
  &--total { color: rgba(255,255,255,0.65); font-variant-numeric: tabular-nums; }
  &--action {
    color: rgba(255,255,255,0.8);
    font-size: 0.75rem;
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 3px;
  }
}

// ─── Stats Grid ──────────────────────────────────────────────────────────────
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.stat-tile {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  transition: transform var(--duration-fast) var(--ease-spring),
              box-shadow var(--duration-fast) var(--ease-out-expo);

  &--clickable {
    cursor: pointer;
    &:hover {
      transform: translateY(-3px);
      box-shadow: var(--gala-elevation-2);
    }
  }

  &__icon {
    width: 34px;
    height: 34px;
    border-radius: var(--gala-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__number {
    font-size: 1.75rem;
    line-height: 1;
    color: var(--on-background);
  }

  &__label {
    font-size: 0.72rem;
    font-weight: 600;
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
}

// ─── Quick Actions ────────────────────────────────────────────────────────────
.section-heading {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--muted);
}

.quick-grid {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 4px;

  &::-webkit-scrollbar { display: none; }
}

.quick-tile {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;

  &:active .quick-tile__icon {
    transform: scale(0.92);
  }

  &__icon {
    width: 54px;
    height: 54px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform var(--duration-fast) var(--ease-spring);
  }

  &__label {
    font-size: 0.7rem;
    font-weight: 600;
    color: var(--on-surface);
    white-space: nowrap;
  }
}

// ─── Activity ────────────────────────────────────────────────────────────────
.activity-empty {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
}

.activity-list {
  display: flex;
  flex-direction: column;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  padding: 10px 0;

  &__icon {
    width: 34px;
    height: 34px;
    border-radius: var(--gala-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-top: 1px;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 0.8375rem;
    font-weight: 500;
    color: var(--on-background);
    line-height: 1.3;
  }

  &__sub {
    font-size: 0.75rem;
    color: var(--muted);
    margin-top: 1px;
  }

  &__time {
    font-size: 0.72rem;
    font-weight: 500;
    color: var(--muted);
    flex-shrink: 0;
    padding-top: 2px;
    font-variant-numeric: tabular-nums;
  }

  &__line {
    position: absolute;
    left: 17px;
    top: 44px;
    bottom: 0;
    width: 1px;
    background: var(--border);
  }
}
</style>
