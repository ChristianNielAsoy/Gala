<template>
  <q-page class="profile-page">

    <!-- ═══ Profile hero ═══ -->
    <div class="profile-hero gala-mesh-bg">
      <div class="profile-hero__inner">
        <div class="profile-avatar">
          <span>{{ userProfile.initials || '?' }}</span>
        </div>
        <h1 class="profile-hero__name">{{ userProfile.name || 'Traveler' }}</h1>
        <p class="profile-hero__email">{{ userProfile.email }}</p>

        <!-- Quick stats row -->
        <div class="profile-stats">
          <div class="profile-stat">
            <span class="profile-stat__num">{{ userProfile.trips.length }}</span>
            <span class="profile-stat__label">Trips</span>
          </div>
          <div class="profile-stat-divider" />
          <div class="profile-stat">
            <span class="profile-stat__num">{{ upcomingCount }}</span>
            <span class="profile-stat__label">Upcoming</span>
          </div>
          <div class="profile-stat-divider" />
          <div class="profile-stat">
            <span class="profile-stat__num">{{ userProfile.badges.length }}</span>
            <span class="profile-stat__label">Badges</span>
          </div>
        </div>
      </div>
    </div>

    <div class="profile-body">

      <!-- Achievements -->
      <div v-if="userProfile.badges.length > 0" class="profile-section q-mb-md">
        <div class="profile-section__label">Achievements</div>
        <div class="profile-badges">
          <span
            v-for="badge in userProfile.badges"
            :key="badge"
            class="profile-badge"
          >
            <q-icon name="stars" size="13px" />
            {{ badge }}
          </span>
        </div>
      </div>

      <!-- Trip history -->
      <div class="profile-section">
        <div class="profile-section__label">Trip History</div>
        <div v-if="userProfile.trips.length > 0" class="profile-panel">
          <div
            v-for="(trip, idx) in userProfile.trips"
            :key="trip.id"
            class="profile-trip-row"
            @click="router.push('/trips/' + trip.id)"
          >
            <q-separator v-if="idx > 0" />
            <div class="profile-trip-row__inner">
              <div class="profile-trip-dot" :class="getTripStatusClass(trip)" />
              <div class="col">
                <div class="profile-trip-name">{{ trip.name }}</div>
                <div class="profile-trip-dates">{{ trip.dateRange }}</div>
              </div>
              <span class="profile-trip-status" :class="getTripStatusClass(trip)">
                {{ getTripStatus(trip) }}
              </span>
              <q-icon name="chevron_right" size="18px" color="grey-4" />
            </div>
          </div>
        </div>
        <div v-else class="profile-empty">
          <q-icon name="flight_takeoff" size="40px" color="grey-4" />
          <p class="profile-empty__text">No trips yet. Start planning your first adventure!</p>
          <q-btn
            unelevated no-caps
            color="primary"
            label="Plan a Trip"
            class="profile-empty__cta"
            @click="router.push('/trips')"
          />
        </div>
      </div>

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from 'src/stores/authStore';
import { useTripStore } from 'src/stores/tripStore';

const router = useRouter();
const authStore = useAuthStore();
const tripStore = useTripStore();

interface TripSummary {
  id: string;
  name: string;
  dateRange: string;
  start_date?: string;
  end_date?: string;
}

interface UserProfile {
  name: string;
  email: string;
  initials: string;
  trips: TripSummary[];
  badges: string[];
}

const userProfile = ref<UserProfile>({
  name: '',
  email: '',
  initials: '',
  trips: [],
  badges: [],
});

const upcomingCount = computed(() =>
  userProfile.value.trips.filter(
    (t) => t.start_date && new Date(t.start_date) > new Date(),
  ).length,
);

function getTripStatus(trip: TripSummary): string {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (!trip.start_date || !trip.end_date) return 'Planned';
  const start = new Date(trip.start_date);
  const end = new Date(trip.end_date);
  if (start <= today && end >= today) return 'Active';
  if (start > today) return 'Upcoming';
  return 'Done';
}

function getTripStatusClass(trip: TripSummary): string {
  const status = getTripStatus(trip);
  switch (status) {
    case 'Active': return 'trip-status--active';
    case 'Upcoming': return 'trip-status--upcoming';
    default: return 'trip-status--done';
  }
}

async function fetchUserProfile() {
  const user = authStore.user;
  if (!user) return;

  const email = user.email ?? '';
  userProfile.value.name = user.user_metadata?.full_name || (email ? email.split('@')[0] : '');
  userProfile.value.email = email;
  userProfile.value.initials = userProfile.value.name
    .split(' ')
    .map((n: string) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  await tripStore.fetchTrips();

  const trips: TripSummary[] = tripStore.trips.map((t) => ({
    id: t.id,
    name: t.name,
    dateRange: `${t.start_date ?? ''} – ${t.end_date ?? ''}`,
    start_date: t.start_date,
    end_date: t.end_date,
  }));
  userProfile.value.trips = trips;

  const badges: string[] = [];
  if (trips.length > 0) badges.push('First Trip!');
  if (trips.length > 2) badges.push('Seasoned Traveler');
  if (trips.length > 5) badges.push('Travel Enthusiast');
  if (trips.length > 10) badges.push('Globe Trotter');

  const completedTrips = trips.filter((t) => t.end_date && new Date(t.end_date) < new Date()).length;
  if (completedTrips > 0) badges.push('Trip Completed');
  if (completedTrips > 3) badges.push('Experienced Explorer');

  const upcomingTrips = trips.filter((t) => t.start_date && new Date(t.start_date) > new Date()).length;
  if (upcomingTrips > 0) badges.push('Adventure Awaits');

  if (badges.length === 0) badges.push('New Traveler');
  userProfile.value.badges = badges;
}

onMounted(fetchUserProfile);
</script>

<style scoped lang="scss">
.profile-page {
  min-height: 100vh;
  background: var(--background);
}

// ─── Hero ────────────────────────────────────────────────────────────────────
.profile-hero {
  padding: 48px 24px 40px;
  text-align: center;
}

.profile-hero__inner {
  max-width: 480px;
  margin: 0 auto;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0D9488, #134E4A);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  border: 3px solid rgba(255, 255, 255, 0.3);

  span {
    font-family: 'Instrument Serif', Georgia, serif;
    font-size: 2rem;
    color: #fff;
    line-height: 1;
  }
}

.profile-hero__name {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2rem;
  color: var(--on-background);
  margin: 0 0 4px;
  line-height: 1.1;
}

.profile-hero__email {
  font-size: 0.9rem;
  color: var(--muted);
  margin: 0 0 24px;
}

// ─── Stats ────────────────────────────────────────────────────────────────────
.profile-stats {
  display: inline-flex;
  align-items: center;
  gap: 0;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-pill);
  padding: 10px 24px;
  gap: 24px;
}

.profile-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.profile-stat__num {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--on-background);
}

.profile-stat__label {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--muted);
}

.profile-stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border);
}

// ─── Body ────────────────────────────────────────────────────────────────────
.profile-body {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px 16px 80px;
}

.profile-section {
  margin-bottom: 24px;
}

.profile-section__label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 12px;
}

// ─── Badges ────────────────────────────────────────────────────────────────────
.profile-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: var(--gala-radius-pill);
  background: rgba(13, 148, 136, 0.1);
  border: 1px solid rgba(13, 148, 136, 0.2);
  color: #0D9488;
  font-size: 0.8125rem;
  font-weight: 600;
}

// ─── Trip rows ────────────────────────────────────────────────────────────────
.profile-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

.profile-trip-row {
  cursor: pointer;

  &:hover .profile-trip-row__inner {
    background: var(--background);
  }
}

.profile-trip-row__inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  transition: background 0.15s ease;
}

.profile-trip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.trip-status--active { background: #10B981; }
  &.trip-status--upcoming { background: #0D9488; }
  &.trip-status--done { background: #D1D5DB; }
}

.profile-trip-name {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--on-background);
  margin-bottom: 2px;
}

.profile-trip-dates {
  font-size: 0.8125rem;
  color: var(--muted);
}

.profile-trip-status {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: var(--gala-radius-pill);

  &.trip-status--active {
    background: rgba(16, 185, 129, 0.12);
    color: #059669;
  }
  &.trip-status--upcoming {
    background: rgba(13, 148, 136, 0.12);
    color: #0D9488;
  }
  &.trip-status--done {
    background: rgba(107, 114, 128, 0.1);
    color: var(--muted);
  }
}

// ─── Empty ────────────────────────────────────────────────────────────────────
.profile-empty {
  text-align: center;
  padding: 48px 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  color: var(--muted);
}

.profile-empty__text {
  font-size: 0.9375rem;
  margin: 12px 0 20px;
  color: var(--on-background);
}

.profile-empty__cta {
  border-radius: var(--gala-radius-pill);
  height: 44px;
  padding: 0 28px;
  font-weight: 600;
}
</style>
