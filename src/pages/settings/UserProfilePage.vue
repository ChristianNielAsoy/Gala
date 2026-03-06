<template>
  <q-page class="profile-page">

    <!-- ═══ Profile hero ═══ -->
    <div class="profile-hero gala-mesh-bg">
      <div class="profile-hero__inner">
        <div class="profile-avatar">
          <span>{{ displayInitials }}</span>
        </div>
        <h1 class="profile-hero__name">{{ displayName }}</h1>
        <p v-if="userProfile.nickname" class="profile-hero__nickname">"{{ userProfile.nickname }}"</p>
        <p class="profile-hero__email">{{ userProfile.email }}</p>
        <p v-if="age !== null" class="profile-hero__age">{{ age }} years old · {{ userProfile.city || userProfile.country ? [userProfile.city, userProfile.country].filter(Boolean).join(', ') : '' }}</p>

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

      <!-- ═══ Personal Info (editable) ═══ -->
      <div class="profile-section q-mb-md">
        <div class="profile-section__header">
          <div class="profile-section__label">Personal Info</div>
          <q-btn
            v-if="!editing"
            flat no-caps dense
            color="primary"
            label="Edit"
            icon="edit"
            size="sm"
            @click="startEdit"
          />
          <div v-else class="row gap-8">
            <q-btn flat no-caps dense color="grey-6" label="Cancel" size="sm" @click="cancelEdit" />
            <q-btn
              unelevated no-caps dense
              color="primary"
              label="Save"
              size="sm"
              :loading="saving"
              @click="saveProfile"
            />
          </div>
        </div>

        <div class="profile-panel">
          <!-- View mode -->
          <template v-if="!editing">
            <div class="profile-info-row">
              <span class="profile-info-label">First Name</span>
              <span class="profile-info-value">{{ userProfile.first_name || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">Last Name</span>
              <span class="profile-info-value">{{ userProfile.last_name || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">Nickname</span>
              <span class="profile-info-value">{{ userProfile.nickname || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">Gender</span>
              <span class="profile-info-value">{{ genderLabel || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">Birthday</span>
              <span class="profile-info-value">{{ formattedBirthday || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">Phone</span>
              <span class="profile-info-value">{{ userProfile.phone || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">City</span>
              <span class="profile-info-value">{{ userProfile.city || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">Country</span>
              <span class="profile-info-value">{{ userProfile.country || '—' }}</span>
            </div>
            <q-separator />
            <div class="profile-info-row">
              <span class="profile-info-label">Bio</span>
              <span class="profile-info-value profile-info-value--bio">{{ userProfile.bio || '—' }}</span>
            </div>
          </template>

          <!-- Edit mode -->
          <template v-else>
            <div class="profile-edit-grid">
              <q-input
                v-model="form.first_name"
                label="First Name"
                outlined dense
                class="profile-edit-field"
              />
              <q-input
                v-model="form.last_name"
                label="Last Name"
                outlined dense
                class="profile-edit-field"
              />
              <q-input
                v-model="form.nickname"
                label="Nickname"
                outlined dense
                class="profile-edit-field"
              />
              <q-select
                v-model="form.gender"
                label="Gender"
                outlined dense
                emit-value map-options
                class="profile-edit-field"
                :options="genderOptions"
              />
              <q-input
                v-model="form.birthday"
                label="Birthday"
                outlined dense
                type="date"
                stack-label
                class="profile-edit-field"
              />
              <q-input
                v-model="form.phone"
                label="Phone"
                outlined dense
                type="tel"
                class="profile-edit-field"
              />
              <q-input
                v-model="form.city"
                label="City"
                outlined dense
                class="profile-edit-field"
              />
              <q-input
                v-model="form.country"
                label="Country"
                outlined dense
                class="profile-edit-field"
              />
              <q-input
                v-model="form.bio"
                label="Bio"
                outlined dense
                type="textarea"
                autogrow
                maxlength="200"
                class="profile-edit-field profile-edit-field--full"
                hint="Max 200 characters"
              />
            </div>
          </template>
        </div>
      </div>

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
import { useQuasar } from 'quasar';
import { useAuthStore } from 'src/stores/authStore';
import { useTripStore } from 'src/stores/tripStore';
import { supabase } from 'boot/supabase';

const router = useRouter();
const $q = useQuasar();
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
  first_name: string;
  last_name: string;
  nickname: string;
  gender: string;
  birthday: string;
  phone: string;
  city: string;
  country: string;
  bio: string;
  trips: TripSummary[];
  badges: string[];
}

const userProfile = ref<UserProfile>({
  name: '',
  email: '',
  first_name: '',
  last_name: '',
  nickname: '',
  gender: '',
  birthday: '',
  phone: '',
  city: '',
  country: '',
  bio: '',
  trips: [],
  badges: [],
});

const editing = ref(false);
const saving = ref(false);
const form = ref({ first_name: '', last_name: '', nickname: '', gender: '', birthday: '', phone: '', city: '', country: '', bio: '' });

// ─── Computed display values ───────────────────────────────────────────────────

const displayName = computed(() => {
  const { first_name, last_name, name } = userProfile.value;
  if (first_name || last_name) return [first_name, last_name].filter(Boolean).join(' ');
  return name || 'Traveler';
});

const displayInitials = computed(() => {
  return displayName.value
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2) || '?';
});

const genderOptions = [
  { label: 'Male', value: 'male' },
  { label: 'Female', value: 'female' },
  { label: 'Non-binary', value: 'non-binary' },
  { label: 'Prefer not to say', value: 'prefer_not_to_say' },
];

const genderLabel = computed(() =>
  genderOptions.find((g) => g.value === userProfile.value.gender)?.label ?? '',
);

const age = computed<number | null>(() => {
  const bday = userProfile.value.birthday;
  if (!bday) return null;
  const birth = new Date(bday);
  const today = new Date();
  let a = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) a--;
  return a;
});

const formattedBirthday = computed(() => {
  if (!userProfile.value.birthday) return '';
  return new Date(userProfile.value.birthday + 'T00:00:00').toLocaleDateString('en-US', {
    year: 'numeric', month: 'long', day: 'numeric',
  });
});

const upcomingCount = computed(() =>
  userProfile.value.trips.filter(
    (t) => t.start_date && new Date(t.start_date) > new Date(),
  ).length,
);

// ─── Edit helpers ──────────────────────────────────────────────────────────────

function startEdit() {
  form.value = {
    first_name: userProfile.value.first_name,
    last_name: userProfile.value.last_name,
    nickname: userProfile.value.nickname,
    gender: userProfile.value.gender,
    birthday: userProfile.value.birthday,
    phone: userProfile.value.phone,
    city: userProfile.value.city,
    country: userProfile.value.country,
    bio: userProfile.value.bio,
  };
  editing.value = true;
}

function cancelEdit() {
  editing.value = false;
}

async function saveProfile() {
  const user = authStore.user;
  if (!user) return;
  saving.value = true;
  try {
    const { error } = await supabase.from('profiles').upsert({
      id: user.id,
      first_name: form.value.first_name.trim() || null,
      last_name: form.value.last_name.trim() || null,
      nickname: form.value.nickname.trim() || null,
      gender: form.value.gender || null,
      birthday: form.value.birthday || null,
      phone: form.value.phone.trim() || null,
      city: form.value.city.trim() || null,
      country: form.value.country.trim() || null,
      bio: form.value.bio.trim() || null,
      updated_at: new Date().toISOString(),
    });
    if (error) throw error;

    userProfile.value.first_name = form.value.first_name.trim();
    userProfile.value.last_name = form.value.last_name.trim();
    userProfile.value.nickname = form.value.nickname.trim();
    userProfile.value.gender = form.value.gender;
    userProfile.value.birthday = form.value.birthday;
    userProfile.value.phone = form.value.phone.trim();
    userProfile.value.city = form.value.city.trim();
    userProfile.value.country = form.value.country.trim();
    userProfile.value.bio = form.value.bio.trim();
    editing.value = false;
    $q.notify({ type: 'positive', message: 'Profile updated!', position: 'top' });
  } catch (err) {
    console.error(err);
    $q.notify({ type: 'negative', message: 'Failed to save profile.', position: 'top' });
  } finally {
    saving.value = false;
  }
}

// ─── Trip helpers ──────────────────────────────────────────────────────────────

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

// ─── Data fetch ────────────────────────────────────────────────────────────────

async function fetchUserProfile() {
  const user = authStore.user;
  if (!user) return;

  const email = user.email ?? '';
  userProfile.value.name = user.user_metadata?.full_name || (email ? email.split('@')[0] : '');
  userProfile.value.email = email;

  // Fetch extended profile from DB
  const { data: profileRow } = await supabase
    .from('profiles')
    .select('first_name, last_name, nickname, gender, birthday, phone, city, country, bio')
    .eq('id', user.id)
    .maybeSingle();

  if (profileRow) {
    userProfile.value.first_name = profileRow.first_name ?? '';
    userProfile.value.last_name = profileRow.last_name ?? '';
    userProfile.value.nickname = profileRow.nickname ?? '';
    userProfile.value.gender = profileRow.gender ?? '';
    userProfile.value.birthday = profileRow.birthday ?? '';
    userProfile.value.phone = profileRow.phone ?? '';
    userProfile.value.city = profileRow.city ?? '';
    userProfile.value.country = profileRow.country ?? '';
    userProfile.value.bio = profileRow.bio ?? '';
  }

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

.profile-hero__nickname {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 1rem;
  color: #0D9488;
  margin: 0 0 4px;
  font-style: italic;
}

.profile-hero__email {
  font-size: 0.9rem;
  color: var(--muted);
  margin: 0 0 4px;
}

.profile-hero__age {
  font-size: 0.85rem;
  color: var(--muted);
  margin: 0 0 20px;
}

// ─── Stats ────────────────────────────────────────────────────────────────────
.profile-stats {
  display: inline-flex;
  align-items: center;
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

.profile-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.profile-section__label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

// ─── Personal Info panel ───────────────────────────────────────────────────────
.profile-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

.profile-info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 16px;
  gap: 16px;
}

.profile-info-label {
  font-size: 0.8125rem;
  color: var(--muted);
  flex-shrink: 0;
  min-width: 90px;
}

.profile-info-value {
  font-size: 0.9375rem;
  color: var(--on-background);
  text-align: right;

  &--bio {
    text-align: left;
    line-height: 1.5;
  }
}

.profile-edit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 16px;
}

.profile-edit-field {
  &--full {
    grid-column: 1 / -1;
  }
}

.gap-8 {
  display: flex;
  gap: 8px;
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
