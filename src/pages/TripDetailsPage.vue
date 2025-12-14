<template>
  <q-page class="q-pb-xl bg-surface">
    <!-- Header with Tabs -->
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round icon="arrow_back" @click="handleBack" aria-label="Go Back" />
        <q-toolbar-title>
          {{ trip ? trip.name : 'Loading...' }}
        </q-toolbar-title>
        <q-btn flat round icon="settings" @click="openSettingsModal" aria-label="Settings" />
      </q-toolbar>

      <!-- Navigation Tabs -->
      <q-tabs v-model="tab" align="justify" indicator-color="white" active-color="white" dense>
        <q-tab name="itinerary" label="Itinerary" icon="event_note" />
        <!-- <q-tab name="expenses" label="Expenses" icon="receipt_long" /> -->
        <q-tab name="settlement" label="Settlement" icon="account_balance_wallet" />
        <q-tab name="members" label="Members" icon="people" />
        <q-tab name="activity" label="Activity" icon="timeline" />
      </q-tabs>
    </q-header>

    <!-- Tab Panels -->
    <q-tab-panels v-model="tab" animated class="bg-surface">
      <!-- Itinerary Tab Panel -->
      <q-tab-panel name="itinerary" class="q-pa-md">
        <itinerary-tab v-model="itineraryItems" />
      </q-tab-panel>
      <!-- Expenses Tab Panel removed -->

      <!-- Settlement Tab Panel -->
      <q-tab-panel name="settlement" class="q-pa-none">
        <settlement-view
          v-if="trip && members.length > 0 && !loading"
          :expenses="itineraryExpensesWithSplits"
          :members="members"
          :current-member-id="currentMemberId ?? ''"
          :currency-code="trip.currency_code"
        />
        <div v-else class="text-center q-pa-xl">
          <q-spinner color="primary" size="lg" />
          <p class="text-grey-6 q-mt-md">Loading settlement data...</p>
        </div>
      </q-tab-panel>

      <!-- Members Tab -->
      <q-tab-panel name="members" class="q-pa-md">
        <div class="text-h6 q-mb-md">Trip Members</div>
        <q-list bordered separator rounded>
          <q-item v-for="member in members" :key="member.id">
            <q-item-section avatar>
              <q-avatar :color="member.is_owner ? 'primary' : 'grey-5'" text-color="white">
                {{ member.name.charAt(0).toUpperCase() }}
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-medium">
                {{ member.name }}
              </q-item-label>
              <q-item-label caption v-if="member.is_owner"> Trip Owner </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-icon name="star" color="amber" v-if="member.is_owner" />
            </q-item-section>
          </q-item>

          <!-- Add Member Button -->
          <q-item clickable @click="addMemberDialog = true">
            <q-item-section avatar>
              <q-avatar color="grey-3" text-color="grey-7" icon="add" />
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-grey-7">Add Member</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-tab-panel>

      <!-- Activity Tab -->
      <q-tab-panel name="activity" class="q-pa-md">
        <div class="text-h6 q-mb-md">Recent Activity</div>
        <q-timeline color="primary">
          <q-timeline-entry
            title="Trip Created"
            :subtitle="formatDate(trip?.created_at || '')"
            icon="add_circle"
          >
            <div>{{ trip?.name }} was created</div>
          </q-timeline-entry>

          <!-- Removed old recentExpenses and expenses display -->
        </q-timeline>
      </q-tab-panel>
    </q-tab-panels>

    <!-- Floating Action Button for Expenses removed -->

    <!-- Settings Modal -->
    <q-dialog v-model="showSettings">
      <q-card style="width: 100%; max-width: 400px">
        <q-card-section>
          <div class="text-h6">Trip Settings: {{ trip?.name }}</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <p>Edit trip details, change currency, or archive the trip.</p>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="primary" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Add Member Dialog -->
    <q-dialog v-model="addMemberDialog">
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Add Member</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <q-input
            v-model="newMemberName"
            label="Member Name"
            outlined
            autofocus
            @keyup.enter="addMember"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn flat label="Add" color="primary" @click="addMember" :disable="!newMemberName" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';
import type { TripMember } from 'src/types/expense';
import SettlementView from 'src/components/SettlementView.vue';
import ItineraryTab from 'src/components/ItineraryTab.vue';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();

// State
const tab = ref('itinerary');
const loading = ref(true);
const loadingExpenses = ref(false);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const showSettings = ref(false);
const addMemberDialog = ref(false);
const newMemberName = ref('');
const tripId = ref(route.params.tripId as string);
import type { ItineraryItem } from 'src/components/itinerary.model';

interface ItineraryEvent {
  id: string;
  trip_id: string;
  event_date: string;
  event_time?: string;
  title: string;
  location?: string;
  description?: string;
  icon?: string;
  display_order: number;
  created_at: string;
}

const itineraryItems = ref<ItineraryItem[]>([]);

// Compute expenses from itinerary items (type: 'expense')
const itineraryExpensesWithSplits = computed(() => {
  // Only include valid expenses with amount and a paid_by_id
  return itineraryItems.value
    .filter((item) => item.type === 'expense' && typeof item.amount === 'number')
    .map((item) => ({
      id: item.id,
      paid_by_id: members.value[0]?.id ?? '', // fallback to first member
      amount: item.amount as number,
      splits: members.value.map((m) => ({
        member_id: m.id,
        share_amount: (item.amount as number) / members.value.length,
      })),
    }));
});

let currentUserId: string | undefined;
supabase.auth
  .getUser()
  .then(({ data: { user } }) => {
    currentUserId = user?.id;
  })
  .catch(console.error);

const currentMemberId = computed(
  () => members.value.find((m: TripMember) => m.user_id === currentUserId)?.id,
);

// Removed unused/invalid old expenses logic

// Methods

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

async function fetchTripData(): Promise<void> {
  loading.value = true;
  loadingExpenses.value = true;
  const id = tripId.value;

  if (!id) {
    trip.value = null;
    loading.value = false;
    loadingExpenses.value = false;
    return;
  }

  try {
    // 1. Fetch Trip Details
    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .select('*')
      .eq('id', id)
      .single();

    if (tripError || !tripData) throw new Error('Trip not found or access denied.');
    trip.value = {
      ...tripData,
      currencyCode: tripData.currency_code,
    } as Trip;

    // 2. Fetch Trip Members
    const { data: memberData, error: memberError } = await supabase
      .from('members')
      .select('*')
      .eq('trip_id', id)
      .order('name');

    if (memberError || !memberData) throw new Error('Could not load trip members.');
    members.value = memberData as TripMember[];

    // 3. Fetch Itinerary Events
    const { data: itineraryData, error: itineraryError } = await supabase
      .from('itinerary_events')
      .select('*')
      .eq('trip_id', id)
      .order('event_date', { ascending: true })
      .order('display_order', { ascending: true });

    if (itineraryError) {
      console.warn('Could not load itinerary events:', itineraryError);
    } else {
      // Convert itinerary_events to ItineraryItem format
      itineraryItems.value = (itineraryData as ItineraryEvent[]).map((event: ItineraryEvent) => ({
        id: event.id,
        phase: 'on' as const,
        title: event.title,
        type: 'text' as const,
        date: event.event_date,
        ...(event.event_time && { time: event.event_time }),
        ...(event.location && { location: event.location }),
        ...(event.description && { notes: event.description }),
      }));
    }

    // 4. Fetch Expenses WITH SPLITS for settlement calculations

    // Removed old expenses logic
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '';
    console.error('Error fetching trip data:', error);
    $q.notify({
      type: 'negative',
      message: errorMessage || 'Error fetching trip data.',
    });
    trip.value = null;
    members.value = [];
  } finally {
    loading.value = false;
    loadingExpenses.value = false;
  }
}

async function addMember(): Promise<void> {
  if (!newMemberName.value.trim()) return;

  try {
    const { error } = await supabase.from('members').insert({
      trip_id: tripId.value,
      name: newMemberName.value.trim(),
      is_owner: false,
    });

    if (error) throw error;

    $q.notify({ type: 'positive', message: 'Member added!' });
    newMemberName.value = '';
    addMemberDialog.value = false;
    await fetchTripData();
  } catch (error) {
    console.error('Error adding member:', error);
    $q.notify({ type: 'negative', message: 'Failed to add member' });
  }
}

function openSettingsModal(): void {
  showSettings.value = true;
}

function handleBack(): void {
  void router.back();
}

// Lifecycle
onMounted(() => {
  void fetchTripData();
});
</script>

<style scoped>
.q-page {
  min-height: 100vh;
}
</style>
