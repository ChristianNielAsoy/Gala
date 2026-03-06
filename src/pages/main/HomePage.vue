<template>
  <q-page class="home-page">

    <!-- ═══ Header ═══ -->
    <div class="home-header">
      <div class="home-header__top">
        <div>
          <div class="home-header__eyebrow">Your Trips</div>
          <h1 class="home-header__title">Gala</h1>
        </div>
        <q-btn
          round unelevated
          icon="add"
          color="primary"
          size="md"
          @click="goToTrips"
          aria-label="New trip"
        />
      </div>
    </div>

    <!-- ═══ Trip cards scroll ═══ -->
    <div class="home-trips-scroll">
      <div class="home-trips-track">
        <div
          v-for="trip in recentTrips"
          :key="trip.id"
          class="home-trip-card"
          @click="goToTripDetails(trip.id)"
        >
          <img :src="getTripImage(trip)" class="home-trip-card__img" :alt="trip.name" />
          <div class="home-trip-card__gradient" />

          <span class="home-trip-card__status" :class="`home-trip-card__status--${getStatusKey(trip)}`">
            {{ getStatus(trip) }}
          </span>

          <div class="home-trip-card__info">
            <div class="home-trip-card__name">{{ truncate(trip.name, 16) }}</div>
            <div class="home-trip-card__dates">{{ formatDateShort(trip.start_date, trip.end_date) }}</div>
          </div>
        </div>

        <!-- Add trip card -->
        <div class="home-trip-card home-trip-card--add" @click="goToTrips">
          <q-icon name="add_circle_outline" size="32px" color="grey-4" />
          <span class="home-trip-card--add__label">New Trip</span>
        </div>
      </div>
    </div>

    <div class="home-body">

      <!-- Calendar -->
      <div class="home-section-label">Trip Dates</div>
      <div class="home-panel q-mb-lg">
        <q-date
          v-model="selectedDate"
          :events="tripDates"
          event-color="primary"
          flat minimal
          class="full-width"
        />
      </div>

      <!-- Stats -->
      <div class="home-section-label">Overview</div>
      <div class="home-stats-grid">
        <div class="home-stat-card">
          <div class="home-stat-card__icon home-stat-card__icon--total">
            <q-icon name="flight_takeoff" size="20px" />
          </div>
          <div class="home-stat-card__num">{{ totalTrips }}</div>
          <div class="home-stat-card__label">Total Trips</div>
        </div>

        <div class="home-stat-card">
          <div class="home-stat-card__icon home-stat-card__icon--upcoming">
            <q-icon name="event" size="20px" />
          </div>
          <div class="home-stat-card__num">{{ upcomingTrips }}</div>
          <div class="home-stat-card__label">Upcoming</div>
        </div>

        <div class="home-stat-card">
          <div class="home-stat-card__icon home-stat-card__icon--active">
            <q-icon name="explore" size="20px" />
          </div>
          <div class="home-stat-card__num">{{ activeTrips }}</div>
          <div class="home-stat-card__label">Active Now</div>
        </div>

        <div class="home-stat-card">
          <div class="home-stat-card__icon home-stat-card__icon--done">
            <q-icon name="check_circle" size="20px" />
          </div>
          <div class="home-stat-card__num">{{ completedTrips }}</div>
          <div class="home-stat-card__label">Completed</div>
        </div>
      </div>

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useTripStore } from 'src/stores/tripStore';
import type { Trip } from 'src/types/expense';

const router = useRouter();
const tripStore = useTripStore();

const selectedDate = ref((new Date().toISOString().split('T')[0] ?? '').replace(/-/g, '/'));

const recentTrips = computed(() => tripStore.trips.slice(0, 5));
const totalTrips = computed(() => tripStore.trips.length);

const today = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
};

const upcomingTrips = computed(() =>
  tripStore.trips.filter((t) => new Date(t.start_date) > today()).length,
);

const activeTrips = computed(() =>
  tripStore.trips.filter((t) => {
    const start = new Date(t.start_date);
    const end = new Date(t.end_date);
    const now = today();
    return start <= now && end >= now;
  }).length,
);

const completedTrips = computed(() =>
  tripStore.trips.filter((t) => new Date(t.end_date) < today()).length,
);

const tripDates = computed(() => {
  const dates: string[] = [];
  tripStore.trips.forEach((trip) => {
    const start = new Date(trip.start_date);
    const end = new Date(trip.end_date);
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      const dateStr = d.toISOString().split('T')[0];
      if (dateStr) dates.push(dateStr.replace(/-/g, '/'));
    }
  });
  return dates;
});

function goToTrips() { void router.push('/trips'); }
function goToTripDetails(tripId: string) { void router.push(`/trips/${tripId}`); }

function getTripImage(trip: Trip): string {
  const images = [
    'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&h=300&fit=crop',
  ];
  return images[Math.abs(trip.name.charCodeAt(0) % images.length)]!;
}

function getStatus(trip: Trip): string {
  const now = today();
  const start = new Date(trip.start_date);
  const end = new Date(trip.end_date);
  if (start <= now && end >= now) return 'Active';
  if (start > now) return 'Upcoming';
  return 'Done';
}

function getStatusKey(trip: Trip): string {
  return getStatus(trip).toLowerCase();
}

function formatDateShort(start: string, end: string): string {
  const opts: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  return `${new Date(start).toLocaleDateString('en-US', opts)} – ${new Date(end).toLocaleDateString('en-US', opts)}`;
}

function truncate(text: string, length: number): string {
  return text.length > length ? text.substring(0, length) + '…' : text;
}

onMounted(() => { void tripStore.fetchTrips(); });
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: var(--background);
}

// ─── Header ────────────────────────────────────────────────────────────────────
.home-header {
  padding: 20px 20px 12px;
}

.home-header__top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.home-header__eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 2px;
}

.home-header__title {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2rem;
  color: var(--on-background);
  margin: 0;
  line-height: 1;
}

// ─── Trip scroll ────────────────────────────────────────────────────────────────
.home-trips-scroll {
  padding: 16px 0 20px 20px;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { display: none; }
}

.home-trips-track {
  display: flex;
  gap: 12px;
  width: max-content;
  padding-right: 20px;
}

.home-trip-card {
  position: relative;
  width: 160px;
  height: 120px;
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 0.2s ease;

  &:hover { transform: translateY(-3px); }

  &--add {
    background: var(--surface);
    border: 1.5px dashed var(--border);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: var(--muted);

    &:hover {
      border-color: #0D9488;
      color: #0D9488;
    }
  }
}

.home-trip-card--add__label {
  font-size: 0.8125rem;
  font-weight: 500;
}

.home-trip-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-trip-card__gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.05) 0%, rgba(0,0,0,0.65) 100%);
}

.home-trip-card__status {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: var(--gala-radius-pill);
  text-transform: uppercase;
  letter-spacing: 0.05em;

  &--active {
    background: rgba(16, 185, 129, 0.9);
    color: #fff;
  }
  &--upcoming {
    background: rgba(13, 148, 136, 0.9);
    color: #fff;
  }
  &--done {
    background: rgba(0,0,0,0.45);
    color: rgba(255,255,255,0.75);
  }
}

.home-trip-card__info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  z-index: 1;
}

.home-trip-card__name {
  font-size: 0.875rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.home-trip-card__dates {
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 2px;
}

// ─── Body ────────────────────────────────────────────────────────────────────
.home-body {
  padding: 0 16px 80px;
}

.home-section-label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 12px;
}

// ─── Calendar panel ────────────────────────────────────────────────────────────
.home-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

// ─── Stats grid ────────────────────────────────────────────────────────────────
.home-stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.home-stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.home-stat-card__icon {
  width: 36px;
  height: 36px;
  border-radius: var(--gala-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;

  &--total {
    background: rgba(13, 148, 136, 0.12);
    color: #0D9488;
  }
  &--upcoming {
    background: rgba(124, 58, 237, 0.12);
    color: #7C3AED;
  }
  &--active {
    background: rgba(16, 185, 129, 0.12);
    color: #10B981;
  }
  &--done {
    background: rgba(107, 114, 128, 0.1);
    color: #6B7280;
  }
}

.home-stat-card__num {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2rem;
  line-height: 1;
  color: var(--on-background);
}

.home-stat-card__label {
  font-size: 0.8125rem;
  color: var(--muted);
  font-weight: 500;
}
</style>
