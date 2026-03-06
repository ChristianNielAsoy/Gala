<template>
  <q-page class="trips-page gala-mesh-bg">
    <q-pull-to-refresh @refresh="onRefresh" color="primary">

      <!-- ═══ Page Header ═══ -->
      <div class="page-header q-px-lg q-pt-lg q-pb-md">
        <div class="row items-end justify-between">
          <div>
            <p class="text-caption text-weight-medium q-mb-xs header-eyebrow">Your Adventures</p>
            <h1 class="gala-display page-title q-mb-none">Barkada<br>Trips</h1>
          </div>
          <q-btn
            unelevated
            round
            color="primary"
            icon="add"
            size="md"
            @click="openNewTripModal"
            aria-label="Plan New Trip"
            class="fab-add"
          />
        </div>
      </div>

      <!-- ═══ Search + Filter ═══ -->
      <div class="q-px-lg q-mb-md">
        <q-input
          v-model="searchQuery"
          outlined
          dense
          placeholder="Search trips..."
          class="search-input q-mb-sm"
          bg-color="background"
        >
          <template v-slot:prepend>
            <q-icon name="search" size="18px" style="color: var(--muted)" />
          </template>
          <template v-slot:append v-if="searchQuery">
            <q-icon name="close" size="16px" class="cursor-pointer" @click="searchQuery = ''" style="color: var(--muted)" />
          </template>
        </q-input>

        <!-- Pill filter tabs -->
        <div class="filter-pills row no-wrap q-gutter-xs">
          <div
            v-for="f in filters"
            :key="f.value"
            class="filter-pill"
            :class="{ 'filter-pill--active': activeFilter === f.value }"
            @click="activeFilter = f.value"
          >
            {{ f.label }}
            <span v-if="filterCount(f.value)" class="filter-pill__count">{{ filterCount(f.value) }}</span>
          </div>
        </div>
      </div>

      <!-- ═══ Loading Skeletons ═══ -->
      <div v-if="tripStore.isLoading" class="q-px-lg q-pb-md">
        <div v-for="n in 3" :key="n" class="skeleton-card q-mb-lg">
          <q-skeleton height="220px" class="skeleton-img" />
          <div class="q-pa-md">
            <q-skeleton type="text" width="55%" class="q-mb-xs" />
            <q-skeleton type="text" width="35%" />
          </div>
        </div>
      </div>

      <!-- ═══ Empty State ═══ -->
      <empty-state
        v-else-if="!tripStore.isLoading && filteredTrips.length === 0"
        icon="flight_takeoff"
        title="No trips found"
        subtitle="Start your journey by planning your first barkada trip!"
        action-label="Create Your First Trip"
        @action="openNewTripModal"
        class="q-mt-xl"
      />

      <!-- ═══ Active / Upcoming Trips ═══ -->
      <template v-else>
        <div v-if="activeUpcomingTrips.length > 0" class="q-px-lg q-mb-xl">
          <div class="section-label q-mb-md">
            <span class="text-caption text-weight-bold" style="color: var(--muted); letter-spacing: 0.08em; text-transform: uppercase">
              {{ activeUpcomingTrips.length === 1 ? 'Next Up' : 'Active & Upcoming' }}
            </span>
          </div>

          <div
            v-for="trip in activeUpcomingTrips"
            :key="trip.id"
            class="immersive-card q-mb-lg"
            @click="goToTripDetails(trip.id)"
          >
            <!-- Cover image with overlaid content -->
            <div class="immersive-card__image-wrap">
              <img
                :src="getTripImage(trip)"
                :alt="trip.name"
                class="immersive-card__image"
                loading="lazy"
              />
              <!-- Gradient overlay -->
              <div class="immersive-card__overlay" />

              <!-- Crown badge — under budget -->
              <div
                v-if="trip.budget_amount && budgetPct(trip) < 70 && (trip.total_expenses ?? 0) > 0"
                class="crown-badge"
                title="Under budget!"
              >
                <q-icon name="workspace_premium" size="14px" />
              </div>

              <!-- Budget arc (SVG) -->
              <div class="immersive-card__arc" v-if="trip.budget_amount && (trip.total_expenses ?? 0) >= 0">
                <svg width="72" height="72" viewBox="0 0 72 72">
                  <circle cx="36" cy="36" r="28" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="4" />
                  <circle
                    cx="36" cy="36" r="28"
                    fill="none"
                    stroke="white"
                    stroke-width="4"
                    stroke-linecap="round"
                    :stroke-dasharray="`${budgetPct(trip) * 1.759} 175.9`"
                    stroke-dashoffset="0"
                    transform="rotate(-90 36 36)"
                  />
                </svg>
                <div class="immersive-card__arc-label">
                  <span class="arc-pct">{{ Math.round(budgetPct(trip)) }}%</span>
                </div>
              </div>

              <!-- Status chip + countdown -->
              <div class="immersive-card__top-row">
                <div class="status-chip" :class="`status-chip--${getStatusLabel(trip).toLowerCase()}`">
                  {{ getStatusLabel(trip) }}
                </div>
                <div class="countdown-badge" v-if="getCountdown(trip)">
                  {{ getCountdown(trip) }}
                </div>
              </div>

              <!-- Trip info overlay -->
              <div class="immersive-card__info">
                <h2 class="gala-display immersive-card__name">{{ trip.name }}</h2>
                <div class="immersive-card__meta row items-center q-gutter-x-md">
                  <span v-if="trip.destination" class="meta-item">
                    <q-icon name="place" size="12px" />
                    {{ trip.destination }}
                  </span>
                  <span class="meta-item">
                    <q-icon name="calendar_today" size="12px" />
                    {{ formatDateRange(trip.start_date, trip.end_date) }}
                  </span>
                </div>

                <!-- Footer row: members + budget -->
                <div class="immersive-card__footer row items-center justify-between q-mt-sm">
                  <div class="row items-center">
                    <div
                      v-for="i in Math.min(trip.member_count || 1, 5)"
                      :key="i"
                      class="member-pip"
                      :style="{ marginLeft: i > 1 ? '-8px' : '0', background: getMemberAvatarColor(i) }"
                    >{{ String.fromCharCode(64 + i) }}</div>
                    <span v-if="(trip.member_count || 1) > 5" class="member-more">
                      +{{ (trip.member_count || 1) - 5 }}
                    </span>
                  </div>

                  <div class="budget-label" v-if="trip.budget_amount">
                    <span class="budget-spent">{{ formatCurrency(trip.total_expenses || 0, trip.currency_code) }}</span>
                    <span class="budget-sep"> / </span>
                    <span class="budget-total">{{ formatCurrency(trip.budget_amount, trip.currency_code) }}</span>
                  </div>
                  <div class="budget-label" v-else>
                    <span class="budget-spent">{{ formatCurrency(trip.total_expenses || 0, trip.currency_code) }}</span>
                    <span class="budget-sep"> spent</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ═══ Past Trips — Polaroid Horizontal Scroll ═══ -->
        <div v-if="pastTrips.length > 0 && (activeFilter === 'all' || activeFilter === 'past')" class="q-mb-xl">
          <div class="section-label q-mb-md q-px-lg">
            <span class="text-caption text-weight-bold" style="color: var(--muted); letter-spacing: 0.08em; text-transform: uppercase">
              Memories
            </span>
          </div>

          <div class="polaroid-scroll">
            <div class="polaroid-track">
              <div
                v-for="trip in pastTrips"
                :key="trip.id"
                class="polaroid-card"
                @click="goToTripDetails(trip.id)"
              >
                <div class="polaroid-card__photo">
                  <img :src="getTripImage(trip)" :alt="trip.name" loading="lazy" />
                  <div class="polaroid-card__overlay" />
                </div>
                <div class="polaroid-card__caption">
                  <div class="text-weight-bold polaroid-card__name gala-truncate">{{ trip.name }}</div>
                  <div class="polaroid-card__sub">{{ formatDateRange(trip.start_date, trip.end_date) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ═══ Load more (for large lists) ═══ -->
        <div v-if="hasMoreTrips" class="text-center q-mt-lg q-mb-xl q-px-lg">
          <q-btn
            flat no-caps
            color="primary"
            label="Show More"
            class="gala-btn-pill"
            @click="loadMoreTrips"
          />
        </div>
      </template>

    </q-pull-to-refresh>

    <!-- ═══ New Trip Modal ═══ -->
    <q-dialog v-model="showNewTripModal" persistent>
      <q-card class="new-trip-modal">
        <!-- Modal header -->
        <div class="modal-header">
          <div>
            <div class="text-caption text-weight-medium modal-eyebrow">Plan ahead</div>
            <div class="text-h6 text-weight-bold">New Trip</div>
          </div>
          <q-btn icon="close" flat round dense v-close-popup style="color: var(--muted)" />
        </div>

        <q-card-section class="q-gutter-y-md q-pt-sm">
          <q-input
            v-model="newTrip.name"
            label="Trip Name"
            placeholder="e.g., Boracay Summer 2026"
            outlined
            dense
            :rules="[(val: string) => !!val || 'Trip name is required']"
          >
            <template v-slot:prepend><q-icon name="flight_takeoff" /></template>
          </q-input>

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
          <div class="date-range-card row items-center no-wrap">
            <div class="date-range-card__side col cursor-pointer">
              <div class="text-caption q-mb-xs" style="color: var(--muted)">
                <q-icon name="flight_takeoff" size="11px" class="q-mr-xs" />Departure
              </div>
              <div class="text-subtitle1 text-weight-bold">{{ formatTripDate(newTrip.start_date) }}</div>
              <div class="text-caption" style="color: var(--muted)">{{ formatTripYear(newTrip.start_date) }}</div>
              <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                <q-date v-model="newTrip.start_date" mask="YYYY-MM-DD" />
              </q-popup-proxy>
            </div>

            <div class="date-range-card__mid column items-center justify-center q-px-sm">
              <q-icon name="arrow_forward" color="primary" size="18px" />
              <div class="text-caption text-primary text-weight-medium q-mt-xs">{{ tripDuration }}n</div>
            </div>

            <div class="date-range-card__side col cursor-pointer text-right">
              <div class="text-caption q-mb-xs" style="color: var(--muted)">
                Return <q-icon name="flight_land" size="11px" class="q-ml-xs" />
              </div>
              <div class="text-subtitle1 text-weight-bold">{{ formatTripDate(newTrip.end_date) }}</div>
              <div class="text-caption" style="color: var(--muted)">{{ formatTripYear(newTrip.end_date) }}</div>
              <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                <q-date v-model="newTrip.end_date" mask="YYYY-MM-DD" :options="(d: string) => d >= newTrip.start_date" />
              </q-popup-proxy>
            </div>
          </div>

          <!-- Currency + Budget -->
          <div class="row q-gutter-sm">
            <q-select
              v-model="newTrip.currency_code"
              :options="tripStore.currencyOptions"
              label="Currency"
              outlined dense emit-value map-options class="col"
            >
              <template v-slot:prepend><q-icon name="payments" /></template>
            </q-select>
            <q-input
              v-model.number="newTrip.budget_amount"
              label="Budget (optional)"
              type="number"
              outlined dense class="col"
              :prefix="newTrip.currency_code"
            />
          </div>

          <!-- Members -->
          <div>
            <div class="text-caption text-weight-medium q-mb-xs" style="color: var(--muted)">Barkada Members (optional)</div>
            <div class="row q-gutter-sm">
              <q-input
                v-model="newMemberName"
                label="Member name"
                outlined dense class="col"
                @keyup.enter="addMember"
              />
              <q-btn flat round icon="person_add" color="primary" @click="addMember" :disable="!newMemberName.trim()" />
            </div>
            <div v-if="newTripMembers.length > 0" class="row q-gutter-xs q-mt-sm">
              <q-chip
                v-for="(name, idx) in newTripMembers" :key="idx"
                removable @remove="removeMember(idx)"
                color="primary" text-color="white" icon="person" dense
              >{{ name }}</q-chip>
            </div>
          </div>
        </q-card-section>

        <q-card-actions align="right" class="q-pa-md q-pt-sm">
          <q-btn flat no-caps label="Cancel" v-close-popup style="color: var(--muted)" />
          <q-btn
            unelevated no-caps
            color="primary"
            label="Create Trip"
            class="gala-btn-pill"
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
import EmptyState from 'src/components/shared/EmptyState.vue';

const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();
const tripStore = useTripStore();

const showNewTripModal = ref(false);
const activeFilter = ref('all');
const creating = ref(false);
const searchQuery = ref('');
const pageSize = ref(12);
const currentPage = ref(1);

const filters = [
  { label: 'All', value: 'all' },
  { label: 'Active', value: 'active' },
  { label: 'Upcoming', value: 'upcoming' },
  { label: 'Past', value: 'past' },
  { label: 'Archived', value: 'archived' },
];

function filterCount(filterValue: string): number {
  if (filterValue === 'all') return 0;
  const now = new Date().toISOString().split('T')[0] ?? '';
  return tripStore.trips.filter((trip) => {
    if (filterValue === 'archived') return trip.status === 'archived';
    if (trip.status === 'archived') return false;
    switch (filterValue) {
      case 'upcoming': return trip.start_date && trip.start_date > now;
      case 'active':   return trip.start_date && trip.end_date && trip.start_date <= now && trip.end_date >= now;
      case 'past':     return trip.end_date && trip.end_date < now;
      default: return true;
    }
  }).length;
}

const filteredTrips = computed(() => {
  let filtered = tripStore.trips;

  if (activeFilter.value === 'archived') {
    filtered = filtered.filter((trip) => trip.status === 'archived');
  } else {
    filtered = filtered.filter((trip) => trip.status !== 'archived');
    if (activeFilter.value !== 'all') {
      const now = new Date().toISOString().split('T')[0] ?? '';
      filtered = filtered.filter((trip) => {
        switch (activeFilter.value) {
          case 'upcoming': return trip.start_date && trip.start_date > now;
          case 'active':   return trip.start_date && trip.end_date && trip.start_date <= now && trip.end_date >= now;
          case 'past':     return trip.end_date && trip.end_date < now;
          default: return true;
        }
      });
    }
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

const now = new Date().toISOString().split('T')[0] ?? '';

const activeUpcomingTrips = computed(() =>
  filteredTrips.value.filter(
    (t) => !(t.end_date && t.end_date < now)
  ).slice(0, currentPage.value * pageSize.value)
);

const pastTrips = computed(() =>
  filteredTrips.value.filter(
    (t) => t.end_date && t.end_date < now
  ).slice(0, currentPage.value * pageSize.value)
);

const hasMoreTrips = computed(() =>
  activeUpcomingTrips.value.length + pastTrips.value.length < filteredTrips.value.length
);

const newTrip = ref({
  name: '',
  destination: '',
  start_date: new Date().toISOString().split('T')[0] ?? '',
  end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
  currency_code: 'PHP',
  budget_amount: null as number | null,
});

const isFormValid = computed(() =>
  newTrip.value.name &&
  newTrip.value.start_date &&
  newTrip.value.end_date &&
  new Date(newTrip.value.end_date) >= new Date(newTrip.value.start_date)
);

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
      { trip_id: tripData.id, user_id: user.id, name: user.email?.split('@')[0] || 'You', is_owner: true },
      ...newTripMembers.value.map((name) => ({ trip_id: tripData.id, name, is_owner: false })),
    ];
    const { error: memberError } = await supabase.from('members').insert(membersToInsert);
    if (memberError) throw memberError;

    tripStore.addTrip(tripData as Trip);
    $q.notify({ type: 'positive', message: 'Trip created!', position: 'top' });

    newTrip.value = {
      name: '', destination: '',
      start_date: new Date().toISOString().split('T')[0] ?? '',
      end_date: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0] ?? '',
      currency_code: 'PHP', budget_amount: null,
    };
    newTripMembers.value = [];
    newMemberName.value = '';
    showNewTripModal.value = false;
    void router.push(`/trips/${tripData.id}`);
  } catch (err) {
    $q.notify({ type: 'negative', message: 'Failed to create trip: ' + (err instanceof Error ? err.message : 'Unknown error'), position: 'top' });
  } finally {
    creating.value = false;
  }
}

function openNewTripModal() { showNewTripModal.value = true; }
function goToTripDetails(tripId: string) { void router.push(`/trips/${tripId}`); }
function loadMoreTrips() { currentPage.value++; }

async function onRefresh(done: () => void) {
  await tripStore.fetchTrips(true);
  done();
}

function getTripImage(trip: Trip): string {
  const images = [
    'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=500&fit=crop',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=500&fit=crop',
    'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&h=500&fit=crop',
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=500&fit=crop',
    'https://images.unsplash.com/photo-1530841377377-3ff06c0ca713?w=800&h=500&fit=crop',
    'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=800&h=500&fit=crop',
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

function budgetPct(trip: Trip): number {
  if (!trip.budget_amount || !trip.total_expenses) return 0;
  return Math.min(100, (trip.total_expenses / trip.budget_amount) * 100);
}

function formatDateRange(start: string, end: string): string {
  const opts: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  const s = new Date(start).toLocaleDateString('en-US', opts);
  const e = new Date(end).toLocaleDateString('en-US', { ...opts, year: 'numeric' });
  return `${s} – ${e}`;
}

function formatCurrency(amount: number, currency = 'PHP'): string {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount);
}

function getCountdown(trip: Trip): string {
  const now = new Date().toISOString().split('T')[0] ?? '';
  if (!trip.start_date) return '';
  const diff = Math.ceil((new Date(trip.start_date).getTime() - new Date(now).getTime()) / 86400000);
  if (trip.end_date && trip.end_date < now) return '';
  if (diff > 1) return `${diff}d away`;
  if (diff === 1) return 'Tomorrow';
  if (diff === 0) return 'Today!';
  if (trip.end_date && trip.end_date >= now) return 'Ongoing';
  return '';
}

const avatarColors = ['#0D9488', '#F59E0B', '#8B5CF6', '#0EA5E9', '#FB7185'];
function getMemberAvatarColor(i: number): string {
  return avatarColors[(i - 1) % avatarColors.length] ?? '#0D9488';
}

const tripDuration = computed(() => {
  if (!newTrip.value.start_date || !newTrip.value.end_date) return 0;
  return Math.max(0, Math.ceil(
    (new Date(newTrip.value.end_date).getTime() - new Date(newTrip.value.start_date).getTime()) / 86400000
  ));
});

function formatTripDate(dateStr: string): string {
  if (!dateStr) return '—';
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
}

function formatTripYear(dateStr: string): string {
  if (!dateStr) return '';
  return new Date(dateStr + 'T00:00:00').getFullYear().toString();
}

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

function removeMember(idx: number) { newTripMembers.value.splice(idx, 1); }
</script>

<style scoped lang="scss">
// ─── Page ────────────────────────────────────────────────────────────────────
.trips-page {
  min-height: 100vh;
  background-color: var(--surface);
}

// ─── Header ──────────────────────────────────────────────────────────────────
.page-header {
  padding-top: 24px;
}

.header-eyebrow {
  color: $primary;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.page-title {
  font-size: clamp(2.5rem, 8vw, 4rem);
  line-height: 1.05;
  color: var(--on-background);
}

.fab-add {
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba($primary, 0.35);
  transition: transform 150ms var(--ease-spring), box-shadow 150ms var(--ease-out-expo);

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 8px 24px rgba($primary, 0.45);
  }
}

// ─── Search + Filter ─────────────────────────────────────────────────────────
.search-input {
  :deep(.q-field__control) {
    border-radius: var(--gala-radius-pill);
  }
}

.filter-pills {
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 4px;
  &::-webkit-scrollbar { display: none; }
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--gala-radius-pill);
  background: var(--background);
  border: 1px solid var(--border);
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--muted);
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--duration-fast) var(--ease-out-expo);
  user-select: none;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &--active {
    background: $primary;
    border-color: $primary;
    color: white;
  }

  &__count {
    background: rgba(255, 255, 255, 0.25);
    border-radius: 99px;
    padding: 1px 6px;
    font-size: 0.6875rem;
  }
}

// ─── Skeleton ────────────────────────────────────────────────────────────────
.skeleton-card {
  border-radius: var(--gala-radius-xl);
  overflow: hidden;
  background: var(--background);
  border: 1px solid var(--border);
}

.skeleton-img :deep(.q-skeleton) {
  border-radius: 0;
}

// ─── Immersive Card ──────────────────────────────────────────────────────────
.immersive-card {
  border-radius: var(--gala-radius-xl);
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-spring),
              box-shadow var(--duration-normal) var(--ease-out-expo);
  box-shadow: var(--gala-elevation-2);

  &:hover {
    transform: translateY(-6px);
    box-shadow: var(--gala-elevation-float);

    .immersive-card__image {
      transform: scale(1.04);
    }
  }

  &:active {
    transform: translateY(-2px) scale(0.99);
  }

  &__image-wrap {
    position: relative;
    height: clamp(200px, 52vw, 300px);
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
    background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0.05) 0%,
      rgba(0, 0, 0, 0.25) 40%,
      rgba(0, 0, 0, 0.8) 100%
    );
  }

  &__arc {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 72px;
    height: 72px;

    svg {
      position: absolute;
      inset: 0;
    }
  }

  &__arc-label {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__top-row {
    position: absolute;
    top: 14px;
    left: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__info {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
  }

  &__name {
    font-size: clamp(1.4rem, 5vw, 2rem);
    color: white;
    line-height: 1.1;
    margin: 0 0 6px;
    text-shadow: 0 1px 8px rgba(0, 0, 0, 0.3);
  }

  &__meta {
    flex-wrap: wrap;
  }

  &__footer {
    margin-top: 10px;
  }
}

.arc-pct {
  font-size: 0.6875rem;
  font-weight: 700;
  color: white;
  font-variant-numeric: tabular-nums;
}

.status-chip {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  padding: 3px 10px;
  border-radius: var(--gala-radius-pill);
  backdrop-filter: blur(8px);

  &--active    { background: rgba($positive, 0.9); color: white; }
  &--upcoming  { background: rgba($accent, 0.9); color: white; }
  &--completed { background: rgba(0, 0, 0, 0.4); color: rgba(255, 255, 255, 0.8); }
}

.countdown-badge {
  font-size: 0.75rem;
  font-weight: 600;
  color: white;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  padding: 3px 10px;
  border-radius: var(--gala-radius-pill);
  animation: countdown-pulse 2.5s ease-in-out infinite;
}

@keyframes countdown-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.75; transform: scale(0.97); }
}

.crown-badge {
  position: absolute;
  bottom: 60px;
  right: 14px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.5);
  animation: crown-float 3s ease-in-out infinite;
}

@keyframes crown-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.meta-item {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  gap: 3px;
}

.member-pip {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: white;
  position: relative;
}

.member-more {
  font-size: 0.75rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  margin-left: 6px;
}

.budget-label {
  font-size: 0.8rem;
}

.budget-spent {
  font-weight: 700;
  color: white;
  font-variant-numeric: tabular-nums;
}

.budget-sep {
  color: rgba(255, 255, 255, 0.5);
}

.budget-total {
  color: rgba(255, 255, 255, 0.65);
  font-variant-numeric: tabular-nums;
}

// ─── Polaroid Scroll ─────────────────────────────────────────────────────────
.polaroid-scroll {
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 8px;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { display: none; }
}

.polaroid-track {
  display: flex;
  gap: 16px;
  padding: 4px 24px 8px;
  width: max-content;
}

.polaroid-card {
  width: 150px;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-spring);

  &:hover {
    transform: translateY(-6px) rotate(1deg);
  }

  &:nth-child(odd) {
    transform: rotate(-1.5deg);
    &:hover { transform: translateY(-6px) rotate(0deg); }
  }

  &:nth-child(even) {
    transform: rotate(1deg);
    &:hover { transform: translateY(-6px) rotate(0deg); }
  }

  &__photo {
    position: relative;
    height: 120px;
    border-radius: 8px 8px 0 0;
    overflow: hidden;
    background: var(--surface);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__overlay {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.12);
  }

  &__caption {
    background: white;
    padding: 8px 10px 12px;
    border-radius: 0 0 4px 4px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }

  &__name {
    font-size: 0.75rem;
    color: var(--on-background);
    margin-bottom: 2px;
  }

  &__sub {
    font-size: 0.6875rem;
    color: var(--muted);
  }
}

body.body--dark .polaroid-card__caption {
  background: #1E293B;
}

// ─── New Trip Modal ──────────────────────────────────────────────────────────
.new-trip-modal {
  width: 100%;
  max-width: 520px;
  border-radius: var(--gala-radius-xl) !important;
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 20px 8px;
}

.modal-eyebrow {
  color: $primary;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.7rem;
  margin-bottom: 2px;
}

.date-range-card {
  border: 1.5px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 14px 16px;
  background: rgba($primary, 0.03);
  transition: border-color var(--duration-fast) ease;

  &:hover { border-color: $primary; }

  &__side {
    padding: 2px 6px;
    border-radius: var(--gala-radius-sm);
    transition: background var(--duration-fast) ease;
    position: relative;
    &:hover { background: rgba($primary, 0.08); }
  }

  &__mid {
    border-left: 1px dashed rgba($primary, 0.25);
    border-right: 1px dashed rgba($primary, 0.25);
    min-width: 48px;
    padding: 4px 10px;
  }
}
</style>
