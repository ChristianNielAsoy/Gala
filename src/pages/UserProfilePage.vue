<template>
  <q-page class="q-pa-md bg-surface">
    <q-card flat bordered>
      <q-card-section>
        <div class="row items-center q-gutter-md">
          <q-avatar size="80px" color="primary" text-color="white">
            {{ userProfile.initials }}
          </q-avatar>
          <div>
            <div class="text-h6 text-weight-bold">{{ userProfile.name }}</div>
            <div class="text-caption text-grey-7">{{ userProfile.email }}</div>
          </div>
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div class="text-subtitle1 q-mb-sm">Trip History</div>
        <q-list bordered separator>
          <q-item v-for="trip in userProfile.trips" :key="trip.id">
            <q-item-section>
              <q-item-label class="text-weight-bold">{{ trip.name }}</q-item-label>
              <q-item-label caption>{{ trip.dateRange }}</q-item-label>
            </q-item-section>
          </q-item>
          <div v-if="userProfile.trips.length === 0" class="text-center q-pa-md text-grey-6">
            No trips yet.
          </div>
        </q-list>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div class="text-subtitle1 q-mb-sm">Achievements</div>
        <q-chip
          v-for="badge in userProfile.badges"
          :key="badge"
          color="secondary"
          text-color="white"
          class="q-mr-sm q-mb-sm"
        >
          {{ badge }}
        </q-chip>
        <div v-if="userProfile.badges.length === 0" class="text-grey-6">No achievements yet.</div>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { supabase } from 'boot/supabase';

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

async function fetchUserProfile() {
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return;
  const email = user.email ?? '';
  userProfile.value.name = user.user_metadata?.full_name || (email ? email.split('@')[0] : '');
  userProfile.value.email = email;
  userProfile.value.initials = userProfile.value.name
    .split(' ')
    .map((n: string) => n[0])
    .join('')
    .toUpperCase();

  // Fetch trips
  const { data: memberData } = await supabase
    .from('members')
    .select('trip_id, trips(name, start_date, end_date)')
    .eq('user_id', user.id);
  type MemberTrip = {
    trip_id: string;
    trips?:
      | { name?: string; start_date?: string; end_date?: string }
      | { name?: string; start_date?: string; end_date?: string }[];
  };
  const trips = (memberData || []).map((m: MemberTrip) => {
    let tripObj: { name?: string; start_date?: string; end_date?: string } | undefined;
    if (Array.isArray(m.trips)) {
      tripObj = m.trips[0];
    } else {
      tripObj = m.trips;
    }
    return {
      id: m.trip_id,
      name: tripObj?.name || 'Trip',
      dateRange: tripObj ? `${tripObj.start_date ?? ''} - ${tripObj.end_date ?? ''}` : '',
      start_date: tripObj?.start_date,
      end_date: tripObj?.end_date,
    };
  });
  userProfile.value.trips = trips as TripSummary[];

  // Example badges (could be dynamic)
  userProfile.value.badges = [];

  // Achievement logic
  if (trips.length > 0) userProfile.value.badges.push('First Trip!');
  if (trips.length > 2) userProfile.value.badges.push('Seasoned Traveler');
  if (trips.length > 5) userProfile.value.badges.push('Travel Enthusiast');
  if (trips.length > 10) userProfile.value.badges.push('Globe Trotter');

  // Check for completed trips
  const completedTrips = trips.filter(
    (t) => t.end_date && new Date(t.end_date) < new Date(),
  ).length;
  if (completedTrips > 0) userProfile.value.badges.push('Trip Completed');
  if (completedTrips > 3) userProfile.value.badges.push('Experienced Explorer');

  // Check for upcoming trips
  const upcomingTrips = trips.filter(
    (t) => t.start_date && new Date(t.start_date) > new Date(),
  ).length;
  if (upcomingTrips > 0) userProfile.value.badges.push('Adventure Awaits');

  // If no badges, add a default one
  if (userProfile.value.badges.length === 0) {
    userProfile.value.badges.push('New Traveler');
  }
}

onMounted(fetchUserProfile);
</script>
